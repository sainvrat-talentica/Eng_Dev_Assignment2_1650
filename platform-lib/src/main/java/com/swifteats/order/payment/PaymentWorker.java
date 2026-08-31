package com.swifteats.order.payment;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.order.config.PaymentMessagingConfig;
import com.swifteats.order.dto.PaymentProcessMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "swifteats.payment.messaging-enabled", havingValue = "true", matchIfMissing = true)
@ServiceScope(ServiceName.PAYMENT)
public class PaymentWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentWorker.class);

    private final PaymentProcessingService paymentProcessingService;

    public PaymentWorker(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    @RabbitListener(queues = PaymentMessagingConfig.PAYMENT_QUEUE)
    public void onPaymentMessage(PaymentProcessMessage message) {
        try {
            paymentProcessingService.process(message);
        } catch (Exception ex) {
            log.error("Payment worker failed for order {}", message.orderId(), ex);
            throw new AmqpRejectAndDontRequeueException(ex);
        }
    }
}
