package last.project.jvmtuner.service.tuning_task;

import jakarta.annotation.PostConstruct;
import last.project.jvmtuner.model.tuning_task.TuningTaskTestId;
import last.project.jvmtuner.service.tuning_task.mode.TuningModeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TasksProcessorService {

    private final TuningTaskTestService taskTestService;
    private final TuningTaskService taskService;
    private final List<TuningModeService> modeServiceList;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @PostConstruct
    private void validateUniqModes() {
        int uniqCount = modeServiceList.stream()
                .map(TuningModeService::getTuningMode)
                .collect(Collectors.toSet())
                .size();
        if (uniqCount != modeServiceList.size()) {
            throw new IllegalStateException("Tuning mode services must be uniq");
        }
    }

    public void processTasks() {
        for (var modeService : modeServiceList) {
            taskTestService.getAllNonProcessedCompletedTests(modeService.getTuningMode()).stream()
                    .map(taskTest -> new TuningTaskTestId()
                            .setTaskId(taskTest.getTaskId())
                            .setTuningTestUuid(taskTest.getTuningTestUuid())
                    )
                    .forEach(
                            id -> CompletableFuture
                                    .runAsync(() -> modeService
                                            .process(id.getTaskId(), id.getTuningTestUuid()), executor)
                                    .exceptionally(getFailTaskFunction(id.getTaskId()))
                    );
        }

        log.info("Checked tasks for processing");
    }

    private Function<Throwable, Void> getFailTaskFunction(Long taskId) {
        return ex -> {
            taskService.failTask(taskId, ex);
            return null;
        };
    }
}
