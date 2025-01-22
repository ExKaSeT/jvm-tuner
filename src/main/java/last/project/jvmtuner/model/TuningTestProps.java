package last.project.jvmtuner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Entity
@Table(name = "tuning_test_props")
@Data
@Accessors(chain = true)
public class TuningTestProps {
    @Id
    @GeneratedValue(generator = "tuning_test_props_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "tuning_test_props_id_seq", sequenceName = "tuning_test_props_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "prepared_deployment", nullable = false)
    private String preparedDeployment;

    @Column(name = "app_container_name", nullable = false)
    private String appContainerName;

    @Column(name = "gatling_exec_command", nullable = false)
    private String gatlingExecCommand;

    @Column(name = "start_test_timeout_sec", nullable = false)
    private Integer startTestTimeoutSec;

    @Column(name = "test_duration_sec", nullable = false)
    private Integer testDurationSec;

    /** Список метрик со значениями для прерывания теста */
    @OneToMany(mappedBy = "metricQueryProps.tuningTestProps", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetricMaxValue> metricMaxValues;

    @OneToMany(mappedBy = "tuningTestProps", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TuningTest> tuningTests;
}