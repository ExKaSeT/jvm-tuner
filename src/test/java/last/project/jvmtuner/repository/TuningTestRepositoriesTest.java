package last.project.jvmtuner.repository;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.dao.TuningTestPropsRepository;
import last.project.jvmtuner.dao.TuningTestRepository;
import last.project.jvmtuner.model.*;
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
public class TuningTestRepositoriesTest {

    private final TuningTestRepository tuningTestRepository;
    private final TuningTestPropsRepository tuningTestPropsRepository;
    private UUID uuid = UUID.fromString("9438909e-e058-4b2f-9238-7294141a3081");

    @Test
    void createTuningTest() {
        var props = new TuningTestProps()
                .setPreparedDeployment("prepared")
                .setStartTestTimeoutSec(30)
                .setGatlingExecCommand("exec gatling")
                .setTestDurationSec(60)
                .setAppContainerName("app_container");

        var metricMaxValues = List.of(
                new MetricMaxValue()
                        .setValue(100)
                        .setMetricQueryProps(new MetricMaxValueId()
                                .setTuningTestProps(props)
                                .setQuery("some_metric1")),
                new MetricMaxValue()
                        .setValue(200)
                        .setMetricQueryProps(new MetricMaxValueId()
                                .setTuningTestProps(props)
                                .setQuery("some_metric2"))
        );
        props.setMetricMaxValues(new ArrayList<>(metricMaxValues));

        var test = new TuningTest()
                .setDeployment("test")
                .setDeployedTime(Instant.now())
                .setUuid(this.uuid)
                .setStatus(TuningTestStatus.NOT_READY)
                .setDeploymentName("deployment-name")
                .setTuningTestProps(props);
        props.setTuningTests(new ArrayList<>(List.of(test)));

        var expectedId = tuningTestPropsRepository.save(props).getId();
        var actualProps = tuningTestPropsRepository.getById(expectedId);
        var actualTest = tuningTestRepository.getById(uuid);

        assertEquals(props.setId(expectedId), actualProps);
        assertEquals(test, actualTest);
    }

    @Test
    void deleteTuningTest() {
        this.createTuningTest();
        var propsId = tuningTestRepository.getById(uuid).getTuningTestProps().getId();

        tuningTestPropsRepository.deleteById(propsId);

        assertThrows(JpaObjectRetrievalFailureException.class, () -> tuningTestRepository.getById(uuid));
    }
}
