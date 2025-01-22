package last.project.jvmtuner.service;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class K8sExecService {

    private final KubernetesClient k8sClient;

    public void execCommandInPod(String podName, String namespace, List<String> cmd) {
        try (var exec = k8sClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(K8sDeployService.GATLING_CONTAINER_NAME)
                .writingOutput(OutputStream.nullOutputStream())
                .exec(cmd.toArray(new String[0]))
        ) {
            int resultCode = exec.exitCode().join();
            if (resultCode != 0) {
                log.error(String.format("Unexpected code (%d) while executing command '%s' in pod '%s' in namespace '%s'",
                        resultCode, cmd, podName, namespace));
            }
        }
    }
}
