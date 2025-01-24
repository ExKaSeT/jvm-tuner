package last.project.jvmtuner.service;

import jakarta.annotation.Nullable;
import last.project.jvmtuner.dto.metric.GetRangeMetricResponseDto;
import last.project.jvmtuner.props.MetricsProps;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final RestClient restClient;
    private final MetricsProps metricsProps;

    public GetRangeMetricResponseDto rangeRequest(String query, Instant start, Instant end, int stepSec) {
        return restClient.get()
                .uri(metricsProps.getQuery().getApi() + "/query_range?query={query}&start={start}&end={end}&step={stepSec}s",
                        query, start, end, stepSec)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(GetRangeMetricResponseDto.class)
                .getBody();
    }

    public String replaceWithTestLabels(String query, @Nullable String testUuid, @Nullable String podName,
                                        @Nullable String containerName) {
        var replaceInfo = metricsProps.getQuery().getReplaceWithLabel();
        if (nonNull(testUuid)) {
            query = query.replace(replaceInfo.getTestUuid(),
                    String.format("%s=\"%s\"", replaceInfo.getTestUuid(), testUuid));
        }
        if (nonNull(podName)) {
            query = query.replace(replaceInfo.getPodName(),
                    String.format("%s=\"%s\"", replaceInfo.getPodName(), podName));
        }
        if (nonNull(containerName)) {
            query = query.replace(replaceInfo.getContainerName(),
                    String.format("%s=\"%s\"", replaceInfo.getContainerName(), containerName));
        }

        return query;
    }
}
