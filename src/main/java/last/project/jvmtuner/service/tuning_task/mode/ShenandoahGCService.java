package last.project.jvmtuner.service.tuning_task.mode;

import last.project.jvmtuner.dto.mode.SequentialModeDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.props.ShenandoahGCProps;
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
public class ShenandoahGCService implements TuningModeService {

    private static final List<List<String>> TEST_JVM_OPTIONS = List.of(
            List.of("-XX:ShenandoahGCMode=iu", "-XX:+UnlockExperimentalVMOptions"),
            List.of("-XX:+UseNUMA"),
            List.of("-XX:+UseTransparentHugePages", "-XX:+AlwaysPreTouch")
    );
    private static final String GC_JVM_OPTION = "-XX:+UseShenandoahGC";

    private final ShenandoahGCProps shenandoahGCProps;
    private final TuningTaskTestService taskTestService;
    private final TuningTaskService taskService;
    private final SequentialModeService sequentialModeService;

    @Override
    @Transactional
    public TuningTask start(TuningTestProps testProps) {
        var task = taskService.createTask(testProps, TuningMode.SHENANDOAH_GC);

        var data = sequentialModeService.startMode(task, testProps,
                SequentialModeService.getTestOptionList(GC_JVM_OPTION, TEST_JVM_OPTIONS));
        task = taskService.updateModeData(task.getId(), SerializationUtil.serialize(data));

        log.info(String.format("Start tuning in task '%s'", task.getId()));
        return task;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void process(long taskId, UUID testUuid) {
        var taskTest = taskTestService.get(taskId, testUuid);
        var task = taskTest.getTask();
        var test = taskTest.getTest();
        var testProps = test.getTuningTestProps();

        var data = SerializationUtil.deserialize(task.getModeData(), SequentialModeDto.class);

        data = sequentialModeService.process(data, shenandoahGCProps.getRetryOnFailCount(), testProps, task, test, taskTest);
        if (SequentialModeService.isEnded(data)) {
            taskService.endTask(taskId);
        }

        taskService.updateModeData(task.getId(), SerializationUtil.serialize(data));
        taskTestService.setProcessed(taskTest);

        log.info(String.format("Test '%s' in task '%s' processed", testUuid, taskId));
    }

    @Override
    public TuningMode getTuningMode() {
        return TuningMode.SHENANDOAH_GC;
    }
}
