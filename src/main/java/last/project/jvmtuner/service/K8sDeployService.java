package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class K8sDeployService {

    private static final String VM_AGENT_IMAGE = "victoriametrics/vmagent:latest";
    private static final String VM_AGENT_CONFIG = "global:\n" +
            "  external_labels:\n" +
            "    jvm-tuner-id: %s\n" +
            "scrape_configs:\n" +
            "- job_name: \"app\"\n" +
            "    scrape_interval: 10s\n" +
            "    scrape_timeout: 10s\n" +
            "    honor_labels: true\n" +
            "    static_configs:\n" +
            "      - targets:\n" +
            "        - %s";

    public void deploy() {
        var uuid = UUID.randomUUID();

        var client = new KubernetesClientBuilder().build();
        String namespace = "default";
        String containerName = "demo-metrics";

        Deployment deployment = new KubernetesSerialization().unmarshal("kind: Deployment\n" +
                "apiVersion: apps/v1\n" +
                "metadata:\n" +
                "  name: jvm-tuner\n" +
                "  namespace: default\n" +
                "  labels:\n" +
                "    app: jvm-tuner\n" +
                "spec:\n" +
                "  replicas: 1\n" +
                "  selector:\n" +
                "    matchLabels:\n" +
                "      app: jvm-tuner\n" +
                "  template:\n" +
                "    metadata:\n" +
                "      creationTimestamp: null\n" +
                "      labels:\n" +
                "        app: jvm-tuner\n" +
                "    spec:\n" +
                "      containers:\n" +
                "        - name: demo-metrics\n" +
                "          image: exkaset/demo-metrics\n" +
                "          ports:\n" +
                "            - containerPort: 8080\n" +
                "              protocol: TCP\n" +
                "          resources:\n" +
                "            limits:\n" +
                "              cpu: 250m\n" +
                "              memory: 500Mi\n" +
                "            requests:\n" +
                "              cpu: 250m\n" +
                "              memory: 500Mi", Deployment.class);

        Container targetContainer = deployment.getSpec().getTemplate().getSpec().getContainers()
                .stream()
                .filter(c -> c.getName().equals(containerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + containerName));

        List<EnvVar> envVars = targetContainer.getEnv();
        boolean javaToolOptionsExists = envVars.stream()
                .anyMatch(envVar -> envVar.getName().equals("JAVA_TOOL_OPTIONS"));

        if (javaToolOptionsExists) {
            throw new IllegalArgumentException("JAVA_TOOL_OPTIONS уже существует в контейнере: " + containerName);
        }

        // Добавляем переменную окружения JAVA_TOOL_OPTIONS
        envVars.add(new EnvVar("JAVA_TOOL_OPTIONS", "-XX:+UseContainerSupport", null));

        String vmAgentConfigVolumeName = "vm-agent-config-" + uuid;
        String vmAgentConfigFilePath = "/etc/vmagent/";
        String vmAgentConfigFileName = "config.yaml";

        deployment.getSpec().getTemplate().getSpec().getVolumes().add(new VolumeBuilder().withName(vmAgentConfigVolumeName).withEmptyDir(new EmptyDirVolumeSource()).build());

        Container vmAgentConfigInitContainer = new ContainerBuilder()
                .withName("vm-agent-init-" + uuid)
                .withImage("busybox")
                .withCommand("sh", "-c", "echo \"" + String.format(VM_AGENT_CONFIG, uuid, "http://localhost:8080/actuator/prometheus") + "\" > " + vmAgentConfigFilePath + vmAgentConfigFileName)
                .withVolumeMounts(new VolumeMountBuilder()
                        .withName(vmAgentConfigVolumeName)
                        .withMountPath(vmAgentConfigFilePath)
                        .build())
                .build();

        deployment.getSpec().getTemplate().getSpec().getInitContainers().add(vmAgentConfigInitContainer);

        Container vmAgentContainer = new ContainerBuilder()
                .withName("vm-agent-" + uuid)
                .withImage(VM_AGENT_IMAGE)
                .withPorts(new ContainerPortBuilder()
                        .withName("vmagent")
                        .withContainerPort(8429)
                        .build())
                .withArgs("-remoteWrite.url=http://192.168.1.82:8428/api/v1/write",
                        "-config.file=" + vmAgentConfigFilePath + vmAgentConfigFileName)
                .withVolumeMounts(new VolumeMountBuilder()
                        .withName(vmAgentConfigVolumeName)
                        .withMountPath(vmAgentConfigFilePath)
                        .build())
                .build();

        deployment.getSpec().getTemplate().getSpec().getContainers().add(vmAgentContainer);

        client.apps().deployments().inNamespace(namespace).resource(deployment).createOrReplace();

        System.out.println("Deployment успешно обновлен!");
        client.close();
    }
}
