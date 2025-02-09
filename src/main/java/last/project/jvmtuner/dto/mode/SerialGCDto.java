package last.project.jvmtuner.dto.mode;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class SerialGCDto {
    private UUID currentTest;
    private Double cpuUsageAvg;
    private Integer oldGenSize;
    private Integer heapSize;
    private Integer retryCount;
    private Boolean foundMinWorkOldGen;
}
