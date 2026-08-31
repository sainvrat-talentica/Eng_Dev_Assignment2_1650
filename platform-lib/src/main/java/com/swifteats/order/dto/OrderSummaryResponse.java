package com.swifteats.order.dto;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        UUID restaurantId,
        String restaurantName,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String deliveryAddressLine1,
        String city,
        Instant createdAt,
        Instant updatedAt,
        Instant paymentProcessedAt) {}
