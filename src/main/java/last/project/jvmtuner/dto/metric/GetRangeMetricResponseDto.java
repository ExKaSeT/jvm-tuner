package last.project.jvmtuner.dto.metric;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class GetRangeMetricResponseDto {
    private String status;
    private MetricsData data;

    @Data
    public static class MetricsData {
        @JsonProperty("resultType")
        private String resultType;
        private List<MetricResult> result;
    }

    @Data
    public static class MetricResult {
        private Metric metric;
        @JsonDeserialize(using = ValueDeserializer.class)
        private List<Value> values;
    }

    @Data
    public static class Metric {
        @JsonProperty("__name__")
        private String name;

        private Map<String, String> labels = new HashMap<>();

        @JsonAnySetter
        public void addLabel(String key, String value) {
            labels.put(key, value);
        }
    }

    @Data
    public static class Value {
        private long epochTime;
        private String value;
    }
}