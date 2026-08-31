package com.swifteats.restaurant.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RestaurantMenuResponse(
        UUID restaurantId,
        String name,
        boolean isOpen,
        Integer estimatedWaitMins,
        List<MenuItemResponse> menuItems,
        Instant cachedAt) {
}
