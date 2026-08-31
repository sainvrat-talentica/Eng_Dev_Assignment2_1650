package com.swifteats.restaurant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateMenuItemRequest(
        @NotBlank String name,
        String description,
        @NotBlank String category,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        boolean available) {
}
