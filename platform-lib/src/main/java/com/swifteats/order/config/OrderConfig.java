package com.swifteats.order.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.tracking.config.TrackingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PaymentProperties.class, TrackingProperties.class})
@ServiceScope(ServiceName.ORDER)
public class OrderConfig {
}
