package com.swifteats.restaurant.dto;

import com.swifteats.common.domain.RestaurantStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RestaurantDetailResponse(
        UUID id,
        String name,
        String addressLine1,
        String city,
        String state,
        BigDecimal rating,
        boolean isOpen,
        RestaurantStatus status,
        List<String> cuisines,
        String contactEmail,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer estimatedWaitMins,
        Instant createdAt,
        Instant updatedAt) {
}
