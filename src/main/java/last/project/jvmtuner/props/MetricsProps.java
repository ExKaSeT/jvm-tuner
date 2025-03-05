package last.project.jvmtuner.props;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MetricsProps {
    @NotBlank
    private String uuidLabelName;
    @NotBlank
    private String pushUrl;
    @NotNull
    private Query query;
    @NotNull
    private TuningTest tuningTest;
    private String grafanaBaseUrl;
    @Data
    public static class Query {
        @NotBlank
        private String api;
        @NotNull
        @Min(5)
        private Integer stepSec;
        @NotNull
        private ReplaceWithLabel replaceWithLabel;
    }
    @Data
    public static class ReplaceWithLabel {
        @NotBlank
        private String testUuid;
        @NotBlank
        private String podName;
        @NotBlank
        private String containerName;
    }
    @Data
    public static class TuningTest {
        @NotNull
        @Min(0)
        private Integer checkDelayAfterLoadStartSec;
    }
}