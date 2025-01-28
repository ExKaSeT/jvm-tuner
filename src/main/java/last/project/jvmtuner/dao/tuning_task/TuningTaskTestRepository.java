package last.project.jvmtuner.dao.tuning_task;

import last.project.jvmtuner.model.tuning_task.TuningTaskTest;
import last.project.jvmtuner.model.tuning_task.TuningTaskTestId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuningTaskTestRepository extends JpaRepository<TuningTaskTest, TuningTaskTestId> {
}