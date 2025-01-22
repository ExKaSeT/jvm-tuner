package last.project.jvmtuner.dto.metric;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static last.project.jvmtuner.dto.metric.GetRangeMetricResponseDto.*;

public class ValueDeserializer extends JsonDeserializer<List<Value>> {
    @Override
    public List<Value> deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        List<Value> values = new ArrayList<>();
        while (nonNull(parser.nextToken())) {
            var array = parser.readValueAs(List.class);
            if (isNull(array)) {
                break;
            }
            if (array.size() == 2) {
                var value = new Value();
                value.setValue(array.get(1).toString());
                value.setEpochTime(((Number) array.get(0)).longValue());
                values.add(value);
            } else {
                throw new IllegalArgumentException("Invalid format for Value: " + array);
            }
        }
        return values;
    }
}