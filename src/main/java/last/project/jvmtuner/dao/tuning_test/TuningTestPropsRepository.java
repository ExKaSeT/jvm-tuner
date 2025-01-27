package last.project.jvmtuner.dao.tuning_test;

import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuningTestPropsRepository extends JpaRepository<TuningTestProps, Long> {
}