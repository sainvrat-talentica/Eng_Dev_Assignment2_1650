package com.swifteats.restaurant.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String name,
        String category,
        BigDecimal price,
        boolean available) {
}
