package last.project.jvmtuner.service.tuning_task;

import last.project.jvmtuner.dao.tuning_task.TuningTaskRepository;
import last.project.jvmtuner.dto.tuning_task.TuningTaskDetailsResponseDto;
import last.project.jvmtuner.dto.tuning_task.TuningTaskResponseDto;
import last.project.jvmtuner.dto.tuning_test.TuningTestResponseDto;
import last.project.jvmtuner.model.tuning_task.TuningMode;
import last.project.jvmtuner.model.tuning_task.TuningTask;
import last.project.jvmtuner.model.tuning_task.TuningTaskStatus;
import last.project.jvmtuner.model.tuning_task.TuningTaskTest;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;
import last.project.jvmtuner.service.tuning_test.TuningTestService;
import last.project.jvmtuner.util.SerializationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class TuningTaskService {

    private final TuningTaskRepository taskRepository;
    private final TuningTaskTestService taskTestService;
    private final TuningTestService testService;
    private final TuningTestPropsService testPropsService;
    private final ApplicationContext appContext;

    @Transactional
    public TuningTask startTask(long propsId, TuningMode mode) {
        var service = appContext.getBean(TuningMode.getServiceClass(mode));
        var props = testPropsService.get(propsId);
        return service.start(props);
    }

    public TuningTask createTask(TuningTestProps props, TuningMode mode) {
        var task = new TuningTask()
                .setMode(mode)
                .setStatus(TuningTaskStatus.RUNNING)
                .setCreatedTime(Instant.now())
                .setTuningTestProps(props);
        return taskRepository.save(task);
    }

    public TuningTask updateModeData(Long taskId, String modeData) {
        var task = taskRepository.findById(taskId).get();
        task.setModeData(modeData);
        return taskRepository.save(task);
    }

    @Transactional
    public void failTask(Long taskId, Throwable ex) {
        var task = taskRepository.findById(taskId).get();
        task.setStatus(TuningTaskStatus.FAILED);
        taskRepository.save(task);

        task.getTaskTests().stream()
                .filter(taskTest -> !taskTest.getProcessed())
                .forEach(taskTestService::setProcessed);

        log.error(String.format("Task '%d' failed", taskId), ex);
    }

    public void endTask(Long taskId) {
        var task = taskRepository.findById(taskId).get();
        task.setStatus(TuningTaskStatus.COMPLETED);
        task.setCompletedTime(Instant.now());
        taskRepository.save(task);

        task.getTaskTests().stream()
                .filter(taskTest -> !taskTest.getProcessed())
                .forEach(taskTestService::setProcessed);

        log.info(String.format("Task '%d' completed", taskId));
    }

    public List<TuningTaskResponseDto> getAllTasks() {
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(task -> new TuningTaskResponseDto()
                        .setId(task.getId())
                        .setMode(task.getMode())
                        .setStatus(task.getStatus())
                        .setCreatedTime(task.getCreatedTime())
                        .setCompletedTime(task.getCompletedTime())
                ).toList();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long taskId) {
        var task = taskRepository.findById(taskId).get();
        var tests = task.getTaskTests().stream().map(TuningTaskTest::getTuningTestUuid).toList();
        taskRepository.deleteById(taskId);
        tests.forEach(testService::delete);
    }

    @Transactional
    public TuningTaskDetailsResponseDto getDetails(long taskId) {
        var task = taskRepository.findById(taskId).get();
        var props = task.getTuningTestProps();
        var response = new TuningTaskDetailsResponseDto()
                .setTaskDto(new TuningTaskResponseDto()
                        .setId(task.getId())
                        .setMode(task.getMode())
                        .setStatus(task.getStatus())
                        .setCreatedTime(task.getCreatedTime())
                        .setCompletedTime(task.getCompletedTime())
                        .setPropsId(task.getTuningTestProps().getId())
                );
        var testDtoList = new ArrayList<TuningTestResponseDto>();
        for (var taskTest : task.getTaskTests()) {
            var test = taskTest.getTest();
            var testMetrics = test.getTuningTestMetrics();
            TuningTestResponseDto.TestMetricsDto testMetricsDto = null;
            if (nonNull(testMetrics)) {
                testMetricsDto = new TuningTestResponseDto.TestMetricsDto()
                        .setCpuUsageAvg(testMetrics.getCpuUsageAvg())
                        .setCpuThrottlingAvg(testMetrics.getCpuThrottlingAvg())
                        .setMemoryRssAvg(testMetrics.getMemoryRssAvg())
                        .setMemoryWssAvg(testMetrics.getMemoryWssAvg())
                        .setMemoryUsageAvg(testMetrics.getMemoryUsageAvg());
            }
            var testDto = new TuningTestResponseDto()
                    .setUuid(test.getUuid())
                    .setStatus(test.getStatus())
                    .setDescription(taskTest.getDescription())
                    .setPodName(test.getPodName())
                    .setDeployedTime(test.getDeployedTime())
                    .setStartedTestTime(test.getStartedTestTime())
                    .setDeployment(SerializationUtil.beautifyJSON(test.getDeployment()))
                    .setTestMetricsDto(testMetricsDto)
                    .setGrafanaUrl(testService.getGrafanaUrl(test, props));
            testDtoList.add(testDto);
        }
        testDtoList.sort(Comparator.comparingLong(testDto -> testDto.getDeployedTime().toEpochMilli()));
        response.setTestDto(testDtoList);
        return response;
    }
}
