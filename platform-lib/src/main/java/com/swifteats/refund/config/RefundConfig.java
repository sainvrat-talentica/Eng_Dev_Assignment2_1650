package com.swifteats.refund.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@ServiceScope(ServiceName.REFUND)
public class RefundConfig {
}
