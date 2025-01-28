package last.project.jvmtuner.service.tuning_test;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.dto.metric.GetRangeMetricResponseDto;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import last.project.jvmtuner.props.MetricsProps;
import last.project.jvmtuner.service.MetricService;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static last.project.jvmtuner.model.tuning_test.TuningTestStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningTestCheckerService {

    private final TuningTestRepository tuningTestRepository;
    private final KubernetesClient k8sClient;
    private final K8sExecService k8sExecService;
    private final MetricService metricService;
    private final MetricsProps metricsProps;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void checkNotReadyTest(UUID testUuid) {
        var test = tuningTestRepository.findById(testUuid).get();

        this.checkTestTimeout(test);

        var deployment = this.deserealizeAndCheckDeployment(test);
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
            throw new IllegalStateException(String.format("Can't find pod of deployment '%s' in namespace '%s'",
                    deploymentName, namespace));
        } else if (pods.size() > 1) {
            throw new IllegalStateException(String.format("Too many pods (%d) of deployment '%s' in namespace '%s'",
                    pods.size(), deploymentName, namespace));
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
        test.setPodName(pod.getMetadata().getName());
        test.setStatus(RUNNING);
        test = tuningTestRepository.save(test);
        log.info(String.format("Test '%s' status changed to %s", test.getUuid(), test.getStatus()));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void checkRunningTest(UUID testUuid) {
        var test = tuningTestRepository.findById(testUuid).get();

        var podName = test.getPodName();
        if (isNull(podName)) {
            throw new IllegalStateException(String.format("Pod name is null in test '%s'", test.getUuid()));
        }

        var deployment = this.deserealizeAndCheckDeployment(test);
        var deploymentName = deployment.getMetadata().getName();
        var namespace = deployment.getMetadata().getNamespace();

        var pod = k8sClient.pods().inNamespace(namespace).withName(podName).get();
        if (isNull(pod)) {
            throw new IllegalStateException(String.format("Can't find pod of deployment '%s' in namespace '%s'",
                    deploymentName, namespace));
        }

        var isHealthy = pod.getStatus().getContainerStatuses().stream()
                .allMatch(containerStatus ->
                        Boolean.TRUE.equals(containerStatus.getReady()) &&
                                containerStatus.getRestartCount() == 0);
        if (!isHealthy) {
            throw new IllegalStateException(String.format("Not healthy pod of deployment '%s' in namespace '%s'",
                    deploymentName, namespace));
        }

        var startedTime = test.getStartedTestTime();
        var endTime = startedTime.plusSeconds(test.getTuningTestProps().getTestDurationSec());

        if (Instant.now().isAfter(startedTime.plusSeconds(metricsProps.getTuningTest().getCheckDelayAfterLoadStartSec()))) {
            var metricMaxValues = test.getTuningTestProps().getMetricMaxValues();
            for (var metricMax : metricMaxValues) {
                var query = metricService.replaceWithTestLabels(metricMax.getMetricQueryProps().getQuery(),
                        test.getUuid().toString(), podName, test.getTuningTestProps().getAppContainerName());
                BigDecimal currentMax;
                try {
                    var currentValues = metricService.rangeRequest(query, startedTime, endTime);
                    var currentMaxString = currentValues.getData().getResult().get(0).getValues()
                            .stream()
                            .max(Comparator.comparing(GetRangeMetricResponseDto.Value::getValue))
                            .get()
                            .getValue();
                    currentMax = new BigDecimal(currentMaxString);
                } catch (Exception ex) {
                    throw new IllegalStateException(String.format("Failed fetching metric '%s' in test '%s'",
                            query, test.getUuid()), ex);
                }
                if (currentMax.compareTo(BigDecimal.valueOf(metricMax.getValue())) > 0) {
                    throw new IllegalStateException(String.format("Test '%s' failed due to metric '%s': %s > %s",
                            test.getUuid(), query, currentMax, metricMax.getValue()));
                }
            }
        }

        if (Instant.now().isAfter(endTime)) {
            test.setStatus(ENDED);
            test = tuningTestRepository.save(test);
            log.info(String.format("Test '%s' status changed to %s", test.getUuid(), test.getStatus()));
            this.deploymentReplicasToZero(test);
        } else {
            log.info(String.format("Test '%s' check OK", test.getUuid()));
        }
    }

    private Deployment deserealizeAndCheckDeployment(TuningTest test) {
        var deployedDeployment = K8sDeploymentUtil.deserialize(test.getDeployment());
        var deployment = k8sClient.apps().deployments()
                .inNamespace(deployedDeployment.getMetadata().getNamespace())
                .withName(deployedDeployment.getMetadata().getName())
                .get();

        if (isNull(deployment)) {
            throw new IllegalStateException(String.format("Deployment '%s' not found in namespace '%s'",
                    deployment.getMetadata().getName(), deployment.getMetadata().getNamespace()));
        }
        return deployment;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void failTest(UUID testUuid, TuningTestStatus newStatus, Throwable ex) {
        var test = tuningTestRepository.findById(testUuid).get();
        test.setStatus(newStatus);
        tuningTestRepository.save(test);
        log.error(String.format("Test '%s' failed: %s", test.getUuid(), test.getStatus().name()), ex);
    }

    @Transactional
    public void deploymentReplicasToZero(UUID testUuid) {
        this.deploymentReplicasToZero(tuningTestRepository.findById(testUuid).get());
    }

    private void deploymentReplicasToZero(TuningTest test) {
        var deployment = this.deserealizeAndCheckDeployment(test);
        k8sClient.apps().deployments()
                .inNamespace(deployment.getMetadata().getNamespace())
                .withName(deployment.getMetadata().getName())
                .scale(0);
    }

    private void checkTestTimeout(TuningTest test) {
        var timeout = Instant.now().getEpochSecond() - test.getDeployedTime().getEpochSecond() >
                test.getTuningTestProps().getStartTestTimeoutSec();
        if (timeout) {
            throw new IllegalStateException(String.format("Test '%s' timeout", test.getUuid()));
        }
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
