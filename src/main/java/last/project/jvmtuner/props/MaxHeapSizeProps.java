package last.project.jvmtuner.props;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MaxHeapSizeProps {
    @NotNull
    @Min(1)
    @Max(50)
    private Integer startStepPercent;
    @NotNull
    @Min(1)
    @Max(50)
    private Integer endStepPercent;
    @NotNull
    @Min(1)
    @Max(10)
    private Double stepDivider;
    @NotNull
    @Min(0)
    private Integer retryOnFailCount;
    @NotBlank
    private String heapSizeMbQuery;
}