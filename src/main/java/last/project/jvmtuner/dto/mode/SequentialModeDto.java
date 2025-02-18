package last.project.jvmtuner.dto.mode;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class SequentialModeDto {
    private UUID currentTest;
    private Integer retryCount;
    private List<List<String>> testOptionsList;
    private Integer currentOptionsIndex;
}
