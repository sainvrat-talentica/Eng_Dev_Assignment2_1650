package com.swifteats.order.payment;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.order.config.PaymentProperties;
import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ServiceScope({ServiceName.PAYMENT, ServiceName.ORDER})
public class MockPaymentGateway {

    private final PaymentProperties properties;

    public MockPaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    public PaymentResult charge(PaymentRequest request) {
        simulateLatency();
        if (request.amount().signum() <= 0) {
            return new PaymentResult(false, null, "Invalid payment amount");
        }
        if (ThreadLocalRandom.current().nextDouble() < properties.getMockFailureRate()) {
            return new PaymentResult(false, null, "Mock gateway declined transaction");
        }
        return new PaymentResult(true, "TXN-" + UUID.randomUUID(), "Payment approved");
    }

    private void simulateLatency() {
        long delay = properties.getMockDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
