package last.project.jvmtuner.util;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.utils.Serialization;

public class K8sDeploymentUtil {


    public static String serialize(Deployment deployment) {
        return Serialization.asJson(deployment);
    }

    public static Deployment deserialize(String deployment) {
        return Serialization.unmarshal(deployment, Deployment.class);
    }
}
