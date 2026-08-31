package com.swifteats.order.payment;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.order.client.OrderInternalClient;
import com.swifteats.order.dto.PaymentProcessMessage;
import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private MockPaymentGateway paymentGateway;
    @Mock
    private OrderInternalClient orderInternalClient;

    @InjectMocks
    private PaymentProcessingService paymentProcessingService;

    @Test
    void process_transitionsToConfirmedOnSuccess() {
        PaymentProcessMessage message = new PaymentProcessMessage(ORDER_ID, new BigDecimal("350.00"));
        when(paymentGateway.charge(new PaymentRequest(ORDER_ID, message.amount())))
                .thenReturn(new PaymentResult(true, "TXN-123", "Payment approved"));

        paymentProcessingService.process(message);

        verify(orderInternalClient).transition(
                eq(ORDER_ID), eq(OrderStatus.CONFIRMED), eq("PAYMENT_WORKER"), isNull());
    }

    @Test
    void process_transitionsToPaymentFailedOnGatewayFailure() {
        PaymentProcessMessage message = new PaymentProcessMessage(ORDER_ID, new BigDecimal("350.00"));
        when(paymentGateway.charge(new PaymentRequest(ORDER_ID, message.amount())))
                .thenReturn(new PaymentResult(false, null, "Mock gateway declined transaction"));

        paymentProcessingService.process(message);

        verify(orderInternalClient).transition(
                eq(ORDER_ID),
                eq(OrderStatus.PAYMENT_FAILED),
                eq("PAYMENT_WORKER"),
                eq("Mock gateway declined transaction"));
    }
}
