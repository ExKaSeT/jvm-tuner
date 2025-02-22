package last.project.jvmtuner.dto.tuning_test;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TuningTestPropsPreviewResponseDto {
    private Long id;
    private String description;
}
