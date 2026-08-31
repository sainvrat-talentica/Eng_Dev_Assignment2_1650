package com.swifteats.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID restaurantId,
        @NotBlank String deliveryAddress,
        String city,
        String state,
        String pincode,
        String paymentMode,
        @NotEmpty List<@Valid OrderLineRequest> items) {

    public record OrderLineRequest(
            @NotNull UUID menuItemId,
            @Min(1) int quantity) {
    }
}
