package last.project.jvmtuner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String serialize(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String object, Class<T> clazz) {
        try {
            return mapper.readValue(object, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
