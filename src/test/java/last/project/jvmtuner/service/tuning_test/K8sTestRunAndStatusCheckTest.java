package last.project.jvmtuner.service.tuning_test;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.dao.tuning_test.TuningTestMetricsRepository;
import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import last.project.jvmtuner.util.GetBaseData;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

@AppTest
@Slf4j
@Transactional
@Rollback(false)
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor
public class K8sTestRunAndStatusCheckTest {

    private final K8sTestRunnerService k8sTestRunnerService;
    private final TuningTestPropsService tuningTestPropsService;
    private final RunningTestCheckerService runningTestCheckerService;
    private final TuningTestRepository tuningTestRepository;
    private final TuningTestMetricsRepository tuningTestMetricsRepository;
    private final EndTestProcessService endTestProcessService;

    private UUID testUuid;

    @Test
    @Order(0)
    void runTestTest() {
        var props = GetBaseData.getTestProps(tuningTestPropsService);

        this.testUuid = k8sTestRunnerService.runTest(props, K8sDeploymentUtil
                .addJvmOptions(List.of("-XX:+PrintCommandLineFlags", "-Xmx470M", "-Xms470M"), "crypto"))
                .getUuid();
        log.info("UUID of test: " + this.testUuid);
    }

    @Test
    @Order(1)
    void notReadyStatusCheckTest() throws InterruptedException {
        runningTestCheckerService.checkNotReadyTest(this.testUuid);
        Thread.sleep(50_000);
        runningTestCheckerService.checkNotReadyTest(this.testUuid);

        var test = tuningTestRepository.findById(this.testUuid).get();

        assertEquals(TuningTestStatus.RUNNING, test.getStatus());
    }

    @Test
    @Order(2)
    void runningStatusCheckTest() throws InterruptedException {
        var testDurationSec = tuningTestRepository
                .findById(this.testUuid).get()
                .getTuningTestProps()
                .getTestDurationSec();

        runningTestCheckerService.checkRunningTest(this.testUuid);
        Thread.sleep(testDurationSec / 2 * 1000L);
        runningTestCheckerService.checkRunningTest(this.testUuid);
        Thread.sleep(testDurationSec / 2 * 1000L);
        runningTestCheckerService.checkRunningTest(this.testUuid);

        var test = tuningTestRepository.findById(this.testUuid).get();

        assertEquals(TuningTestStatus.ENDED, test.getStatus());
    }

    @Test
    @Order(3)
    void endedStatusCheckTest() {
        endTestProcessService.processEndedTest(this.testUuid);

        var test = tuningTestRepository.findById(this.testUuid).get();
        var metrics = tuningTestMetricsRepository.findById(this.testUuid).get();

        assertEquals(TuningTestStatus.PROCESSED, test.getStatus());
        assertNotNull(metrics.getCpuUsageAvg());
        assertNotNull(metrics.getCpuThrottlingAvg());
        assertNotNull(metrics.getMemoryUsageAvg());
        assertNotNull(metrics.getMemoryWssAvg());
        assertNotNull(metrics.getMemoryRssAvg());
    }
}
