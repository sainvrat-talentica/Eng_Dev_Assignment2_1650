package com.swifteats.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "swifteats.jpa.enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = "com.swifteats")
public class JpaConfig {
}
