package last.project.jvmtuner.dto.tuning_test;

import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class TuningTestResponseDto {
    private UUID uuid;
    private TuningTestStatus status;
    private String description;
    private String podName;
    private Instant deployedTime;
    private Instant startedTestTime;
    private String deployment;
    private TestMetricsDto testMetricsDto;
    @Data
    @Accessors(chain = true)
    public static class TestMetricsDto {
        private Double cpuUsageAvg;
        private Double cpuThrottlingAvg;
        private Double memoryUsageAvg;
        private Double memoryWssAvg;
        private Double memoryRssAvg;
    }
}