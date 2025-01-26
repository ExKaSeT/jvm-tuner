package last.project.jvmtuner.util;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class K8sDeploymentUtil {

    private static final String JAVA_OPTIONS_ENV = "JAVA_TOOL_OPTIONS";

    public static String serialize(Deployment deployment) {
        return Serialization.asJson(deployment);
    }

    public static Deployment deserialize(String deployment) {
        return Serialization.unmarshal(deployment, Deployment.class);
    }

    public static Consumer<Deployment> addJvmOptions(List<String> options, String containerName) {
        return deployment -> {
            var appContainer = deployment.getSpec().getTemplate().getSpec().getContainers().stream()
                    .filter(c -> c.getName().equals(containerName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Container not found: " + containerName));

            var envVars = appContainer.getEnv();
            var currentOptionsEnv = envVars.stream()
                    .filter(envVar -> envVar.getName().equals(JAVA_OPTIONS_ENV))
                    .findFirst();

            if (currentOptionsEnv.isPresent()) {
                var newOptions = Arrays.stream(currentOptionsEnv.get().getValue().trim().split("\\s+"))
                        .collect(Collectors.toList());
                options.stream().filter(o -> !newOptions.contains(o)).forEach(newOptions::add);

                currentOptionsEnv.get().setValue(String.join(" ", newOptions));
            } else {
                envVars.add(new EnvVar(JAVA_OPTIONS_ENV, String.join(" ", options), null));
            }
        };
    }
}
