package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import last.project.jvmtuner.annotation.AppTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

@AppTest
@RequiredArgsConstructor
class K8sDeployServiceTest {

    private final K8sDeployService k8sDeployService;

    @Test
    void deployTest() {
        var client = new KubernetesClientBuilder().build();
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


        k8sDeployService.prepareDeployment(deployment, "8080/actuator/prometheus",
                "exkaset/gatling@sha256:4810aec32e1862453419c776217e63b346d7a05a499251ca6f840364b9e1c71f",
                "crypto");
        k8sDeployService.deploy(deployment, client);
    }

}
