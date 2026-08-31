package com.swifteats.restaurant.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateMenuItemRequest(
        String name,
        String description,
        String category,
        @DecimalMin("0.01") BigDecimal price,
        Boolean available) {
}
