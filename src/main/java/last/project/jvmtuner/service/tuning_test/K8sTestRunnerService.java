package last.project.jvmtuner.service.tuning_test;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import last.project.jvmtuner.dao.tuning_test.TuningTestRepository;
import last.project.jvmtuner.model.tuning_test.TuningTest;
import last.project.jvmtuner.model.tuning_test.TuningTestProps;
import last.project.jvmtuner.model.tuning_test.TuningTestStatus;
import last.project.jvmtuner.util.K8sDeploymentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Consumer;

@Service
@Transactional
@RequiredArgsConstructor
public class K8sTestRunnerService {

    private final K8sDeployService k8sDeployService;
    private final TuningTestRepository tuningTestRepository;
    private final KubernetesClient k8sClient;

    public TuningTest runTest(TuningTestProps tuningTestProps, Consumer<Deployment> modify) {
        var deployment = K8sDeploymentUtil.deserialize(tuningTestProps.getPreparedDeployment());
        modify.accept(deployment);

        var uuid = k8sDeployService.deploy(deployment, k8sClient);

        var test = new TuningTest()
                .setUuid(uuid)
                .setStatus(TuningTestStatus.NOT_READY)
                .setDeployment(K8sDeploymentUtil.serialize(deployment))
                .setDeploymentName(deployment.getMetadata().getName())
                .setDeployedTime(Instant.now())
                .setTuningTestProps(tuningTestProps);

        return tuningTestRepository.save(test);
    }

    public TuningTest runTest(TuningTestProps tuningTestProps) {
        return this.runTest(tuningTestProps, deployment -> {});
    }
}
