package last.project.jvmtuner.props;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParallelGCProps {
    @NotNull
    @Min(0)
    private Integer retryOnFailCount;
    @NotNull
    @Min(1)
    private Integer initialThreads;
    @NotNull
    @Min(1)
    @Max(50)
    private Integer maxThreads;
    @Min(1)
    @Max(50)
    private Integer step;
}