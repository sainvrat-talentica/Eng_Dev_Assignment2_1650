package com.swifteats.restaurant.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RestaurantSummaryResponse(
        UUID id,
        String name,
        String city,
        BigDecimal rating,
        boolean isOpen,
        List<String> cuisines,
        Integer estimatedWaitMins) {
}
