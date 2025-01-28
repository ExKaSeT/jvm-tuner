package last.project.jvmtuner.service.tuning_test;

import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static last.project.jvmtuner.model.tuning_test.TuningTestStatus.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestsProcessorService {

    private final RunningTestCheckerService checkService;
    private final TuningTestRepository tuningTestRepository;
    private final EndTestProcessService endTestProcessService;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public void processTests() {
        tuningTestRepository.getAllByStatus(NOT_READY).stream()
                .map(TuningTest::getUuid)
                .forEach(
                        uuid -> CompletableFuture
                                .runAsync(() -> checkService.checkNotReadyTest(uuid), executor)
                                .exceptionally(getFailTestFunction(uuid, FAILED_READY))
                );

        tuningTestRepository.getAllByStatus(RUNNING).stream()
                .map(TuningTest::getUuid)
                .forEach(
                        uuid -> CompletableFuture
                                .runAsync(() -> checkService.checkRunningTest(uuid), executor)
                                .exceptionally(getFailTestFunction(uuid, FAILED_RUNNING))
                );

        tuningTestRepository.getAllByStatus(ENDED).stream()
                .map(TuningTest::getUuid)
                .forEach(
                        uuid -> CompletableFuture
                                .runAsync(() -> endTestProcessService.processEndedTest(uuid), executor)
                                .exceptionally(getFailTestFunction(uuid, FAILED_PROCESSING))
                );

        log.info("Checked tests for processing");
    }

    private Function<Throwable, Void> getFailTestFunction(UUID test, TuningTestStatus status) {
        return ex -> {
            checkService.failTest(test, status, ex);
            checkService.deploymentReplicasToZero(test);
            return null;
        };
    }
}
