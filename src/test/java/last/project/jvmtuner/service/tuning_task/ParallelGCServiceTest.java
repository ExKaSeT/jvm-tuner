package last.project.jvmtuner.service.tuning_task;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.service.tuning_task.mode.ParallelGCService;
import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;
import last.project.jvmtuner.util.GetBaseData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

@AppTest
@RequiredArgsConstructor
public class ParallelGCServiceTest {

    private final ParallelGCService parallelGCService;
    private final TuningTestPropsService tuningTestPropsService;

    @Test
    void startSerialGCTest() {
        var props = GetBaseData.getTestProps(tuningTestPropsService);

        parallelGCService.start(props);
    }
}
