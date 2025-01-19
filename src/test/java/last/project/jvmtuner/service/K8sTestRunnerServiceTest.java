package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import last.project.jvmtuner.annotation.AppTest;
import last.project.jvmtuner.dto.tuning_test.MetricMaxValueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

@AppTest
@Slf4j
@RequiredArgsConstructor
class K8sTestRunnerServiceTest {

    private final K8sTestRunnerService k8sTestRunnerService;

    @Test
    void runTestTest() {
        Deployment deployment = new KubernetesSerialization().unmarshal("""
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
                              cpu: 200m
                              memory: 300Mi
                            requests:
                              cpu: 200m
                              memory: 300Mi
                        - name: curl
                          image: alpine/curl
                          command: ["/bin/sh", "-c", "tail -f /dev/null"]""", Deployment.class);

        var uuid = k8sTestRunnerService.runTest(deployment, "crypto", "8080/actuator/prometheus",
                "exkaset/gatling@sha256:4810aec32e1862453419c776217e63b346d7a05a499251ca6f840364b9e1c71f",
                "bash -c \"mvn gatling:test > /dev/null 2> /dev/null &\"", 60, 60,
                List.of(new MetricMaxValueDto()
                        .setQuery("sum(gatling_count_total{type=\"ko\"}) / sum(gatling_count_total{type=\"ok\"}) * 100")
                        .setMaxValue(10)
                ));

        log.info("UUID of test: " + uuid);
    }

}
