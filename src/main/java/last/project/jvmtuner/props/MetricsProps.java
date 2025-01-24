package last.project.jvmtuner.props;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MetricsProps {
    @NotNull
    private String uuidLabelName;
    @NotNull
    private String pushUrl;
    @NotNull
    private Query query;
    @NotNull
    private TuningTest tuningTest;
    @Data
    public static class Query {
        @NotNull
        private String api;
        @NotNull
        private ReplaceWithLabel replaceWithLabel;
    }
    @Data
    public static class ReplaceWithLabel {
        @NotNull
        private String testUuid;
        @NotNull
        private String podName;
        @NotNull
        private String containerName;
    }
    @Data
    public static class TuningTest {
        @NotNull
        private Integer checkDelayAfterLoadStartSec;
    }
}