package last.project.jvmtuner.service.tuning_task.mode;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import last.project.jvmtuner.dto.mode.MaxHeapSizeDto;
import last.project.jvmtuner.model.tuning_task.*;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestMetricsService;
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
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaxHeapSizeService implements TuningModeService {

    private static final List<String> STATIC_JVM_OPTIONS = List.of("-XX:+AlwaysPreTouch");

    private final K8sTestRunnerService k8sTestRunnerService;
    private final MetricService metricService;
    private final MaxHeapSizeProps maxHeapSizeProps;
    private final TuningTaskTestService taskTestService;
    private final TuningTaskService taskService;
    private final TuningTestMetricsService testMetricsService;

    @Override
    @Transactional
    public void start(TuningTestProps testProps) {
        var memoryLimitMB = K8sDeploymentUtil.getAppMemoryLimitsMB(testProps);
        var data = new MaxHeapSizeDto()
                .setRetryCount(0)
                .setContainerLimitMB(memoryLimitMB)
                .setStepSizeMB((memoryLimitMB / 100) * maxHeapSizeProps.getStartStepPercent());

        var task = taskService.createTask(testProps, TuningMode.MAX_HEAP_SIZE);
        var test = runInitialTest(testProps);
        taskTestService.save(task, test, "Initial test with unmodified deployment");

        data.setCurrentTest(new MaxHeapSizeDto.Test().setUuid(test.getUuid()));
        data.setPrevTest(new MaxHeapSizeDto.Test());
        taskService.updateModeData(task.getId(), SerializationUtil.serialize(data));

        log.info(String.format("Start initial test '%s' in task '%s'", test.getUuid(), task.getId()));
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

        var data = SerializationUtil.deserialize(task.getModeData(), MaxHeapSizeDto.class);
        if (!data.getCurrentTest().getUuid().equals(testUuid)) {
            throw new IllegalArgumentException(String.format("Test '%s' in task '%s' is not current",
                    testUuid, taskId));
        }

        var testFailed = test.getStatus().isFailed();

        // повторение неуспешного теста, если позволяют настройки
        if (testFailed && data.getRetryCount() < maxHeapSizeProps.getRetryOnFailCount()) {
            int retryNumber = data.getRetryCount() + 1;

            TuningTest retryTest;
            if (isInitialTest(data)) {
                retryTest = runInitialTest(testProps);
            } else {
                retryTest = k8sTestRunnerService.runTest(testProps,
                        setFixedHeapSize(testProps.getAppContainerName(), data.getCurrentTest().getHeapSizeMB()));
            }
            var retryTaskTest = taskTestService.save(task, retryTest,
                    taskTestService.addRetryPrefix(taskTest.getDescription(), retryNumber));

            data.setRetryCount(retryNumber);
            data.getCurrentTest().setUuid(retryTest.getUuid());
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

            int heapSize;
            var startTime = test.getStartedTestTime();
            var endTime = startTime.plusSeconds(testProps.getTestDurationSec());
            var query = metricService.replaceWithTestLabels(maxHeapSizeProps.getHeapSizeMbQuery(),
                    testUuid.toString(), test.getPodName(), testProps.getAppContainerName());
            try {
                var response = metricService.rangeRequest(query, startTime, endTime);
                var maxValue = response.getData().getResult().get(0).getValues()
                        .stream()
                        .mapToDouble(value -> Double.parseDouble(value.getValue()))
                        .max()
                        .orElseThrow(() -> new IllegalStateException("Not found min heap size metric"));
                heapSize = (int) Math.round(maxValue);
            } catch (Exception ex) {
                throw new IllegalStateException(String.format("Failed fetching metric '%s' in test '%s'",
                        query, test.getUuid()), ex);
            }

            data.getCurrentTest().setHeapSizeMB(heapSize);
        }

        int nextTestHeapSizeMB;
        if (testFailed) {



            throw new NotImplementedException();
        } else {
            // Если нагрузка на CPU увеличилась, то меняем направление и уменьшаем шаг
            if (testMetricsService.compareByCpuUsage(testUuid, data.getPrevTest().getUuid()) < 0) {
                if (data.isIncreaseDirection()) {
                    data.setMaxHeapBound(data.getCurrentTest().getHeapSizeMB());
                } else {
                    data.setMinHeapBound(data.getCurrentTest().getHeapSizeMB());
                }
                data.setIncreaseDirection(!data.isIncreaseDirection());
                data.setStepSizeMB((int) (data.getStepSizeMB() / maxHeapSizeProps.getStepDivider()));
            }

            if (data.isIncreaseDirection()) {
                nextTestHeapSizeMB = data.getCurrentTest().getHeapSizeMB() + data.getStepSizeMB();
            } else {
                nextTestHeapSizeMB = data.getCurrentTest().getHeapSizeMB() - data.getStepSizeMB();
            }
        }

        // TODO: check END test: stepSize < (containerLimits / 100) * endStepPercent
        // TODO: if nextTestHeapSizeMB > containerLimits

        var nextTest = k8sTestRunnerService.runTest(testProps,
                setFixedHeapSize(testProps.getAppContainerName(), nextTestHeapSizeMB));
        var nextTaskTest = taskTestService.save(task, nextTest,
                String.format("Fixed heap size: %d MB", nextTestHeapSizeMB));

        data.setPrevTest(data.getCurrentTest());
        data.setCurrentTest(new MaxHeapSizeDto.Test()
                .setHeapSizeMB(nextTestHeapSizeMB)
                .setUuid(nextTest.getUuid())
        );
        taskService.updateModeData(taskId, SerializationUtil.serialize(data));

        taskTestService.setProcessed(taskTest);

        log.info(String.format("Run test '%s' with description '%s'",
                nextTest.getUuid(), nextTaskTest.getDescription()));
    }

    @Override
    public TuningMode getTuningMode() {
        return TuningMode.MAX_HEAP_SIZE;
    }

    private boolean isInitialTest(MaxHeapSizeDto data) {
        return data.isInitialTest();
    }

    private boolean is

    private TuningTest runInitialTest(TuningTestProps props) {
        return k8sTestRunnerService.runTest(props, K8sDeploymentUtil
                .addJvmOptions(STATIC_JVM_OPTIONS, props.getAppContainerName()));
    }

    private Consumer<Deployment> setFixedHeapSize(String containerName, int heapSizeMB) {
        return K8sDeploymentUtil.addJvmOptions(STATIC_JVM_OPTIONS, containerName, options -> {
            options.removeIf(option -> option.startsWith("-Xmx") || option.startsWith("-Xms"));
            options.add(String.format("-Xmx%dm", heapSizeMB));
            options.add(String.format("-Xms%dm", heapSizeMB));
        });
    }
}
