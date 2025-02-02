package last.project.jvmtuner.dto.mode;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class MaxHeapSizeDto {
    private Test currentTest;
    // Предыдущий тест с прошлым heap size (повторные тесты сюда не устанавливаются)
    private Test prevTest;
    // Кол-во повторов текущего теста
    private int retryCount;
    // Верхняя граница, установленная при изменении направления (больше к ней не должны вернуться)
    private Integer maxHeapBound;
    // Нижняя граница, установленная при изменении направления (больше к ней не должны вернуться)
    private Integer minHeapBound;
    // Лимиты контейнера в k8s
    private int containerLimitMB;
    // Текущий размер шага
    private int stepSizeMB;
    // Размер шага, использующийся, чтобы найти рабочую конфигурацию в текущем направлении
    private Integer localStepSizeMB;
    // Начальный тест без изменений Heap Size
    private boolean isInitialTest = true;
    // Направление изменения размера Heap (true = увеличение)
    private boolean isIncreaseDirection = true;

    @Data
    @Accessors(chain = true)
    public static class Test {
        private UUID uuid;
        private Integer heapSizeMB;
    }
}
