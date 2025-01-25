package last.project.jvmtuner.props;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MetricQueriesProps {
    @NotNull
    private String cpuUsageAvg;
    @NotNull
    private String cpuThrottlingAvg;
    @NotNull
    private String memoryUsageAvg;
    @NotNull
    private String memoryWssAvg;
    @NotNull
    private String memoryRssAvg;
}