package com.swifteats.order.payment;

import com.swifteats.order.dto.PaymentProcessMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentWorkerTest {

    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private PaymentProcessingService paymentProcessingService;

    @InjectMocks
    private PaymentWorker paymentWorker;

    @Test
    void onPaymentMessage_delegatesToProcessingService() {
        PaymentProcessMessage message = new PaymentProcessMessage(ORDER_ID, new BigDecimal("250.00"));

        paymentWorker.onPaymentMessage(message);

        verify(paymentProcessingService).process(message);
    }

    @Test
    void onPaymentMessage_rejectsAndDoesNotRequeueOnFailure() {
        PaymentProcessMessage message = new PaymentProcessMessage(ORDER_ID, new BigDecimal("250.00"));
        doThrow(new RuntimeException("gateway down")).when(paymentProcessingService).process(message);

        assertThatThrownBy(() -> paymentWorker.onPaymentMessage(message))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
