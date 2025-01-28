package last.project.jvmtuner.service.mode;

import last.project.jvmtuner.dao.tuning_task.TuningTaskRepository;
import last.project.jvmtuner.dao.tuning_task.TuningTaskTestRepository;
import last.project.jvmtuner.dto.mode.max_heap_size.MaxHeapSizeDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_task.TuningTaskStatus;
import last.project.jvmtuner.model.tuning_task.TuningTaskTest;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.props.MaxHeapSizeProps;
import last.project.jvmtuner.service.MetricService;
import last.project.jvmtuner.service.tuning_test.K8sTestRunnerService;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import last.project.jvmtuner.util.SerializationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaxHeapSizeService implements TuningModeService {

    private final K8sTestRunnerService k8sTestRunnerService;
    private final MetricService metricService;
    private final MaxHeapSizeProps maxHeapSizeProps;
    private final TuningTaskRepository taskRepository;
    private final TuningTaskTestRepository taskTestRepository;

    @Override
    @Transactional
    public void start(TuningTestProps testProps) {
        var data = new MaxHeapSizeDto()
                .setRetryCount(maxHeapSizeProps.getRetryOnFailCount())
                .setMaxHeapSize(K8sDeploymentUtil.getAppMemoryLimitsMB(testProps));

        var task = new TuningTask()
                .setMode(TuningMode.MAX_HEAP_SIZE)
                .setStatus(TuningTaskStatus.RUNNING)
                .setCreatedTime(Instant.now())
                .setTuningTestProps(testProps);
        task = taskRepository.save(task);

        var test = k8sTestRunnerService.runTest(testProps);

        var taskTest = new TuningTaskTest()
                .setTask(task)
                .setTest(test)
                .setDescription("Initial test with unmodified deployment");
        taskTestRepository.save(taskTest);

        data.setCurrentTest(test.getUuid());
        task.setModeData(SerializationUtil.serialize(data));
        task = taskRepository.save(task);

        log.info(String.format("Start initial test '%s' in task '%s'", test.getUuid(), task.getId()));
    }

    @Override
    public void check() {
//        if (!started) {
//            throw new IllegalStateException("Not started");
//        }
//
//        // init test
//        if (processedTests.isEmpty()) {
//
//        }

    }
}
