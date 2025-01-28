package last.project.jvmtuner.config;

import last.project.jvmtuner.service.tuning_test.TestsProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SchedulingConfig {

    private final TestsProcessorService testStatusCheckerService;

    @Scheduled(cron = "${scheduler.process-tests-cron}")
    public void testStatusCheckerService() {
        testStatusCheckerService.processTests();
    }
}
