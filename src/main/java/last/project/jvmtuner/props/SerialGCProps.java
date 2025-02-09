package last.project.jvmtuner.props;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SerialGCProps {
    @NotNull
    @Min(0)
    private Integer retryOnFailCount;
    @NotBlank
    private String minOldGenSizeMbQuery;
    @NotNull
    @Min(0)
    @Max(50)
    private Integer stepPercent;
}