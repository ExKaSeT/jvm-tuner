package last.project.jvmtuner.dao.tuning_task;

import last.project.jvmtuner.model.tuning_task.TuningTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuningTaskRepository extends JpaRepository<TuningTask, Long> {
}