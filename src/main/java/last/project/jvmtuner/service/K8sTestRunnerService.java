package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import last.project.jvmtuner.model.MetricMaxValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class K8sTestRunnerService {

    private final K8sDeployService k8sDeployService;

    public void runTest(Deployment app, String appContainerName, String appMetricPortWithPath,
                        String gatlingImage, String gatlingExecCommand,
                        int durationSec, int startTestTimeoutSec,
                        List<MetricMaxValue> metricMaxValues) {

    }
}
