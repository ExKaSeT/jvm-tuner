package last.project.jvmtuner.model.tuning_test;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Entity
@Table(name = "tuning_test_metrics")
@Data
@Accessors(chain = true)
public class TuningTestMetrics {

    @Id
    @Column(name = "tuning_test_uuid", nullable = false)
    private UUID tuningTestUuid;

    @OneToOne
    @MapsId
    @JoinColumn(name = "tuning_test_uuid", referencedColumnName = "uuid", nullable = false)
    private TuningTest tuningTest;

    @Column(name = "cpu_usage_avg", nullable = false)
    private Double cpuUsageAvg;

    @Column(name = "cpu_throttling_avg", nullable = false)
    private Double cpuThrottlingAvg;

    @Column(name = "memory_usage_avg", nullable = false)
    private Double memoryUsageAvg;

    @Column(name = "memory_wss_avg", nullable = false)
    private Double memoryWssAvg;

    @Column(name = "memory_rss_avg", nullable = false)
    private Double memoryRssAvg;
}
