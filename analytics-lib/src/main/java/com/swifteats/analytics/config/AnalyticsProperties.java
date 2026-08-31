package com.swifteats.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "swifteats.analytics")
public record AnalyticsProperties(
        boolean importOnStartup,
        boolean demoOnStartup,
        String datasetPath,
        int batchSize
) {
}
