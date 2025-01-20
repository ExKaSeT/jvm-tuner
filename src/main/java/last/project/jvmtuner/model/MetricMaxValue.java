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
    @AttributeOverride(name="tuningTestProps", column=@Column(name="tuning_test_props_id"))
    @AttributeOverride(name="query", column=@Column(name="query"))
    private MetricMaxValueId metricQueryProps;

    private long value;
}
