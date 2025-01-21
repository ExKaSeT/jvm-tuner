package last.project.jvmtuner.dao;

import last.project.jvmtuner.model.TuningTest;
import last.project.jvmtuner.model.TuningTestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TuningTestRepository extends JpaRepository<TuningTest, UUID> {

    List<TuningTest> getAllByStatus(TuningTestStatus status);
}