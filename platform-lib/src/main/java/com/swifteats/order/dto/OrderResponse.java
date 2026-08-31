package com.swifteats.order.dto;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID customerId,
        UUID restaurantId,
        UUID driverId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String deliveryAddressLine1,
        String city,
        String failureReason,
        String delayReason,
        Instant promisedDeliveryAt,
        Instant prepStartedAt,
        Instant outForDeliveryAt,
        Instant deliveredAt,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        Instant paymentProcessedAt) {

    public record OrderItemResponse(
            UUID id,
            UUID menuItemId,
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }
}
