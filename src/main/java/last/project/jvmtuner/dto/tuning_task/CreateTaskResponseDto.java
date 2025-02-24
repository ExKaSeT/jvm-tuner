package last.project.jvmtuner.dto.tuning_task;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateTaskResponseDto {
    private Long id;
}
