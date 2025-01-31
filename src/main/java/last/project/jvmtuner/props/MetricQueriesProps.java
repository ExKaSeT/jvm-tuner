package last.project.jvmtuner.props;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MetricQueriesProps {
    @NotBlank
    private String cpuUsageAvg;
    @NotBlank
    private String cpuThrottlingAvg;
    @NotBlank
    private String memoryUsageAvg;
    @NotBlank
    private String memoryWssAvg;
    @NotBlank
    private String memoryRssAvg;
}