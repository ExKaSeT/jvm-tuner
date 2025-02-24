package last.project.jvmtuner.model.tuning_task;

import last.project.jvmtuner.service.tuning_task.mode.*;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public enum TuningMode {
    MAX_HEAP_SIZE,
    SERIAL_GC,
    PARALLEL_GC,
    G1_GC,
    SHENANDOAH_GC,
    Z_GC;

    public static Class<? extends TuningModeService> getServiceClass(TuningMode mode) {
        switch (mode) {
            case MAX_HEAP_SIZE -> {
                return MaxHeapSizeService.class;
            }
            case SERIAL_GC -> {
                return SerialGCService.class;
            }
            case PARALLEL_GC -> {
                return ParallelGCService.class;
            }
            case G1_GC -> {
                return G1GCService.class;
            }
            case SHENANDOAH_GC -> {
                return ShenandoahGCService.class;
            }
            case Z_GC -> {
                return ZGCService.class;
            }
            default -> throw new NotImplementedException();
        }
    }

    public static List<TuningMode> getAvailableModes() {
        return List.of(TuningMode.values());
    }
}
