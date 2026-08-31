package com.swifteats.tracking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverLocationSnapshot(
        UUID driverId,
        UUID orderId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal heading,
        Instant timestamp) {

    public DriverLocationSnapshot withOrderId(UUID orderId) {
        if (orderId == null || orderId.equals(this.orderId)) {
            return this;
        }
        return new DriverLocationSnapshot(driverId, orderId, latitude, longitude, heading, timestamp);
    }
}
