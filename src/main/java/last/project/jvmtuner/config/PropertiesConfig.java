package last.project.jvmtuner.config;

import last.project.jvmtuner.props.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
public class PropertiesConfig {

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "metrics", ignoreUnknownFields = false)
    public MetricsProps metricsProps() {
        return new MetricsProps();
    }

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "metric-queries", ignoreUnknownFields = false)
    public MetricQueriesProps queriesProps() {
        return new MetricQueriesProps();
    }

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "modes.max-heap-size", ignoreUnknownFields = false)
    public MaxHeapSizeProps maxHeapSizeProps() {
        return new MaxHeapSizeProps();
    }

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "modes.serial-gc", ignoreUnknownFields = false)
    public SerialGCProps serialGcProps() {
        return new SerialGCProps();
    }

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "modes.parallel-gc", ignoreUnknownFields = false)
    public ParallelGCProps parallelGCProps() {
        return new ParallelGCProps();
    }

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "modes.g1-gc", ignoreUnknownFields = false)
    public G1GCProps G1GCProps() {
        return new G1GCProps();
    }

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "modes.shenandoah-gc", ignoreUnknownFields = false)
    public ShenandoahGCProps shenandoahGCProps() {
        return new ShenandoahGCProps();
    }

    @Bean
    @Validated
    @ConfigurationProperties(prefix = "modes.z-gc", ignoreUnknownFields = false)
    public ZGCProps zgcProps() {
        return new ZGCProps();
    }
}