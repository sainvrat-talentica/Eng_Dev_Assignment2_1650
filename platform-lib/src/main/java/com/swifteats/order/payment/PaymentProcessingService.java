package com.swifteats.order.payment;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.order.dto.PaymentProcessMessage;
import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import com.swifteats.order.client.OrderInternalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@ServiceScope(ServiceName.PAYMENT)
public class PaymentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);

    private final MockPaymentGateway paymentGateway;
    private final OrderInternalClient orderInternalClient;

    public PaymentProcessingService(
            MockPaymentGateway paymentGateway,
            @Qualifier("httpOrderInternalClient") OrderInternalClient orderInternalClient) {
        this.paymentGateway = paymentGateway;
        this.orderInternalClient = orderInternalClient;
    }

    public void process(PaymentProcessMessage message) {
        log.info("Processing payment for order {}", message.orderId());
        PaymentResult result = paymentGateway.charge(new PaymentRequest(message.orderId(), message.amount()));
        if (result.success()) {
            orderInternalClient.transition(message.orderId(), OrderStatus.CONFIRMED, "PAYMENT_WORKER", null);
            log.info("Payment succeeded for order {}", message.orderId());
        } else {
            orderInternalClient.transition(
                    message.orderId(),
                    OrderStatus.PAYMENT_FAILED,
                    "PAYMENT_WORKER",
                    result.message());
            log.warn("Payment failed for order {}: {}", message.orderId(), result.message());
        }
    }
}
