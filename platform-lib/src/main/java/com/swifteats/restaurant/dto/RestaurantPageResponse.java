package com.swifteats.restaurant.dto;

import java.util.List;

public record RestaurantPageResponse(
        List<RestaurantSummaryResponse> content,
        int page,
        int size,
        long totalElements) {
}
