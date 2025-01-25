package last.project.jvmtuner.service;

import last.project.jvmtuner.dao.TuningTestMetricsRepository;
import last.project.jvmtuner.dao.TuningTestRepository;
import last.project.jvmtuner.model.TuningTest;
import last.project.jvmtuner.model.TuningTestMetrics;
import last.project.jvmtuner.model.TuningTestStatus;
import last.project.jvmtuner.props.MetricQueriesProps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EndTestProcessService {

    private final MetricService metricService;
    private final TuningTestRepository tuningTestRepository;
    private final TuningTestMetricsRepository testMetricsRepository;
    private final MetricQueriesProps metricQueriesProps;

    @Transactional
    public void processEndTest(TuningTest test) {
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
