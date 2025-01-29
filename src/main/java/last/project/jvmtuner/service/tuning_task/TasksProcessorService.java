package last.project.jvmtuner.service.tuning_task;

import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTaskTestId;
import last.project.jvmtuner.service.tuning_task.mode.MaxHeapSizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class TasksProcessorService {

    private final TuningTaskTestService taskTestService;
    private final MaxHeapSizeService maxHeapSizeService;
    private final TuningTaskService taskService;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public void processTasks() {
        taskTestService.getAllNonProcessedCompletedTests(TuningMode.MAX_HEAP_SIZE).stream()
                .map(taskTest -> new TuningTaskTestId()
                        .setTaskId(taskTest.getTaskId())
                        .setTuningTestUuid(taskTest.getTuningTestUuid())
                )
                .forEach(
                        id -> CompletableFuture
                                .runAsync(() -> maxHeapSizeService.process(id.getTaskId(), id.getTuningTestUuid()), executor)
                                .exceptionally(getFailTaskFunction(id.getTaskId()))
                );

        log.info("Checked tasks for processing");
    }

    private Function<Throwable, Void> getFailTaskFunction(Long taskId) {
        return ex -> {
            taskService.failTask(taskId, ex);
            return null;
        };
    }
}
