package last.project.jvmtuner.dto.mode;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class ParallelGCDto {
    private UUID currentTest;
    private Double cpuUsageAvg;
    private Integer threads;
    private Integer retryCount;
}
