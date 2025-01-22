package last.project.jvmtuner.service;

import last.project.jvmtuner.annotation.AppTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;

@AppTest
@RequiredArgsConstructor
class MetricServiceTest {

    private final MetricService metricService;

    @Test
    void getRangeMetricTest() {
        var result = metricService
                .rangeRequest("gatling_count_total",
                Instant.ofEpochSecond(1737562261L), Instant.ofEpochSecond(1737562501L), 15);

        System.out.println(result);
    }
}
