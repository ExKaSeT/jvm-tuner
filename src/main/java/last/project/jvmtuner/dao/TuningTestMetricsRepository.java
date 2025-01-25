package last.project.jvmtuner.dao;

import last.project.jvmtuner.model.TuningTestMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TuningTestMetricsRepository extends JpaRepository<TuningTestMetrics, UUID> {
}