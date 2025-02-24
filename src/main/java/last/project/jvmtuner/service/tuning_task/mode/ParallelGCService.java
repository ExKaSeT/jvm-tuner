package last.project.jvmtuner.service.tuning_task.mode;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import last.project.jvmtuner.dto.mode.ParallelGCDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.props.ParallelGCProps;
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

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParallelGCService implements TuningModeService {

    private static final List<String> STATIC_JVM_OPTIONS = List.of("-XX:+UseParallelGC");

    private final K8sTestRunnerService k8sTestRunnerService;
    private final ParallelGCProps parallelGCProps;
    private final TuningTaskTestService taskTestService;
    private final TuningTaskService taskService;

    @Override
    @Transactional
    public TuningTask start(TuningTestProps testProps) {
        var data = new ParallelGCDto()
                .setRetryCount(0)
                .setThreads(parallelGCProps.getInitialThreads());

        var task = taskService.createTask(testProps, TuningMode.PARALLEL_GC);
        var test = k8sTestRunnerService.runTest(testProps,
                getSetParallelGCThreads(testProps.getAppContainerName(), data.getThreads()));
        taskTestService.save(task, test, String.format("Initial test with %d threads", data.getThreads()));

        data.setCurrentTest(test.getUuid());
        task = taskService.updateModeData(task.getId(), SerializationUtil.serialize(data));

        log.info(String.format("Start initial test '%s' in task '%s'", test.getUuid(), task.getId()));
        return task;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void process(long taskId, UUID testUuid) {
        var taskTest = taskTestService.get(taskId, testUuid);
        var task = taskTest.getTask();
        var test = taskTest.getTest();
        var testProps = test.getTuningTestProps();

        if (!test.getStatus().isCompleted()) {
            throw new IllegalArgumentException(String.format("Can't process not completed test '%s' in task '%s'",
                    testUuid, taskId));
        }

        var data = SerializationUtil.deserialize(task.getModeData(), ParallelGCDto.class);
        if (!data.getCurrentTest().equals(testUuid)) {
            throw new IllegalArgumentException(String.format("Test '%s' in task '%s' is not current",
                    testUuid, taskId));
        }

        var testFailed = test.getStatus().isFailed();

        // повторение неуспешного теста, если позволяют настройки
        if (testFailed && data.getRetryCount() < parallelGCProps.getRetryOnFailCount()) {
            int retryNumber = data.getRetryCount() + 1;

            var retryTest = k8sTestRunnerService.runTest(testProps,
                    getSetParallelGCThreads(testProps.getAppContainerName(), data.getThreads()));

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

        data.setRetryCount(0);

        if (isInitialTest(data)) {
            if (testFailed) {
                throw new IllegalStateException(String.format("Initial test '%s' in task '%s' failed",
                        testUuid, taskId));
            }
        } else {
            if (testFailed || data.getCpuUsageAvg() < test.getTuningTestMetrics().getCpuUsageAvg()) {
                taskService.endTask(taskId);
                return;
            }
        }
        data.setCpuUsageAvg(test.getTuningTestMetrics().getCpuUsageAvg());

        int nextTestThreads = data.getThreads() + parallelGCProps.getStep();
        if (nextTestThreads > parallelGCProps.getMaxThreads()) {
            taskService.endTask(taskId);
            return;
        }

        var nextTest = k8sTestRunnerService.runTest(testProps,
                getSetParallelGCThreads(testProps.getAppContainerName(), nextTestThreads));
        var nextTaskTest = taskTestService.save(task, nextTest,
                String.format("Threads count: %d", nextTestThreads));

        data.setCurrentTest(nextTest.getUuid());
        data.setThreads(nextTestThreads);
        taskService.updateModeData(taskId, SerializationUtil.serialize(data));

        taskTestService.setProcessed(taskTest);

        log.info(String.format("Run test '%s' with description '%s'",
                nextTest.getUuid(), nextTaskTest.getDescription()));
    }

    @Override
    public TuningMode getTuningMode() {
        return TuningMode.PARALLEL_GC;
    }

    private boolean isInitialTest(ParallelGCDto data) {
        return isNull(data.getCpuUsageAvg());
    }

    private Consumer<Deployment> getSetParallelGCThreads(String containerName, int threads) {
        return K8sDeploymentUtil.addJvmOptions(STATIC_JVM_OPTIONS, containerName, options -> {
            options.removeIf(option -> option.startsWith("-XX:ParallelGCThreads="));
            options.add(String.format("-XX:ParallelGCThreads=%d", threads));
        });
    }
}
