package com.swifteats.restaurant.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.restaurant.dto.AdminRestaurantSummaryResponse;
import com.swifteats.restaurant.dto.CreateMenuItemRequest;
import com.swifteats.restaurant.dto.CreateRestaurantRequest;
import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantDetailResponse;
import com.swifteats.restaurant.dto.UpdateMenuItemRequest;
import com.swifteats.restaurant.dto.UpdateRestaurantRequest;
import com.swifteats.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/restaurants")
@SecurityRequirement(name = OpenApiConfig.ADMIN_API_KEY)
@ServiceScope(ServiceName.ENTITIES)
public class AdminRestaurantController {

    private final RestaurantService restaurantService;

    public AdminRestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public List<AdminRestaurantSummaryResponse> listRestaurants(
            @RequestParam(required = false) String status) {
        return restaurantService.listForAdmin(status);
    }

    @GetMapping("/{id}")
    public RestaurantDetailResponse getRestaurant(@PathVariable UUID id) {
        return restaurantService.getRestaurantForAdmin(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantDetailResponse createRestaurant(@Valid @RequestBody CreateRestaurantRequest request) {
        return restaurantService.createRestaurant(request);
    }

    @PatchMapping("/{id}")
    public RestaurantDetailResponse updateRestaurant(
            @PathVariable UUID id, @Valid @RequestBody UpdateRestaurantRequest request) {
        return restaurantService.updateRestaurant(id, request);
    }

    @DeleteMapping("/{id}")
    public RestaurantDetailResponse softDeleteRestaurant(@PathVariable UUID id) {
        return restaurantService.softDeleteRestaurant(id);
    }

    @PatchMapping("/{id}/approve")
    public RestaurantDetailResponse approveRestaurant(@PathVariable UUID id) {
        return restaurantService.approveRestaurant(id);
    }

    @PostMapping("/{id}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse addMenuItem(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMenuItemRequest request) {
        return restaurantService.addMenuItem(id, request);
    }

    @PatchMapping("/{id}/menu-items/{itemId}")
    public MenuItemResponse updateMenuItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        return restaurantService.updateMenuItem(id, itemId, request);
    }

    @DeleteMapping("/{id}/menu-items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenuItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        restaurantService.deleteMenuItem(id, itemId);
    }
}
