package com.swifteats.restaurant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalTime;
import java.util.List;

public record CreateRestaurantRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        @NotEmpty List<@NotBlank String> cuisines,
        @Email String contactEmail,
        LocalTime openingTime,
        LocalTime closingTime) {
}
