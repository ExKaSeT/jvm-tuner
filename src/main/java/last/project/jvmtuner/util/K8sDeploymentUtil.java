package last.project.jvmtuner.util;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.util.UUID;

public class K8sDeploymentUtil {

    public static String serialize(Deployment deployment) {
        return Serialization.asJson(deployment);
    }

    public static String getGatlingContainerName(UUID uuid) {
        return "gatling-" + uuid;
    }

    public static void addJVMOptions(Deployment deployment) {
        // TODO:
    }
}
