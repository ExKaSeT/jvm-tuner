package last.project.jvmtuner.service.tuning_task.mode;

import last.project.jvmtuner.dto.mode.SequentialModeDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.props.G1GCProps;
import last.project.jvmtuner.service.tuning_task.TuningTaskService;
import last.project.jvmtuner.service.tuning_task.TuningTaskTestService;
import last.project.jvmtuner.service.tuning_task.mode.common.SequentialModeService;
import last.project.jvmtuner.util.SerializationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class G1GCService implements TuningModeService {

    private static final List<String> TEST_JVM_OPTIONS = List.of("-XX:+UseStringDeduplication");
    private static final String GC_JVM_OPTION = "-XX:+UseG1GC";

    private final G1GCProps g1GCProps;
    private final TuningTaskTestService taskTestService;
    private final TuningTaskService taskService;
    private final SequentialModeService sequentialModeService;

    @Override
    @Transactional
    public void start(TuningTestProps testProps) {
        var task = taskService.createTask(testProps, TuningMode.G1_GC);

        var data = sequentialModeService.startMode(task, testProps,
                SequentialModeService.getTestOptionList(GC_JVM_OPTION, TEST_JVM_OPTIONS));
        taskService.updateModeData(task.getId(), SerializationUtil.serialize(data));

        log.info(String.format("Start tuning in task '%s'", task.getId()));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void process(long taskId, UUID testUuid) {
        var taskTest = taskTestService.get(taskId, testUuid);
        var task = taskTest.getTask();
        var test = taskTest.getTest();
        var testProps = test.getTuningTestProps();

        var data = SerializationUtil.deserialize(task.getModeData(), SequentialModeDto.class);

        data = sequentialModeService.process(data, g1GCProps.getRetryOnFailCount(), testProps, task, test, taskTest);
        if (SequentialModeService.isEnded(data)) {
            taskService.endTask(taskId);
        }

        taskService.updateModeData(task.getId(), SerializationUtil.serialize(data));
        taskTestService.setProcessed(taskTest);

        log.info(String.format("Test '%s' in task '%s' processed", testUuid, taskId));
    }

    @Override
    public TuningMode getTuningMode() {
        return TuningMode.G1_GC;
    }
}
