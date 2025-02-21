package last.project.jvmtuner.props;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShenandoahGCProps {
    @NotNull
    @Min(0)
    private Integer retryOnFailCount;
}