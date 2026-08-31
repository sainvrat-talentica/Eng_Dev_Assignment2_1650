package com.swifteats.analytics.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AnalyticsProperties.class)
@ServiceScope(ServiceName.ANALYTICS)
public class AnalyticsConfig {
}
