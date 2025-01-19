package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.PostConstruct;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class K8sDeployService {

    private static final String VM_AGENT_IMAGE = "victoriametrics/vmagent:latest";
    private static final String VM_AGENT_CONFIG_PATH = "/etc/vmagent/";
    private static final String VM_AGENT_CONFIG_FILE_NAME = "config.yaml";
    private static String VM_AGENT_CONFIG = """
            global:
              external_labels:
                %s: %s
            
            scrape_configs:
              - job_name: "app"
                scrape_interval: 15s
                scrape_timeout: 10s
                static_configs:
                  - targets:
                      - %s
            """;
    private static final String VM_AGENT_RELABEL_CONFIG_FILE_NAME = "relabel.yaml";
    private static final String VM_AGENT_RELABEL_CONFIG = """
            - action: graphite
              match: "*.*.*.*.*"
              labels:
                __name__: ${1}_${5}_total
                class: $2
                scn: $3
                type: $4
                metric: $5
                %s: %s
            """;
    private static final String GRAPHITE_PORT = "2003";
    private static final Map<String, Quantity> VMAGENT_LIMITS = Map.of("cpu", Quantity.parse("250m"), "memory", Quantity.parse("250Mi"));

    @Value("${metrics.uuid-label-name}")
    private String uuidLabelName;

    @Value("${metrics.push-url}")
    private String metricsPushUrl;

    @PostConstruct
    private void init() {
        VM_AGENT_CONFIG = String.format(VM_AGENT_CONFIG, uuidLabelName, "%s", "%s");
    }

    public UUID deploy(Deployment app, KubernetesClient client, String appMetricPortWithPath,
                       String gatlingImage) {
        String namespace = client.getNamespace();
        var uuidObject = UUID.randomUUID();
        var uuid = uuidObject.toString();

        var appMeta = app.getMetadata();
        appMeta.setName(uuid);
        appMeta.getLabels().put("app", uuid);
        var appSpec = app.getSpec();
        appSpec.setReplicas(1);
        appSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(Map.of("app", uuid)).build());
        appSpec.getTemplate().getMetadata().getLabels().put("app", uuid);
        appSpec.getTemplate().getMetadata().getLabels().put("group", "jvm-tuner");

        String vmAgentConfigVolumeName = "vm-agent-config-" + uuid;
        app.getSpec().getTemplate().getSpec().getVolumes().add(new VolumeBuilder().withName(vmAgentConfigVolumeName)
                .withEmptyDir(new EmptyDirVolumeSource()).build());

        var vmAgentConfigInitContainer = new ContainerBuilder()
                .withName("vm-agent-init-" + uuid)
                .withImage("busybox")
                .withCommand("sh", "-c", String.format("echo '%s' > %s%s; echo '%s' > %s%s",
                                String.format(VM_AGENT_CONFIG, uuid, "http://localhost:" + appMetricPortWithPath),
                        VM_AGENT_CONFIG_PATH, VM_AGENT_CONFIG_FILE_NAME,
                        String.format(VM_AGENT_RELABEL_CONFIG, uuidLabelName, uuid), VM_AGENT_CONFIG_PATH, VM_AGENT_RELABEL_CONFIG_FILE_NAME))
                .withVolumeMounts(new VolumeMountBuilder()
                        .withName(vmAgentConfigVolumeName)
                        .withMountPath(VM_AGENT_CONFIG_PATH)
                        .build())
                .build();
        app.getSpec().getTemplate().getSpec().getInitContainers().add(vmAgentConfigInitContainer);

        var vmAgentContainer = new ContainerBuilder()
                .withName("vm-agent-" + uuid)
                .withImage(VM_AGENT_IMAGE)
                .withPorts(new ContainerPortBuilder()
                        .withContainerPort(8429)
                        .build())
                .withArgs("-remoteWrite.url=" + metricsPushUrl,
                        "-promscrape.config=" + VM_AGENT_CONFIG_PATH + VM_AGENT_CONFIG_FILE_NAME,
                        "-remoteWrite.relabelConfig=" + VM_AGENT_CONFIG_PATH + VM_AGENT_RELABEL_CONFIG_FILE_NAME,
                        "-graphiteListenAddr=:" + GRAPHITE_PORT)
                .withVolumeMounts(new VolumeMountBuilder()
                        .withName(vmAgentConfigVolumeName)
                        .withMountPath(VM_AGENT_CONFIG_PATH)
                        .build())
                .withResources(new ResourceRequirementsBuilder()
                        .addToLimits(VMAGENT_LIMITS)
                        .addToRequests(VMAGENT_LIMITS)
                        .build())
                .build();
        app.getSpec().getTemplate().getSpec().getContainers().add(vmAgentContainer);

        var gatlingContainer = new ContainerBuilder()
                .withName(K8sDeploymentUtil.getGatlingContainerName(uuidObject))
                .withImage(gatlingImage)
                .build();
        app.getSpec().getTemplate().getSpec().getContainers().add(gatlingContainer);

        client.apps().deployments().inNamespace(namespace).resource(app).create();

        return uuidObject;
    }
}
