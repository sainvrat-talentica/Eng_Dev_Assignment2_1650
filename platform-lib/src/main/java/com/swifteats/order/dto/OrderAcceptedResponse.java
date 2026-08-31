package com.swifteats.order.dto;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderAcceptedResponse(
        UUID orderId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String message) {
}
