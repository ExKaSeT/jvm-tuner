package last.project.jvmtuner.service;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.dao.TuningTestMetricsRepository;
import last.project.jvmtuner.dao.TuningTestRepository;
import last.project.jvmtuner.model.TuningTestStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@AppTest
@Transactional
@RequiredArgsConstructor
class EndTestProcessServiceTest {

    private static final UUID testUuid = UUID.fromString("2a884ce7-6c59-49e7-89d5-2b7f0a9145a9");

    private final TuningTestRepository tuningTestRepository;
    private final TuningTestMetricsRepository tuningTestMetricsRepository;
    private final EndTestProcessService endTestProcessService;

    @Test
    void getRangeMetricTest() {
        var test = tuningTestRepository.getById(testUuid);

        endTestProcessService.processEndTest(test);
        var metrics = tuningTestMetricsRepository.getById(testUuid);

        assertEquals(TuningTestStatus.PROCESSED, test.getStatus());
        assertNotNull(metrics.getCpuUsageAvg());
        assertNotNull(metrics.getCpuThrottlingAvg());
        assertNotNull(metrics.getMemoryUsageAvg());
        assertNotNull(metrics.getMemoryWssAvg());
        assertNotNull(metrics.getMemoryRssAvg());
    }
}
