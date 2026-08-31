package com.swifteats.restaurant.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RestaurantProperties.class)
@ServiceScope({ServiceName.ENTITIES, ServiceName.ORDER, ServiceName.BACKEND})
public class RestaurantConfig {
}
