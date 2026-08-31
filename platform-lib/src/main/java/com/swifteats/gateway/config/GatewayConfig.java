package com.swifteats.gateway.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "swifteats.gateway.enabled", havingValue = "true")
@ServiceScope(ServiceName.BACKEND)
public class GatewayConfig {
}
