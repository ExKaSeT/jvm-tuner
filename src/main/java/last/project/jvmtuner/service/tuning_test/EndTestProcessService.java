package last.project.jvmtuner.service.tuning_test;

import last.project.jvmtuner.dao.tuning_test.TuningTestMetricsRepository;
import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestMetrics;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import last.project.jvmtuner.props.MetricQueriesProps;
import last.project.jvmtuner.service.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EndTestProcessService {

    private final MetricService metricService;
    private final TuningTestRepository tuningTestRepository;
    private final TuningTestMetricsRepository testMetricsRepository;
    private final MetricQueriesProps metricQueriesProps;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processEndedTest(UUID testUuid) {
        var test = tuningTestRepository.findById(testUuid).get();

        test.setStatus(TuningTestStatus.PROCESSED);
        test = tuningTestRepository.save(test);

        var metrics = new TuningTestMetrics()
                .setTuningTest(test)
                .setCpuUsageAvg(this.makeQueryAndCalculateAvg(metricQueriesProps.getCpuUsageAvg(), test))
                .setCpuThrottlingAvg(this.makeQueryAndCalculateAvg(metricQueriesProps.getCpuThrottlingAvg(), test))
                .setMemoryUsageAvg(this.makeQueryAndCalculateAvg(metricQueriesProps.getMemoryUsageAvg(), test))
                .setMemoryWssAvg(this.makeQueryAndCalculateAvg(metricQueriesProps.getMemoryWssAvg(), test))
                .setMemoryRssAvg(this.makeQueryAndCalculateAvg(metricQueriesProps.getMemoryRssAvg(), test));
        testMetricsRepository.save(metrics);

        log.info(String.format("Test '%s' processed", test.getUuid()));
    }

    private double makeQueryAndCalculateAvg(String query, TuningTest test) {
        var startTime = test.getStartedTestTime();
        var endTime = startTime.plusSeconds(test.getTuningTestProps().getTestDurationSec());

        query = metricService.replaceWithTestLabels(query, test.getUuid().toString(), test.getPodName(),
                test.getTuningTestProps().getAppContainerName());
        try {
            var response = metricService.rangeRequest(query, startTime, endTime);
            return response.getData().getResult().get(0).getValues()
                    .stream()
                    .mapToDouble(value -> Double.parseDouble(value.getValue()))
                    .average()
                    .orElse(0.0);
        } catch (Exception ex) {
            throw new IllegalStateException(String.format("Failed fetching metric '%s' in test '%s'",
                    query, test.getUuid()), ex);
        }
    }
}
