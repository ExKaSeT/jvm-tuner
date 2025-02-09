package last.project.jvmtuner.service.tuning_task.mode;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import last.project.jvmtuner.dto.mode.SerialGCDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.props.SerialGCProps;
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

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class SerialGCService implements TuningModeService {

    private static final List<String> STATIC_JVM_OPTIONS = List.of("-XX:+UseSerialGC");
    private static final BigInteger DIVISOR_BYTES_TO_MB = BigInteger.valueOf(1048576L);
    private static final double FAIL_TEST_MAX_OLD_NEW_RATIO = 4;

    private final K8sTestRunnerService k8sTestRunnerService;
    private final MetricService metricService;
    private final SerialGCProps serialGcProps;
    private final TuningTaskTestService taskTestService;
    private final TuningTaskService taskService;

    @Override
    @Transactional
    public void start(TuningTestProps testProps) {
        var jvmOptions = K8sDeploymentUtil.getJvmOptionList(K8sDeploymentUtil
                .deserialize(testProps.getPreparedDeployment()), testProps.getAppContainerName());
        var maxHeap = jvmOptions.stream()
                .filter(option -> option.startsWith("-Xmx"))
                .findFirst();
        if (maxHeap.isEmpty()) {
            throw new IllegalArgumentException(String.format("Heap size is not fixed in test props '%d'",
                    testProps.getId()));
        }

        var data = new SerialGCDto()
                .setHeapSize(parseOptionSizeToMB(maxHeap.get()))
                .setFoundMinWorkOldGen(false)
                .setRetryCount(0);

        var task = taskService.createTask(testProps, TuningMode.SERIAL_GC);
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
        var testProps = test.getTuningTestProps();

        if (!test.getStatus().isCompleted()) {
            throw new IllegalArgumentException(String.format("Can't process not completed test '%s' in task '%s'",
                    testUuid, taskId));
        }

        var data = SerializationUtil.deserialize(task.getModeData(), SerialGCDto.class);
        if (!data.getCurrentTest().equals(testUuid)) {
            throw new IllegalArgumentException(String.format("Test '%s' in task '%s' is not current",
                    testUuid, taskId));
        }

        var testFailed = test.getStatus().isFailed();

        // повторение неуспешного теста, если позволяют настройки
        if (testFailed && data.getRetryCount() < serialGcProps.getRetryOnFailCount()) {
            int retryNumber = data.getRetryCount() + 1;

            TuningTest retryTest;
            if (isInitialTest(data)) {
                retryTest = k8sTestRunnerService.runTest(testProps);
            } else {
                retryTest = k8sTestRunnerService.runTest(testProps,
                        getSetFixedOldGenSize(testProps.getAppContainerName(), data.getOldGenSize()));
            }
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

            int minOldGenSize;
            var startTime = test.getStartedTestTime();
            var endTime = startTime.plusSeconds(testProps.getTestDurationSec());
            var query = metricService.replaceWithTestLabels(serialGcProps.getMinOldGenSizeMbQuery(),
                    testUuid.toString(), test.getPodName(), testProps.getAppContainerName());
            try {
                var response = metricService.rangeRequest(query, startTime, endTime);
                var minValue = response.getData().getResult().get(0).getValues()
                        .stream()
                        .mapToDouble(value -> Double.parseDouble(value.getValue()))
                        .min()
                        .orElseThrow(() -> new IllegalStateException("Not found min old gen metric"));
                minOldGenSize = (int) Math.round(minValue);
            } catch (Exception ex) {
                throw new IllegalStateException(String.format("Failed fetching metric '%s' in test '%s'",
                        query, test.getUuid()), ex);
            }

            data.setOldGenSize(minOldGenSize);
        } else {
            if (data.getFoundMinWorkOldGen()) {
                if (testFailed || data.getCpuUsageAvg() < test.getTuningTestMetrics().getCpuUsageAvg()) {
                    taskService.endTask(taskId);
                    return;
                }
                data.setCpuUsageAvg(test.getTuningTestMetrics().getCpuUsageAvg());
            } else if (!testFailed) {
                data.setFoundMinWorkOldGen(true);
                data.setCpuUsageAvg(test.getTuningTestMetrics().getCpuUsageAvg());
            }
        }
        int nextTestOldGenSizeMB = data.getOldGenSize() + (data.getHeapSize() * serialGcProps.getStepPercent() / 100);

        if (nextTestOldGenSizeMB / (double) (data.getHeapSize() - nextTestOldGenSizeMB) > FAIL_TEST_MAX_OLD_NEW_RATIO) {
            throw new IllegalStateException(String.format("Failed due to Old/New ratio in task '%d'", taskId));
        }

        var nextTest = k8sTestRunnerService.runTest(testProps,
                getSetFixedOldGenSize(testProps.getAppContainerName(), nextTestOldGenSizeMB));
        var nextTaskTest = taskTestService.save(task, nextTest,
                String.format("Old gen size: %d MB", nextTestOldGenSizeMB));

        data.setCurrentTest(nextTest.getUuid());
        data.setOldGenSize(nextTestOldGenSizeMB);
        taskService.updateModeData(taskId, SerializationUtil.serialize(data));

        taskTestService.setProcessed(taskTest);

        log.info(String.format("Run test '%s' with description '%s'",
                nextTest.getUuid(), nextTaskTest.getDescription()));
    }

    @Override
    public TuningMode getTuningMode() {
        return TuningMode.SERIAL_GC;
    }

    private boolean isInitialTest(SerialGCDto data) {
        return isNull(data.getOldGenSize());
    }

    private Consumer<Deployment> getSetFixedOldGenSize(String containerName, int oldGenSizeMB) {
        return K8sDeploymentUtil.addJvmOptions(STATIC_JVM_OPTIONS, containerName, options -> {
            options.removeIf(option -> option.startsWith("-XX:New") || option.startsWith("-Xmn"));
            options.add(String.format("-Xmn%dm", oldGenSizeMB));
        });
    }

    private int parseOptionSizeToMB(String option) {
        var lastCharIndex = option.length() - 1;
        Integer numberIndex = null;
        for (int i = lastCharIndex - 1; i >= 0; i--) {
            if (Character.isDigit(option.charAt(i))) {
                numberIndex = i;
            } else {
                break;
            }
        }
        if (isNull(numberIndex)) {
            throw new IllegalArgumentException(String.format("Can't parse size in '%s'", option));
        }

        var unit = Character.toLowerCase(option.charAt(lastCharIndex));
        var size = option.substring(numberIndex, lastCharIndex);
        return switch (unit) {
            case 'm' -> Integer.parseInt(size);
            case 'g' -> Integer.parseInt(size) * 1024;
            case 'b' -> new BigInteger(size)
                    .divide(DIVISOR_BYTES_TO_MB)
                    .intValue();
            default -> throw new IllegalArgumentException(String.format("Can't parse unit in '%s'", option));
        };
    }
}
