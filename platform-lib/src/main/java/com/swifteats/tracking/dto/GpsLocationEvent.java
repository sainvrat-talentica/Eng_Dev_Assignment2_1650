package com.swifteats.tracking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GpsLocationEvent(
        UUID driverId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal heading,
        Instant timestamp,
        UUID orderId) {
}
