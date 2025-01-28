package last.project.jvmtuner.dto.mode.max_heap_size;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class MaxHeapSizeDto {
    private UUID currentTest;
    private Integer minHeapSize;
    private Integer maxHeapSize;
    private Integer retryCount;
}
