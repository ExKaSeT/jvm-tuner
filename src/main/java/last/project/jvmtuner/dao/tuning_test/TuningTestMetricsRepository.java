package last.project.jvmtuner.dao.tuning_test;

import last.project.jvmtuner.model.tuning_test.TuningTestMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TuningTestMetricsRepository extends JpaRepository<TuningTestMetrics, UUID> {
}