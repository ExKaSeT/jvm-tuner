package last.project.jvmtuner.service;

import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.dao.tuning_test.TuningTestMetricsRepository;
import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.dto.tuning_test.MetricMaxValueDto;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import last.project.jvmtuner.service.tuning_test.EndTestProcessService;
import last.project.jvmtuner.service.tuning_test.K8sTestRunnerService;
import last.project.jvmtuner.service.tuning_test.K8sTestStatusCheckerService;
import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@AppTest
@Slf4j
@Transactional
@Rollback(false)
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor
class K8sTestRunAndStatusCheckTest {

    private final K8sTestRunnerService k8sTestRunnerService;
    private final TuningTestPropsService tuningTestPropsService;
    private final K8sTestStatusCheckerService k8sTestStatusCheckerService;
    private final TuningTestRepository tuningTestRepository;
    private final TuningTestMetricsRepository tuningTestMetricsRepository;
    private final EndTestProcessService endTestProcessService;

    private TuningTest test;

    @Test
    @Order(0)
    void runTestTest() {
        String deployment = """
                kind: Deployment
                apiVersion: apps/v1
                metadata:
                  name: jvm-tuner
                  namespace: default
                  labels:
                    app: jvm-tuner
                    group: jvm-tuner
                spec:
                  replicas: 1
                  selector:
                    matchLabels:
                      app: jvm-tuner
                  template:
                    metadata:
                      creationTimestamp: null
                      labels:
                        app: jvm-tuner
                    spec:
                      containers:
                        - name: crypto
                          image: exkaset/crypto
                          env:
                            - name: JAVA_TOOL_OPTIONS
                              value: '-XX:+PrintCommandLineFlags'
                          envFrom:
                            - configMapRef:
                                name: crypto-config
                          ports:
                            - containerPort: 8080
                              protocol: TCP
                          resources:
                            limits:
                              cpu: 800m
                              memory: 800Mi
                            requests:
                              cpu: 800m
                              memory: 800Mi
                          livenessProbe:
                            httpGet:
                              path: /actuator/health/liveness
                              port: 8080
                              scheme: HTTP
                            initialDelaySeconds: 60
                            timeoutSeconds: 1
                            periodSeconds: 10
                            successThreshold: 1
                            failureThreshold: 3
                          readinessProbe:
                            httpGet:
                              path: /actuator/health/readiness
                              port: 8080
                              scheme: HTTP
                            initialDelaySeconds: 30
                            timeoutSeconds: 1
                            periodSeconds: 5
                            successThreshold: 1
                            failureThreshold: 3
                        - name: curl
                          image: alpine/curl
                          command: ["/bin/sh", "-c", "tail -f /dev/null"]""";

        var props = tuningTestPropsService.saveTuningTestProps(deployment, "crypto",
                "8080/actuator/prometheus", "exkaset/gatling:1.0",
                "bash -c \"mvn gatling:test > /dev/null 2> /dev/null &\"",
                60, 60,
                List.of(new MetricMaxValueDto()
                        .setQuery("sum(gatling_count_total{type=\"ko\", $jvm_tuner_id}) / sum(gatling_count_total{type=\"ok\", $jvm_tuner_id}) * 100")
                        .setMaxValue(10)));

        this.test = k8sTestRunnerService.runTest(props, K8sDeploymentUtil
                .addJvmOptions(List.of("-XX:+PrintCommandLineFlags", "-Xmx400M"), "crypto"));
        log.info("UUID of test: " + this.test.getUuid());
    }

    @Test
    @Order(1)
    void notReadyStatusCheckTest() throws InterruptedException {
        k8sTestStatusCheckerService.checkNotReadyTest(this.test);
        Thread.sleep(50_000);
        k8sTestStatusCheckerService.checkNotReadyTest(this.test);

        this.test = tuningTestRepository.getById(this.test.getUuid());

        assertEquals(TuningTestStatus.RUNNING, this.test.getStatus());
    }

    @Test
    @Order(2)
    void runningStatusCheckTest() throws InterruptedException {
        var testDurationSec = test.getTuningTestProps().getTestDurationSec();

        k8sTestStatusCheckerService.checkRunningTest(this.test);
        Thread.sleep(testDurationSec / 2 * 1000L);
        k8sTestStatusCheckerService.checkRunningTest(this.test);
        Thread.sleep(testDurationSec / 2 * 1000L);
        k8sTestStatusCheckerService.checkRunningTest(this.test);

        this.test = tuningTestRepository.getById(this.test.getUuid());

        assertEquals(TuningTestStatus.ENDED, this.test.getStatus());
    }

    @Test
    @Order(3)
    void endedStatusCheckTest() {
        endTestProcessService.processEndTest(this.test);

        this.test = tuningTestRepository.getById(this.test.getUuid());
        var metrics = tuningTestMetricsRepository.getById(this.test.getUuid());

        assertEquals(TuningTestStatus.PROCESSED, this.test.getStatus());
        assertNotNull(metrics.getCpuUsageAvg());
        assertNotNull(metrics.getCpuThrottlingAvg());
        assertNotNull(metrics.getMemoryUsageAvg());
        assertNotNull(metrics.getMemoryWssAvg());
        assertNotNull(metrics.getMemoryRssAvg());
    }
}
