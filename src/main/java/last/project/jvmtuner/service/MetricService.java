package last.project.jvmtuner.service;

import last.project.jvmtuner.dto.metric.GetRangeMetricResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final RestClient restClient;

    @Value("${metrics.query-api}")
    private String getMetricsUrl;

    public GetRangeMetricResponseDto rangeRequest(String query, Instant start, Instant end, int stepSec) {
        return restClient.get()
                .uri(getMetricsUrl + "/query_range?query={query}&start={start}&end={end}&step={stepSec}s",
                        query, start, end, stepSec)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(GetRangeMetricResponseDto.class)
                .getBody();
    }
}
