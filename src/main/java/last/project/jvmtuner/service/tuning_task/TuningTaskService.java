package last.project.jvmtuner.service.tuning_task;

import last.project.jvmtuner.dao.tuning_task.TuningTaskRepository;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_task.TuningTaskStatus;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class TuningTaskService {

    private final TuningTaskRepository tuningTaskRepository;
    private final TuningTaskTestService taskTestService;

    public TuningTask createTask(TuningTestProps props, TuningMode mode) {
        var task = new TuningTask()
                .setMode(mode)
                .setStatus(TuningTaskStatus.RUNNING)
                .setCreatedTime(Instant.now())
                .setTuningTestProps(props);
        return tuningTaskRepository.save(task);
    }

    public TuningTask updateModeData(Long taskId, String modeData) {
        var task = tuningTaskRepository.findById(taskId).get();
        task.setModeData(modeData);
        return tuningTaskRepository.save(task);
    }

    @Transactional
    public void failTask(Long taskId, Throwable ex) {
        var task = tuningTaskRepository.findById(taskId).get();
        task.setStatus(TuningTaskStatus.FAILED);
        tuningTaskRepository.save(task);

        task.getTaskTests().stream()
                .filter(taskTest -> !taskTest.getProcessed())
                .forEach(taskTestService::setProcessed);

        log.error(String.format("Task '%d' failed", taskId), ex);
    }

    public void endTask(Long taskId) {
        var task = tuningTaskRepository.findById(taskId).get();
        task.setStatus(TuningTaskStatus.COMPLETED);
        tuningTaskRepository.save(task);

        task.getTaskTests().stream()
                .filter(taskTest -> !taskTest.getProcessed())
                .forEach(taskTestService::setProcessed);

        log.info(String.format("Task '%d' completed", taskId));
    }
}
