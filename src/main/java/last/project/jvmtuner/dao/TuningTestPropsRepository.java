package last.project.jvmtuner.dao;

import last.project.jvmtuner.model.TuningTestProps;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuningTestPropsRepository extends JpaRepository<TuningTestProps, Long> {
}