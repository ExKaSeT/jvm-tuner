package last.project.jvmtuner.dto.tuning_task;

import last.project.jvmtuner.dto.tuning_test.TuningTestResponseDto;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class TuningTaskDetailsResponseDto {
    private TuningTaskResponseDto taskDto;
    private List<TuningTestResponseDto> testDto;
}
