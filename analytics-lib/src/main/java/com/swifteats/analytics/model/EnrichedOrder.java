package com.swifteats.analytics.model;

import java.time.Instant;

public record EnrichedOrder(
        long orderId,
        String city,
        String status,
        String failureReason,
        boolean delayed,
        boolean failed,
        Long clientId,
        String clientName,
        Long warehouseId,
        String warehouseName,
        String warehouseNotes,
        String gpsDelayNotes,
        Long driverId,
        String trafficCondition,
        String weatherCondition,
        String eventType,
        String feedbackText,
        String feedbackSentiment,
        Instant orderDate) {
}
