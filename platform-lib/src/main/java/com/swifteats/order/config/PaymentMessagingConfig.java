package com.swifteats.order.config;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "swifteats.payment.messaging-enabled", havingValue = "true", matchIfMissing = true)
@ServiceScope(ServiceName.PAYMENT)
public class PaymentMessagingConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_QUEUE = "payment.process.queue";
    public static final String PAYMENT_ROUTING_KEY = "payment.process";
    public static final String PAYMENT_DLQ = "payment.process.dlq";

    @Bean
    DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .deadLetterExchange(PAYMENT_EXCHANGE)
                .deadLetterRoutingKey("payment.dlq")
                .build();
    }

    @Bean
    Queue paymentDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    Binding paymentBinding(Queue paymentQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    Binding paymentDlqBinding(Queue paymentDeadLetterQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentDeadLetterQueue).to(paymentExchange).with("payment.dlq");
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
