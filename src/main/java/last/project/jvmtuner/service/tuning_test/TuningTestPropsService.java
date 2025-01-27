package last.project.jvmtuner.service.tuning_test;

import last.project.jvmtuner.dao.tuning_test.TuningTestPropsRepository;
import last.project.jvmtuner.dto.tuning_test.MetricMaxValueDto;
import last.project.jvmtuner.model.tuning_test.MetricMaxValue;
import last.project.jvmtuner.model.tuning_test.MetricMaxValueId;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TuningTestPropsService {

    private final TuningTestPropsRepository tuningTestPropsRepository;
    private final K8sDeployService k8sDeployService;

    public TuningTestProps saveTuningTestProps(String deployment, String appContainerName, String appMetricPortWithPath,
                                               String gatlingImage, String gatlingExecCommand,
                                               int startTestTimeoutSec, int testDurationSec,
                                               List<MetricMaxValueDto> metricMaxValueDto) {
        var props = new TuningTestProps();
        var metricMaxValues = metricMaxValueDto.stream()
                .map(o -> new MetricMaxValue()
                        .setMetricQueryProps(new MetricMaxValueId()
                                .setQuery(o.getQuery())
                                .setTuningTestProps(props)
                        )
                        .setValue(o.getMaxValue())
                )
                .collect(Collectors.toList());

        var app = K8sDeploymentUtil.deserialize(deployment);
        k8sDeployService.prepareDeployment(app, appMetricPortWithPath, gatlingImage, appContainerName);

        props.setMetricMaxValues(metricMaxValues)
                .setPreparedDeployment(K8sDeploymentUtil.serialize(app))
                .setAppContainerName(appContainerName)
                .setGatlingExecCommand(gatlingExecCommand)
                .setTestDurationSec(testDurationSec)
                .setStartTestTimeoutSec(startTestTimeoutSec);

        return tuningTestPropsRepository.save(props);
    }
}
