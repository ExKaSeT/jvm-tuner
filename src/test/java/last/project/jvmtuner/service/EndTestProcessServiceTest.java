package last.project.jvmtuner.service;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.dao.tuning_test.TuningTestMetricsRepository;
import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import last.project.jvmtuner.service.tuning_test.EndTestProcessService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@AppTest
@Transactional
@RequiredArgsConstructor
public class EndTestProcessServiceTest {

    private static final UUID testUuid = UUID.fromString("2a884ce7-6c59-49e7-89d5-2b7f0a9145a9");

    private final TuningTestRepository tuningTestRepository;
    private final TuningTestMetricsRepository tuningTestMetricsRepository;
    private final EndTestProcessService endTestProcessService;

    @Test
    void processEndedTestTest() {
        tuningTestMetricsRepository.deleteById(testUuid);
        var test = tuningTestRepository.findById(testUuid).get();

        endTestProcessService.processEndedTest(testUuid);
        var metrics = tuningTestMetricsRepository.findById(testUuid).get();

        assertEquals(TuningTestStatus.PROCESSED, test.getStatus());
        assertNotNull(metrics.getCpuUsageAvg());
        assertNotNull(metrics.getCpuThrottlingAvg());
        assertNotNull(metrics.getMemoryUsageAvg());
        assertNotNull(metrics.getMemoryWssAvg());
        assertNotNull(metrics.getMemoryRssAvg());
    }
}
