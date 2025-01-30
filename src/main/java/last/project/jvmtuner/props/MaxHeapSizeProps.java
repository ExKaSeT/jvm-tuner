package last.project.jvmtuner.props;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaxHeapSizeProps {
    @NotNull
    private Integer endStepMb;
    @NotNull
    private Integer retryOnFailCount;
    @NotNull
    private String minHeapSizeMbQuery;
}