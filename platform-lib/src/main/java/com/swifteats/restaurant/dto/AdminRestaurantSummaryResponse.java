package com.swifteats.restaurant.dto;

import com.swifteats.common.domain.RestaurantStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminRestaurantSummaryResponse(
        UUID id,
        String name,
        String city,
        RestaurantStatus status,
        boolean isOpen,
        Instant createdAt,
        Instant updatedAt) {}
