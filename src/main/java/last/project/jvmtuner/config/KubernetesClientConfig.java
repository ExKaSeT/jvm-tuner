package last.project.jvmtuner.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class KubernetesClientConfig {

    @Value("${k8s-client.config-path}")
    private String configPath;

    @Bean
    @SneakyThrows
    public KubernetesClient getK8sClient() {
        String config = Files.readString(Paths.get(configPath));
        return new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(config))
                .build();
    }
}
