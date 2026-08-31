package com.swifteats.restaurant.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import com.swifteats.restaurant.service.RestaurantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
@ServiceScope({ServiceName.ENTITIES, ServiceName.ORDER, ServiceName.BACKEND})
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public RestaurantPageResponse listRestaurants(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) Boolean isOpen,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return restaurantService.searchRestaurants(city, cuisine, isOpen, minRating, name, page, size);
    }

    @GetMapping("/{id}/menu")
    public RestaurantMenuResponse getMenu(@PathVariable UUID id) {
        return restaurantService.getMenu(id);
    }
}
