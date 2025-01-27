package last.project.jvmtuner.model.tuning_task;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class TuningTaskTestId implements Serializable {
    private Long taskId;
    private UUID tuningTestUuid;
}
