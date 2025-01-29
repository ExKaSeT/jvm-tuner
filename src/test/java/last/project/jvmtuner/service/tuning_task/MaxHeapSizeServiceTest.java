package last.project.jvmtuner.service.tuning_task;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.service.tuning_task.mode.MaxHeapSizeService;
import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;
import last.project.jvmtuner.util.GetBaseData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

@AppTest
@RequiredArgsConstructor
public class MaxHeapSizeServiceTest {

    private final MaxHeapSizeService maxHeapSizeService;
    private final TuningTestPropsService tuningTestPropsService;

    @Test
    void startMaxHeapSizeTaskTest() {
        var props = GetBaseData.getTestProps(tuningTestPropsService);

        maxHeapSizeService.start(props);
    }
}
