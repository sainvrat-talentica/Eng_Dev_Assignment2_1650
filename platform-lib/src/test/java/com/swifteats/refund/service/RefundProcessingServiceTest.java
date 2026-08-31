package com.swifteats.refund.service;

import com.swifteats.refund.config.RefundProperties;
import com.swifteats.refund.entity.Refund;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefundProcessingServiceTest {

    private RefundProperties properties;
    private RefundProcessingService service;

    @BeforeEach
    void setUp() {
        properties = new RefundProperties();
        properties.setMockDelayMs(0);
        service = new RefundProcessingService(properties);
    }

    @Test
    void process_succeedsWhenFailureRateIsZero() {
        properties.setMockFailureRate(0.0);

        RefundResult result = service.process(sampleRefund());

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Refund processed");
    }

    @Test
    void process_failsWhenFailureRateIsOne() {
        properties.setMockFailureRate(1.0);

        RefundResult result = service.process(sampleRefund());

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("declined");
    }

    private static Refund sampleRefund() {
        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setOrderId(UUID.randomUUID());
        return refund;
    }
}
