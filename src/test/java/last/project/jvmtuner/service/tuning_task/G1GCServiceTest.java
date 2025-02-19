package last.project.jvmtuner.service.tuning_task;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.service.tuning_task.mode.G1GCService;
import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;
import last.project.jvmtuner.util.GetBaseData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

@AppTest
@RequiredArgsConstructor
public class G1GCServiceTest {

    private final G1GCService g1GCService;
    private final TuningTestPropsService tuningTestPropsService;

    @Test
    void startSerialGCTest() {
        var props = GetBaseData.getTestProps(tuningTestPropsService);

        g1GCService.start(props);
    }
}
