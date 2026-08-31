package com.swifteats.restaurant.dto;

import jakarta.validation.constraints.Email;

import java.time.LocalTime;
import java.util.List;

public record UpdateRestaurantRequest(
        String name,
        String address,
        String city,
        List<String> cuisines,
        @Email String contactEmail,
        LocalTime openingTime,
        LocalTime closingTime,
        Boolean isOpen,
        Integer estimatedWaitMins) {}
