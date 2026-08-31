package com.swifteats.order.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ServiceScope(ServiceName.PAYMENT)
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentServiceConfig {
}
