package last.project.jvmtuner.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.PostConstruct;
import last.project.jvmtuner.props.MetricsProps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class K8sDeployService {
    public static final String GATLING_CONTAINER_NAME = "gatling";
    private static final String VM_AGENT_IMAGE = "victoriametrics/vmagent:latest";
    private static final String VM_AGENT_CONFIG_PATH = "/etc/vmagent/";
    private static final String VM_AGENT_CONFIG_FILE_NAME = "config.yaml";
    private static final String UUID_DEFAULT = "bb89d48e-046b-430b-ae95-c1d61599d98a";
    private static final String VM_AGENT_CONFIG_VOLUME_NAME = "vm-agent-config";
    private static final String VM_AGENT_INIT_CONTAINER_NAME = "vm-agent-init";
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

    private final MetricsProps metricsProps;

    @PostConstruct
    private void init() {
        VM_AGENT_CONFIG = String.format(VM_AGENT_CONFIG, metricsProps.getUuidLabelName(), "%s", "%s");
    }

    public void prepareDeployment(Deployment app, String appMetricPortWithPath, String gatlingImage,
                                  String appContainerName) {
        var containerExists = app.getSpec().getTemplate().getSpec().getContainers().stream()
                .anyMatch(container -> appContainerName.equals(container.getName()));
        if (!containerExists) {
            throw new IllegalArgumentException("Deployment doesn't contain container " + appContainerName);
        }

        app.getSpec().getTemplate().getSpec().getVolumes().add(new VolumeBuilder().withName(VM_AGENT_CONFIG_VOLUME_NAME)
                .withEmptyDir(new EmptyDirVolumeSource()).build());

        var vmAgentConfigInitContainer = new ContainerBuilder()
                .withName(VM_AGENT_INIT_CONTAINER_NAME)
                .withImage("busybox")
                .withCommand("sh", "-c", String.format("echo '%s' > %s%s; echo '%s' > %s%s",
                                String.format(VM_AGENT_CONFIG, UUID_DEFAULT, "http://localhost:" + appMetricPortWithPath),
                        VM_AGENT_CONFIG_PATH, VM_AGENT_CONFIG_FILE_NAME,
                        String.format(VM_AGENT_RELABEL_CONFIG, metricsProps.getUuidLabelName(), UUID_DEFAULT), VM_AGENT_CONFIG_PATH, VM_AGENT_RELABEL_CONFIG_FILE_NAME))
                .withVolumeMounts(new VolumeMountBuilder()
                        .withName(VM_AGENT_CONFIG_VOLUME_NAME)
                        .withMountPath(VM_AGENT_CONFIG_PATH)
                        .build())
                .build();
        app.getSpec().getTemplate().getSpec().getInitContainers().add(vmAgentConfigInitContainer);

        var vmAgentContainer = new ContainerBuilder()
                .withName("vm-agent")
                .withImage(VM_AGENT_IMAGE)
                .withPorts(new ContainerPortBuilder()
                        .withContainerPort(8429)
                        .build())
                .withArgs("-remoteWrite.url=" + metricsProps.getPushUrl(),
                        "-promscrape.config=" + VM_AGENT_CONFIG_PATH + VM_AGENT_CONFIG_FILE_NAME,
                        "-remoteWrite.relabelConfig=" + VM_AGENT_CONFIG_PATH + VM_AGENT_RELABEL_CONFIG_FILE_NAME,
                        "-graphiteListenAddr=:" + GRAPHITE_PORT)
                .withVolumeMounts(new VolumeMountBuilder()
                        .withName(VM_AGENT_CONFIG_VOLUME_NAME)
                        .withMountPath(VM_AGENT_CONFIG_PATH)
                        .build())
                .withResources(new ResourceRequirementsBuilder()
                        .addToLimits(VMAGENT_LIMITS)
                        .addToRequests(VMAGENT_LIMITS)
                        .build())
                .build();
        app.getSpec().getTemplate().getSpec().getContainers().add(vmAgentContainer);

        var gatlingContainer = new ContainerBuilder()
                .withName(GATLING_CONTAINER_NAME)
                .withImage(gatlingImage)
                .build();
        app.getSpec().getTemplate().getSpec().getContainers().add(gatlingContainer);

        app.getSpec().setReplicas(1);
    }

    public UUID deploy(Deployment app, KubernetesClient client) {
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

        var vmAgentInit = appSpec.getTemplate().getSpec().getInitContainers().stream()
                .filter(container -> VM_AGENT_INIT_CONTAINER_NAME.equals(container.getName()))
                .findFirst()
                .get();
        vmAgentInit.setCommand(vmAgentInit.getCommand().stream()
                .map(arg -> arg.replace(UUID_DEFAULT, uuid))
                .collect(Collectors.toList()));

        var namespace = app.getMetadata().getNamespace();
        client.apps().deployments().inNamespace(namespace).resource(app).create();
        return uuidObject;
    }
}
