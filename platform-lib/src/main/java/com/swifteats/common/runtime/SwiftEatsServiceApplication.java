package com.swifteats.common.runtime;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.swifteats")
@EnableScheduling
@ComponentScan(
        basePackages = "com.swifteats",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.CUSTOM,
                classes = ServiceScopeExcludeFilter.class))
public @interface SwiftEatsServiceApplication {
}
