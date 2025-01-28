package last.project.jvmtuner.service.mode;

import last.project.jvmtuner.model.tuning_test.TuningTestProps;

public interface TuningModeService {

    void start(TuningTestProps testProps);

    void check();
}
