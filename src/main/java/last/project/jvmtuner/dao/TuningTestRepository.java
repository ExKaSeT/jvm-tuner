package last.project.jvmtuner.dao;

import last.project.jvmtuner.model.TuningTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TuningTestRepository extends JpaRepository<TuningTest, UUID> {
}