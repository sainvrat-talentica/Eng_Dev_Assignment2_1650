package com.swifteats.order.payment;

import com.swifteats.order.config.PaymentProperties;
import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentGatewayTest {

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private PaymentProperties properties;
    private MockPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.setMockDelayMs(0);
        gateway = new MockPaymentGateway(properties);
    }

    @Test
    void charge_rejectsNonPositiveAmount() {
        PaymentResult result = gateway.charge(new PaymentRequest(ORDER_ID, BigDecimal.ZERO));

        assertThat(result.success()).isFalse();
        assertThat(result.transactionId()).isNull();
        assertThat(result.message()).isEqualTo("Invalid payment amount");
    }

    @Test
    void charge_rejectsNegativeAmount() {
        PaymentResult result = gateway.charge(new PaymentRequest(ORDER_ID, new BigDecimal("-1.00")));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Invalid payment amount");
    }

    @Test
    void charge_declinesWhenFailureRateIsOne() {
        properties.setMockFailureRate(1.0);

        PaymentResult result = gateway.charge(new PaymentRequest(ORDER_ID, new BigDecimal("350.00")));

        assertThat(result.success()).isFalse();
        assertThat(result.transactionId()).isNull();
        assertThat(result.message()).isEqualTo("Mock gateway declined transaction");
    }

    @Test
    void charge_approvesWhenFailureRateIsZero() {
        properties.setMockFailureRate(0.0);

        PaymentResult result = gateway.charge(new PaymentRequest(ORDER_ID, new BigDecimal("350.00")));

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).startsWith("TXN-");
        assertThat(result.message()).isEqualTo("Payment approved");
    }
}
