package last.project.jvmtuner.service.tuning_test;

import jakarta.annotation.Nullable;
import last.project.jvmtuner.dao.tuning_test.TuningTestPropsRepository;
import last.project.jvmtuner.dto.tuning_test.TuningTestPropsWithModesResponseDto;
import last.project.jvmtuner.dto.tuning_test.MetricMaxValueDto;
import last.project.jvmtuner.dto.tuning_test.TuningTestPropsPreviewResponseDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_test.MetricMaxValue;
import last.project.jvmtuner.model.tuning_test.MetricMaxValueId;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import last.project.jvmtuner.util.SerializationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TuningTestPropsService {

    private final TuningTestPropsRepository testPropsRepository;
    private final K8sDeployService k8sDeployService;

    public TuningTestProps saveTuningTestProps(String deployment, String appContainerName, String appMetricPortWithPath,
                                               String gatlingImage, String gatlingExecCommand,
                                               int startTestTimeoutSec, int testDurationSec,
                                               List<MetricMaxValueDto> metricMaxValueDto,
                                               @Nullable String description) {
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
                .setStartTestTimeoutSec(startTestTimeoutSec)
                .setDescription(description);

        return testPropsRepository.save(props);
    }

    public List<TuningTestPropsPreviewResponseDto> getAllProps() {
        return testPropsRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(prop -> new TuningTestPropsPreviewResponseDto()
                        .setId(prop.getId())
                        .setDescription(prop.getDescription()))
                .toList();
    }

    @Transactional
    public void delete(long propId) {
        testPropsRepository.deleteById(propId);
    }

    @Transactional
    public TuningTestPropsWithModesResponseDto getPropsWithModes(long propsId) {
        var props = testPropsRepository.findById(propsId).get();
        var metricValuesList = props.getMetricMaxValues().stream()
                .map(metricValue -> new MetricMaxValueDto()
                        .setQuery(metricValue.getMetricQueryProps().getQuery())
                        .setMaxValue(metricValue.getValue())
                ).toList();
        return new TuningTestPropsWithModesResponseDto()
                .setId(propsId)
                .setDescription(props.getDescription())
                .setAppContainerName(props.getAppContainerName())
                .setGatlingExecCommand(props.getGatlingExecCommand())
                .setStartTestTimeoutSec(props.getStartTestTimeoutSec())
                .setTestDurationSec(props.getTestDurationSec())
                .setPreparedDeployment(SerializationUtil.beautifyJSON(props.getPreparedDeployment()))
                .setMetricMaxValues(metricValuesList)
                .setTuningModes(TuningMode.getAvailableModes());
    }

    public TuningTestProps get(long propId) {
        return testPropsRepository.findById(propId).get();
    }
}
