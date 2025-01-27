package last.project.jvmtuner.model.tuning_task;

import jakarta.persistence.*;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Entity
@Table(name = "tuning_tasks_tests")
@IdClass(TuningTaskTestId.class)
@Data
@Accessors(chain = true)
public class TuningTaskTest {

    @Id
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Id
    @Column(name = "tuning_test_uuid", nullable = false)
    private UUID tuningTestUuid;

    @Column(name = "description", nullable = false)
    private String description;

    /** Показывает, обработан ли завершенный тест */
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @ManyToOne
    @MapsId
    @JoinColumn(name = "task_id", nullable = false)
    private TuningTask task;

    @ManyToOne
    @MapsId
    @JoinColumn(name = "tuning_test_uuid", nullable = false)
    private TuningTest test;
}
