package last.project.jvmtuner.dto.tuning_test;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MetricMaxValueDto {

    @NotBlank(message = "Запрос не может быть пустым")
    private String query;

    @NotNull(message = "Необходимо указать максимальное значение")
    @Min(value = 0, message = "Максимальное значение не может быть отрицательным")
    private Long maxValue;
}