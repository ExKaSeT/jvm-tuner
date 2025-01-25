package last.project.jvmtuner.config;

import last.project.jvmtuner.props.MetricQueriesProps;
import last.project.jvmtuner.props.MetricsProps;
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
}