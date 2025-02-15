package last.project.jvmtuner.util;

import last.project.jvmtuner.dto.tuning_test.MetricMaxValueDto;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.service.tuning_test.TuningTestPropsService;

import java.util.List;

public class GetBaseData {
    public static TuningTestProps getTestProps(TuningTestPropsService service) {
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
                              value: '-XX:+PrintCommandLineFlags -Xmx470M -Xms470M'
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

        return service.saveTuningTestProps(deployment, "crypto",
                "8080/actuator/prometheus", "exkaset/gatling:1.0",
                "bash -c \"mvn gatling:test > /dev/null 2> /dev/null &\"",
                90, 400,
                List.of(new MetricMaxValueDto()
                        .setQuery("sum(gatling_count_total{type=\"ko\", $jvm_tuner_id}) / sum(gatling_count_total{type=\"ok\", $jvm_tuner_id}) * 100")
                        .setMaxValue(10)));
    }
}
