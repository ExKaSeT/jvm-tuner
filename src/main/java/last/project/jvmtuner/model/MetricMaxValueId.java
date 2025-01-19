package last.project.jvmtuner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Embeddable
@Accessors(chain = true)
public class MetricMaxValueId implements Serializable {
    @ManyToOne
    @JoinColumn(name = "tuning_test_uuid", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TuningTest tuningTest;

    private String query;
}