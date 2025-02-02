package last.project.jvmtuner.model.tuning_test;

import last.project.jvmtuner.dao.tuning_test.TuningTestMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TuningTestMetricsService {

    private final TuningTestMetricsRepository testMetricsRepository;

    public int compareByCpuUsage(UUID testA, UUID testB) {
        var a = testMetricsRepository.findById(testA).get();
        var b = testMetricsRepository.findById(testB).get();
        return Double.compare(a.getCpuUsageAvg(), b.getCpuUsageAvg());
    }
}