package last.project.jvmtuner.model.tuning_test;

import jakarta.persistence.*;
import last.project.jvmtuner.model.tuning_task.TuningTaskTest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
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

    /** UNIX epoch time деплоя deployment */
    @Column(name = "deployed_time", nullable = false)
    private Instant deployedTime;

    /** Время запуска нагрузки */
    @Column(name = "started_test_time")
    private Instant startedTestTime;

    @ManyToOne
    @JoinColumn(name = "tuning_test_props_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TuningTestProps tuningTestProps;

    @OneToOne(mappedBy = "tuningTest", fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TuningTestMetrics tuningTestMetrics;

    @OneToMany(mappedBy = "test")
    @PrimaryKeyJoinColumn
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TuningTaskTest> taskTests;
}