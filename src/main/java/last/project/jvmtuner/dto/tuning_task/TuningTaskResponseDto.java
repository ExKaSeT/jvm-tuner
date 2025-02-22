package last.project.jvmtuner.dto.tuning_task;

import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTaskStatus;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
public class TuningTaskResponseDto {
        private Long id;
        private TuningMode mode;
        private TuningTaskStatus status;
        private Instant createdTime;
        private Instant completedTime;
}
