package last.project.jvmtuner.service.tuning_task;

import last.project.jvmtuner.dao.tuning_task.TuningTaskTestRepository;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_task.TuningTaskTest;
import last.project.jvmtuner.model.tuning_task.TuningTaskTestId;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TuningTaskTestService {

    private final TuningTaskTestRepository taskTestRepository;

    public TuningTaskTest save(TuningTask task, TuningTest test, String description) {
        var taskTest = new TuningTaskTest()
                .setTask(task)
                .setTaskId(task.getId())
                .setTest(test)
                .setTuningTestUuid(test.getUuid())
                .setDescription(description);
        return taskTestRepository.save(taskTest);
    }

    public TuningTaskTest get(long taskId, UUID testUuid) {
        return taskTestRepository.findById(
                new TuningTaskTestId()
                        .setTaskId(taskId)
                        .setTuningTestUuid(testUuid)
        ).get();
    }

    public TuningTaskTest setProcessed(TuningTaskTest taskTest) {
        taskTest.setProcessed(true);
        return taskTestRepository.save(taskTest);
    }

    public List<TuningTaskTest> getAllNonProcessedCompletedTests(TuningMode mode) {
        return taskTestRepository.getAllByProcessedAndTaskMode(false, mode).stream()
                .filter(taskTest -> taskTest.getTest().getStatus().isCompleted())
                .collect(Collectors.toList());
    }

    public String addRetryPrefix(String description, int retryNumber) {
        return String.format("Retry %d: %s", retryNumber, description);
    }
}
