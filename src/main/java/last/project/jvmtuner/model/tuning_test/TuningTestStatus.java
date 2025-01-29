package last.project.jvmtuner.model.tuning_test;

public enum TuningTestStatus {
    NOT_READY,
    RUNNING,
    ENDED,
    PROCESSED,
    FAILED_READY,
    FAILED_RUNNING,
    FAILED_PROCESSING;

    public boolean isFailed() {
        return this.equals(FAILED_READY) || this.equals(FAILED_RUNNING) || this.equals(FAILED_PROCESSING);
    }

    public boolean isCompleted() {
        return this.isFailed() || this.equals(PROCESSED);
    }
}
