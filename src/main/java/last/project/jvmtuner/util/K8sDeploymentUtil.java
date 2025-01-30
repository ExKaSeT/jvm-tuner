package last.project.jvmtuner.util;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class K8sDeploymentUtil {

    private static final String JAVA_OPTIONS_ENV = "JAVA_TOOL_OPTIONS";
    private static final String POD_MEMORY_LIMITS_NAME = "memory";
    private static final BigDecimal DIVISOR_BYTES_TO_MB = new BigDecimal(1048576);

    public static String serialize(Deployment deployment) {
        return Serialization.asJson(deployment);
    }

    public static Deployment deserialize(String deployment) {
        return Serialization.unmarshal(deployment, Deployment.class);
    }

    public static Consumer<Deployment> addJvmOptions(List<String> options, String containerName,
                                                     Consumer<List<String>> modifyResultOptionList) {
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

                modifyResultOptionList.accept(newOptions);

                currentOptionsEnv.get().setValue(String.join(" ", newOptions));
            } else {
                envVars.add(new EnvVar(JAVA_OPTIONS_ENV, String.join(" ", options), null));
            }
        };
    }

    public static Consumer<Deployment> addJvmOptions(List<String> options, String containerName) {
        return addJvmOptions(options, containerName, l -> {});
    }


    public static int getAppMemoryLimitsMB(TuningTestProps testProps) {
        var deployment = deserialize(testProps.getPreparedDeployment());

        var containerName = testProps.getAppContainerName();
        var container = deployment.getSpec().getTemplate().getSpec().getContainers().stream()
                .filter(c -> c.getName().equals(containerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Container '%s' not found", containerName))
                );
        return container.getResources().getLimits().get(POD_MEMORY_LIMITS_NAME)
                .getNumericalAmount()
                .divide(DIVISOR_BYTES_TO_MB)
                .intValue();
    }
}
