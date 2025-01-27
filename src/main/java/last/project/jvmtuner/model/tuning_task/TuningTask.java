package last.project.jvmtuner.model.tuning_task;

import jakarta.persistence.*;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "tuning_tasks")
@Data
@Accessors(chain = true)
public class TuningTask {

    @Id
    @GeneratedValue(generator = "tuning_tasks_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "tuning_tasks_id_seq", sequenceName = "tuning_tasks_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private TuningTaskMode mode;

    @Column(name = "mode_data", nullable = false)
    private String modeData;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TuningTaskStatus status;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "completed_time")
    private Instant completedTime;

    @ManyToOne
    @JoinColumn(name = "tuning_test_props_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TuningTestProps tuningTestProps;

    @OneToMany(mappedBy = "task")
    @PrimaryKeyJoinColumn
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TuningTaskTest> taskTests;
}
