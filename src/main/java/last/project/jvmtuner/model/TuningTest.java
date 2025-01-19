package last.project.jvmtuner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tuning_tests")
@Data
@Accessors(chain = true)
public class TuningTest {
    @Id
    @Column(name = "uuid", nullable = false)
    private UUID uuid;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TuningTestStatus status;

    /** Задеплоенный deployment в формате JSON */
    @Column(name = "deployment", nullable = false)
    private String deployment;

    @Column(name = "deployment_name", nullable = false)
    private String deploymentName;

    @Column(name = "pod_name")
    private String podName;

    @Column(name = "gatling_exec_command", nullable = false)
    private String gatlingExecCommand;

    /** UNIX epoch time деплоя deployment */
    @Column(name = "deployed_time", nullable = false)
    private Instant deployedTime;

    @Column(name = "start_test_timeout_sec", nullable = false)
    private Integer startTestTimeoutSec;

    /** Время запуска нагрузки */
    @Column(name = "started_test_time")
    private Instant startedTestTime;

    @Column(name = "test_duration_sec", nullable = false)
    private Integer testDurationSec;

    /** Список метрик со значениями для прерывания теста */
    @OneToMany(mappedBy = "metricNameTest.tuningTest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetricMaxValue> metricMaxValues;
}