package last.project.jvmtuner;

import last.project.jvmtuner.service.K8sDeployService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor
class JvmTunerApplicationTests {

    private final K8sDeployService k8sDeployService;

    @Test
    void contextLoads() {
        k8sDeployService.deploy();
    }

}
