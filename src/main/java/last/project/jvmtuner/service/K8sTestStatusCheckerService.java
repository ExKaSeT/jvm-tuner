package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import last.project.jvmtuner.dao.TuningTestRepository;
import last.project.jvmtuner.model.TuningTest;
import last.project.jvmtuner.model.TuningTestStatus;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.isNull;
import static last.project.jvmtuner.model.TuningTestStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class K8sTestStatusCheckerService {

    private final TuningTestRepository tuningTestRepository;
    private final KubernetesClient k8sClient;
    private final K8sExecService k8sExecService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public void checkTests() {
        tuningTestRepository.getAllByStatus(NOT_READY).forEach(this::checkNotReadyTest);
    }

    public void checkNotReadyTest(TuningTest test) {
        if (this.checkTestTimeout(test)) {
            return;
        }

        var deployedDeployment = K8sDeploymentUtil.deserialize(test.getDeployment());
        var deployment = k8sClient.apps().deployments()
                .inNamespace(deployedDeployment.getMetadata().getNamespace())
                .withName(deployedDeployment.getMetadata().getName())
                .get();

        if (isNull(deployment)) {
            log.error(String.format("Deployment '%s' not found in namespace '%s'", deployment.getMetadata().getName(),
                    deployment.getMetadata().getNamespace()));
            this.failTest(test, FAILED);
            return;
        }

        var deploymentName = deployment.getMetadata().getName();
        var namespace = deployment.getMetadata().getNamespace();

        var selector = deployment.getSpec().getSelector().getMatchLabels().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Deployment '%s' in namespace '%s' doesn't have selector",
                                deploymentName, namespace)
                ));

        List<Pod> pods = k8sClient.pods().inNamespace(namespace)
                .withLabelSelector(selector)
                .list()
                .getItems();

        if (pods.isEmpty()) {
            log.error(String.format("Can't find pod of deployment '%s' in namespace '%s'", deploymentName, namespace));
            this.failTest(test, FAILED);
            return;
        } else if (pods.size() > 1) {
            log.error(String.format("Too many pods (%d) of deployment '%s' in namespace '%s'", pods.size(),
                    deploymentName, namespace));
            this.failTest(test, FAILED);
            return;
        }

        var pod = pods.get(0);
        var status = pod.getStatus();
        if (isNull(status) || isNull(status.getContainerStatuses()) || status.getContainerStatuses().stream()
                .anyMatch(containerStatus -> Boolean.FALSE.equals(containerStatus.getReady()))) {
            log.info(String.format("Pod '%s' in namespace '%s' not ready", pod.getMetadata().getName(), namespace));
            return;
        }

        k8sExecService.execCommandInPod(pod.getMetadata().getName(), namespace,
                this.parseExecCmd(test.getTuningTestProps().getGatlingExecCommand()));

        test.setStartedTestTime(Instant.now());
        test.setStatus(RUNNING);
        tuningTestRepository.save(test);
    }

    private void failTest(TuningTest test, TuningTestStatus newStatus) {
        test.setStatus(newStatus);
        tuningTestRepository.save(test);
        log.warn(String.format("Test '%s' failed: %s", test.getUuid(), test.getStatus().name()));
        // TODO: scale deployment replicas to 0
    }

    private boolean checkTestTimeout(TuningTest test) {
        var timeout = Instant.now().getEpochSecond() - test.getDeployedTime().getEpochSecond() >
                test.getTuningTestProps().getStartTestTimeoutSec();
        if (timeout) {
            log.warn(String.format("Test '%s' timeout", test.getUuid()));
            this.failTest(test, FAILED);
        }
        return timeout;
    }

    private List<String> parseExecCmd(String cmd) {
        var result = new ArrayList<String>();

        boolean canSpaceSplit = true;
        var builder = new StringBuilder();
        for (char ch : cmd.toCharArray()) {
            if (ch == '"') {
                canSpaceSplit = !canSpaceSplit;
                continue;
            } else if (ch == ' ' && canSpaceSplit) {
                if (!builder.isEmpty()) {
                    result.add(builder.toString());
                    builder.setLength(0);
                }
                continue;
            }
            builder.append(ch);
        }
        if (!builder.isEmpty()) {
            result.add(builder.toString());
        }
        return result;
    }
}
