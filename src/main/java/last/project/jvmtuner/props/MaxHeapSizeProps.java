package last.project.jvmtuner.props;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MaxHeapSizeProps {
    @NotNull
    @Min(1)
    @Max(50)
    private Integer endStepPercent;
    @NotNull
    @Min(0)
    private Integer retryOnFailCount;
    @NotBlank
    private String minHeapSizeMbQuery;
}