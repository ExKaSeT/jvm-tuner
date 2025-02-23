package last.project.jvmtuner.dto.tuning_test;

import last.project.jvmtuner.model.tuning_task.TuningMode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class TuningTestPropsWithModesResponseDto {
    private Long id;
    private String description;
    private String appContainerName;
    private String gatlingExecCommand;
    private Integer startTestTimeoutSec;
    private Integer testDurationSec;
    private String preparedDeployment;
    private List<MetricMaxValueDto> metricMaxValues;
    private List<TuningMode> tuningModes;
}
