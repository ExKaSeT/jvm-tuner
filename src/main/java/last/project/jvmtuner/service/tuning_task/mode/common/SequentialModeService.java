package last.project.jvmtuner.service.tuning_task.mode.common;

import last.project.jvmtuner.dto.mode.SequentialModeDto;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_task.TuningTaskTest;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.service.tuning_task.TuningTaskTestService;
import last.project.jvmtuner.service.tuning_test.K8sTestRunnerService;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SequentialModeService {

    private final K8sTestRunnerService k8sTestRunnerService;
    private final TuningTaskTestService taskTestService;

    @Transactional
    public SequentialModeDto startMode(TuningTask task, TuningTestProps testProps, List<List<String>> testOptionsList) {
        var modeDto = new SequentialModeDto()
                .setRetryCount(0)
                .setTestOptionsList(testOptionsList)
                .setCurrentOptionsIndex(0);
        var currentOptions = modeDto.getTestOptionsList().get(0);
        var test = k8sTestRunnerService.runTest(testProps,
                K8sDeploymentUtil.addJvmOptions(currentOptions, testProps.getAppContainerName()));
        taskTestService.save(task, test,
                this.getDescription(currentOptions));
        modeDto.setCurrentTest(test.getUuid());

        log.info(String.format("Start sequential mode with test '%s' in task '%s'", test.getUuid(), task.getId()));
        return modeDto;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SequentialModeDto process(SequentialModeDto modeDto, Integer maxRetryCount, TuningTestProps testProps,
                                     TuningTask task, TuningTest test, TuningTaskTest taskTest) {
        if (isEnded(modeDto)) {
            throw new IllegalStateException("Mode already completed");
        }
        if (!test.getStatus().isCompleted()) {
            throw new IllegalArgumentException(String.format("Can't process not completed test '%s' in task '%s'",
                    test.getUuid(), task.getId()));
        }
        if (!modeDto.getCurrentTest().equals(test.getUuid())) {
            throw new IllegalArgumentException(String.format("Test '%s' in task '%s' is not current",
                    test.getUuid(), task.getId()));
        }

        var testFailed = test.getStatus().isFailed();

        // повторение неуспешного теста, если позволяют настройки
        if (testFailed && modeDto.getRetryCount() < maxRetryCount) {
            int retryNumber = modeDto.getRetryCount() + 1;
            var currentOptions = modeDto.getTestOptionsList().get(modeDto.getCurrentOptionsIndex());

            var retryTest = k8sTestRunnerService.runTest(testProps,
                    K8sDeploymentUtil.addJvmOptions(currentOptions, testProps.getAppContainerName()));

            var retryTaskTest = taskTestService.save(task, retryTest,
                    taskTestService.addRetryPrefix(taskTest.getDescription(), retryNumber));

            modeDto.setRetryCount(retryNumber);
            modeDto.setCurrentTest(retryTest.getUuid());

            log.info(String.format("Run retry test '%s' with description '%s'",
                    retryTest.getUuid(), retryTaskTest.getDescription()));
            return modeDto;
        }

        modeDto.setCurrentOptionsIndex(modeDto.getCurrentOptionsIndex() + 1);
        modeDto.setRetryCount(0);
        if (isEnded(modeDto)) {
            return modeDto;
        }

        var currentOptions = modeDto.getTestOptionsList().get(modeDto.getCurrentOptionsIndex());
        var nextTest = k8sTestRunnerService.runTest(testProps,
                K8sDeploymentUtil.addJvmOptions(currentOptions, testProps.getAppContainerName()));
        var nextTaskTest = taskTestService.save(task, nextTest, this.getDescription(currentOptions));

        modeDto.setCurrentTest(nextTest.getUuid());
        taskTestService.setProcessed(taskTest);

        log.info(String.format("Run test '%s' with description '%s'",
                nextTest.getUuid(), nextTaskTest.getDescription()));

        return modeDto;
    }

    private String getDescription(List<String> options) {
        if (options.isEmpty()) {
            return "Test with unmodified deployment";
        }
        return "Test with options: " + String.join(", ", options);
    }

    public static boolean isEnded(SequentialModeDto modeDto) {
        return modeDto.getCurrentOptionsIndex() >= modeDto.getTestOptionsList().size();
    }

    public static List<List<String>> getTestOptionList(String gcOption, List<List<String>> testOptions) {
        var result = new ArrayList<List<String>>(testOptions.size() + 1);
        result.add(List.of(gcOption));
        for (var options : testOptions) {
            var newOptions = new ArrayList<String>(options.size() + 1);
            newOptions.add(gcOption);
            newOptions.addAll(options);
            result.add(newOptions);
        }
        return result;
    }
}
