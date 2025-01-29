package last.project.jvmtuner.service.tuning_task.mode;

import last.project.jvmtuner.dto.mode.max_heap_size.MaxHeapSizeDto;
import last.project.jvmtuner.model.tuning_task.*;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.props.MaxHeapSizeProps;
import last.project.jvmtuner.service.MetricService;
import last.project.jvmtuner.service.tuning_task.TuningTaskService;
import last.project.jvmtuner.service.tuning_task.TuningTaskTestService;
import last.project.jvmtuner.service.tuning_test.K8sTestRunnerService;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import last.project.jvmtuner.util.SerializationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaxHeapSizeService implements TuningModeService {

    private final K8sTestRunnerService k8sTestRunnerService;
    private final MetricService metricService;
    private final MaxHeapSizeProps maxHeapSizeProps;
    private final TuningTaskTestService taskTestService;
    private final TuningTaskService taskService;

    @Override
    @Transactional
    public void start(TuningTestProps testProps) {
        var data = new MaxHeapSizeDto()
                .setRetryCount(0)
                .setMaxHeapSize(K8sDeploymentUtil.getAppMemoryLimitsMB(testProps));

        var task = taskService.createTask(testProps, TuningMode.MAX_HEAP_SIZE);
        var test = k8sTestRunnerService.runTest(testProps);
        taskTestService.save(task, test, "Initial test with unmodified deployment");

        data.setCurrentTest(test.getUuid());
        taskService.updateModeData(task.getId(), SerializationUtil.serialize(data));

        log.info(String.format("Start initial test '%s' in task '%s'", test.getUuid(), task.getId()));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void process(long taskId, UUID testUuid) {
        var taskTest = taskTestService.get(taskId, testUuid);
        var task = taskTest.getTask();
        var test = taskTest.getTest();

        if (!test.getStatus().isCompleted()) {
            throw new IllegalArgumentException(String.format("Can't process not completed test '%s' in task '%s'",
                    testUuid, taskId));
        }

        var data = SerializationUtil.deserialize(task.getModeData(), MaxHeapSizeDto.class);
        if (!data.getCurrentTest().equals(testUuid)) {
            throw new IllegalArgumentException(String.format("Test '%s' in task '%s' is not current",
                    testUuid, taskId));
        }

        var testFailed = test.getStatus().isFailed();

        // повторение неуспешного теста, если позволяют настройки
        if (testFailed && data.getRetryCount() < maxHeapSizeProps.getRetryOnFailCount()) {
            int retryNumber = data.getRetryCount() + 1;

            var retryTest = k8sTestRunnerService.runTest(task.getTuningTestProps());
            var retryTaskTest = taskTestService.save(task, retryTest,
                    taskTestService.addRetryPrefix(taskTest.getDescription(), retryNumber));

            data.setRetryCount(retryNumber);
            data.setCurrentTest(retryTest.getUuid());
            taskService.updateModeData(taskId, SerializationUtil.serialize(data));

            taskTestService.setProcessed(taskTest);

            log.info(String.format("Run retry test '%s' with description '%s'",
                    retryTest.getUuid(), retryTaskTest.getDescription()));
            return;
        }

        // TODO:
        throw new RuntimeException("Не реализовано");

        // не определен минимальный HeapSize - был начальный тест
//        if (isNull(data.getMinHeapSize())) {
//
//        }
    }
}
