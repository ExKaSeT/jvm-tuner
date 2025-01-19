package last.project.jvmtuner.repository;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.dao.TuningTestRepository;
import last.project.jvmtuner.model.MetricMaxValue;
import last.project.jvmtuner.model.MetricMaxValueId;
import last.project.jvmtuner.model.TuningTest;
import last.project.jvmtuner.model.TuningTestStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AppTest
@Transactional
@RequiredArgsConstructor
public class TuningTestRepositoryTest {

    private final TuningTestRepository tuningTestRepository;
    private UUID uuid = UUID.fromString("9438909e-e058-4b2f-9238-7294141a3081");

    @Test
    void createTuningTest() {
        var test = new TuningTest()
                .setTestDurationSec(60)
                .setStartTestTimeoutSec(30)
                .setDeployment("test")
                .setDeployedTime(Instant.now())
                .setUuid(this.uuid)
                .setStatus(TuningTestStatus.NOT_READY)
                .setDeploymentName("deployment-name")
                .setGatlingExecCommand("exec gatling")
                .setAppContainerName("app_container");
        var metricMaxValues = List.of(
                new MetricMaxValue()
                        .setValue(100)
                        .setMetricQueryTest(new MetricMaxValueId()
                                .setTuningTest(test)
                                .setQuery("some_metric1")),
                new MetricMaxValue()
                        .setValue(200)
                        .setMetricQueryTest(new MetricMaxValueId()
                                .setTuningTest(test)
                                .setQuery("some_metric2"))
        );
        test.setMetricMaxValues(new ArrayList<>(metricMaxValues));

        tuningTestRepository.save(test);
        var actual = tuningTestRepository.getById(uuid);

        assertEquals(test, actual);
    }

    @Test
    void deleteTuningTest() {
        this.createTuningTest();

        tuningTestRepository.deleteById(uuid);

        assertThrows(JpaObjectRetrievalFailureException.class, () -> tuningTestRepository.getById(uuid));
    }
}
