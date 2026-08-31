package com.swifteats.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "swifteats.payment.messaging-enabled", havingValue = "true", matchIfMissing = true)
@ServiceScope(ServiceName.ORDER)
public class OrderPaymentPublishConfig {

    @Bean
    MessageConverter orderPaymentMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
