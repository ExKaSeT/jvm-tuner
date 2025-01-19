package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import last.project.jvmtuner.dao.TuningTestRepository;
import last.project.jvmtuner.dto.tuning_test.MetricMaxValueDto;
import last.project.jvmtuner.model.MetricMaxValue;
import last.project.jvmtuner.model.MetricMaxValueId;
import last.project.jvmtuner.model.TuningTest;
import last.project.jvmtuner.model.TuningTestStatus;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class K8sTestRunnerService {

    private final K8sDeployService k8sDeployService;
    private final TuningTestRepository tuningTestRepository;
    private final KubernetesClient k8sClient;

    @Transactional
    public UUID runTest(Deployment app, String appContainerName, String appMetricPortWithPath,
                        String gatlingImage, String gatlingExecCommand,
                        int durationSec, int startTestTimeoutSec,
                        List<MetricMaxValueDto> metricMaxValueDtos) {

        var containerExists = app.getSpec().getTemplate().getSpec().getContainers().stream()
                .anyMatch(container -> appContainerName.equals(container.getName()));
        if (!containerExists) {
            throw new IllegalArgumentException("Deployment doesn't contain container " + appContainerName);
        }

        var uuid = k8sDeployService.deploy(app, k8sClient, appMetricPortWithPath, gatlingImage);

        var test = new TuningTest()
                .setUuid(uuid)
                .setStatus(TuningTestStatus.NOT_READY)
                .setDeployment(K8sDeploymentUtil.serialize(app))
                .setDeploymentName(app.getMetadata().getName())
                .setAppContainerName(appContainerName)
                .setGatlingExecCommand(gatlingExecCommand)
                .setDeployedTime(Instant.now())
                .setStartTestTimeoutSec(startTestTimeoutSec)
                .setTestDurationSec(durationSec);

        var metricMaxValue = metricMaxValueDtos.stream()
                .map(o -> new MetricMaxValue()
                        .setMetricQueryTest(new MetricMaxValueId()
                                .setQuery(o.getQuery())
                                .setTuningTest(test)
                        )
                        .setValue(o.getMaxValue())
                )
                .collect(Collectors.toList());

        test.setMetricMaxValues(metricMaxValue);

        tuningTestRepository.save(test);

        return uuid;
    }
}
