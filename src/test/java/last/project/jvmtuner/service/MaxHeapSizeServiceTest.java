package last.project.jvmtuner.service;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.service.mode.MaxHeapSizeService;
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
    void startTaskTest() {
        var props = GetBaseData.getTestProps(tuningTestPropsService);

        maxHeapSizeService.start(props);
    }
}
