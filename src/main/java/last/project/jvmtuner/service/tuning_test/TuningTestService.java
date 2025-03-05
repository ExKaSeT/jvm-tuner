package last.project.jvmtuner.service.tuning_test;

import jakarta.annotation.Nullable;
import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import last.project.jvmtuner.props.MetricsProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class TuningTestService {

    private final TuningTestRepository testRepository;
    private final MetricsProps metricsProps;

    @Transactional
    public TuningTest get(UUID testUuid) {
        return testRepository.findById(testUuid).get();
    }

    @Transactional
    public TuningTest update(UUID testUuid, @Nullable TuningTestStatus status, @Nullable String podName,
                             @Nullable Instant startedTestTime) {
        var test = testRepository.findById(testUuid).get();
        if (nonNull(status)) {
            test.setStatus(status);
        }
        if (nonNull(podName)) {
            test.setPodName(podName);
        }
        if (nonNull(startedTestTime)) {
            test.setStartedTestTime(startedTestTime);
        }
        return testRepository.save(test);
    }

    public void checkTestTimeout(TuningTest test) {
        var timeout = Instant.now().getEpochSecond() - test.getDeployedTime().getEpochSecond() >
                test.getTuningTestProps().getStartTestTimeoutSec();
        if (timeout) {
            throw new IllegalStateException(String.format("Test '%s' timeout", test.getUuid()));
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void failTest(UUID testUuid, TuningTestStatus newStatus, Throwable ex) {
        var test = testRepository.findById(testUuid).get();
        test.setStatus(newStatus);
        testRepository.save(test);
        log.error(String.format("Test '%s' failed: %s", test.getUuid(), test.getStatus().name()), ex);
    }

    @Transactional
    public List<TuningTest> getAll(TuningTestStatus testStatus) {
        return testRepository.getAllByStatus(testStatus);
    }

    public void delete(UUID testUuid) {
        testRepository.deleteById(testUuid);
    }

    @Nullable
    public String getGrafanaUrl(TuningTest test, TuningTestProps testProps) {
        var baseUrl = metricsProps.getGrafanaBaseUrl();
        var podName = test.getPodName();
        if (isNull(baseUrl) || baseUrl.isBlank() || isNull(podName)) {
            return null;
        }
        long startTestTime = test.getStartedTestTime().toEpochMilli();
        long endTestTime = startTestTime + testProps.getTestDurationSec() * 1000;
        return String.format("%s&from=%dZ&to=%dZ&var-Pod=%s&var-TunerId=%s", baseUrl, startTestTime, endTestTime,
               podName, test.getUuid());
    }
}
