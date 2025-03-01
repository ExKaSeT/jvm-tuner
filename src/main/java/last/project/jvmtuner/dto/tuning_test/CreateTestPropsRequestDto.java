package last.project.jvmtuner.dto.tuning_test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class CreateTestPropsRequestDto {

    private String description;

    @NotBlank(message = "Имя контейнера не может быть пустым")
    private String appContainerName;

    @NotBlank(message = "Порт с путем сбора метрик приложения не может быть пустым")
    private String appMetricPortWithPath;

    @NotBlank(message = "Команда запуска Gatling не может быть пустой")
    private String gatlingExecCommand;

    @NotBlank(message = "Образ с Gatling не может быть пустой")
    private String gatlingImage;

    @NotNull(message = "Необходимо указать таймаут старта теста")
    @Min(value = 0, message = "Таймаут старта теста не может быть отрицательным")
    private Integer startTestTimeoutSec;

    @NotNull(message = "Необходимо указать длительность теста")
    @Min(value = 0, message = "Длительность теста не может быть отрицательной")
    private Integer testDurationSec;

    @NotBlank(message = "Развертывание не может быть пустым")
    private String deployment;

    @Valid
    private List<MetricMaxValueDto> metricMaxValues;
}