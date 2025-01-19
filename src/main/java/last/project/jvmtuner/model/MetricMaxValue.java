package last.project.jvmtuner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Entity
@Table(name = "metric_max_values")
@Data
@Accessors(chain = true)
public class MetricMaxValue {

    @EmbeddedId
    @AttributeOverride(name="tuningTest", column=@Column(name="tuning_test_uuid"))
    @AttributeOverride(name="name", column=@Column(name="name"))
    private MetricMaxValueId metricNameTest;

    private long value;
}
