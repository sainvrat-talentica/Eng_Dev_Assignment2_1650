package com.swifteats.restaurant.mapper;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantDetailResponse;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantSummaryResponse;
import com.swifteats.restaurant.entity.Cuisine;
import com.swifteats.restaurant.entity.MenuItem;
import com.swifteats.restaurant.entity.Restaurant;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
@ServiceScope({ServiceName.ENTITIES, ServiceName.ORDER, ServiceName.BACKEND})
public class RestaurantMapper {

    public RestaurantSummaryResponse toSummary(Restaurant restaurant) {
        return new RestaurantSummaryResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCity(),
                restaurant.getRating(),
                restaurant.isOpen(),
                cuisineNames(restaurant),
                restaurant.getEstimatedWaitMins());
    }

    public RestaurantDetailResponse toDetail(Restaurant restaurant) {
        return new RestaurantDetailResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddressLine1(),
                restaurant.getCity(),
                restaurant.getState(),
                restaurant.getRating(),
                restaurant.isOpen(),
                restaurant.getStatus(),
                cuisineNames(restaurant),
                restaurant.getContactEmail(),
                restaurant.getOpeningTime(),
                restaurant.getClosingTime(),
                restaurant.getEstimatedWaitMins(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt());
    }

    public RestaurantMenuResponse toMenuResponse(Restaurant restaurant, Instant cachedAt) {
        List<MenuItemResponse> items = restaurant.getMenuItems().stream()
                .sorted(Comparator.comparing(MenuItem::getCategory).thenComparing(MenuItem::getName))
                .map(this::toMenuItem)
                .toList();

        return new RestaurantMenuResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.isOpen(),
                restaurant.getEstimatedWaitMins(),
                items,
                cachedAt);
    }

    public MenuItemResponse toMenuItem(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getPrice(),
                item.isAvailable());
    }

    private List<String> cuisineNames(Restaurant restaurant) {
        return restaurant.getCuisines().stream()
                .map(Cuisine::getName)
                .sorted()
                .toList();
    }
}
