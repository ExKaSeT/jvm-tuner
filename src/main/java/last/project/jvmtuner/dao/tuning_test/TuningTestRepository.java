package last.project.jvmtuner.dao.tuning_test;

import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TuningTestRepository extends JpaRepository<TuningTest, UUID> {

    List<TuningTest> getAllByStatus(TuningTestStatus status);
}