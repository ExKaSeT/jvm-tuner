package last.project.jvmtuner.service.tuning_task.mode;

import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;

import java.util.UUID;

public interface TuningModeService {

    TuningTask start(TuningTestProps testProps);

    void process(long taskId, UUID testUuid);

    TuningMode getTuningMode();
}
