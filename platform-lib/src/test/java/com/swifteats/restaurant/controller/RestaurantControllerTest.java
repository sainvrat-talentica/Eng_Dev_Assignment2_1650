package com.swifteats.restaurant.controller;

import com.swifteats.common.security.AdminApiKeyFilter;
import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import com.swifteats.restaurant.dto.RestaurantSummaryResponse;
import com.swifteats.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final String ADMIN_API_KEY = "dev-admin-key";

    @Mock
    private RestaurantService restaurantService;

    private MockMvc publicApi;
    private MockMvc adminApi;

    @BeforeEach
    void setUp() {
        publicApi = MockMvcBuilders.standaloneSetup(new RestaurantController(restaurantService)).build();
        adminApi = MockMvcBuilders.standaloneSetup(new AdminRestaurantController(restaurantService))
                .addFilters(new AdminApiKeyFilter(ADMIN_API_KEY))
                .build();
    }

    @Test
    void listRestaurants_returnsPage() throws Exception {
        RestaurantSummaryResponse summary = new RestaurantSummaryResponse(
                RESTAURANT_ID, "Misal House", "Pune", BigDecimal.valueOf(4.5), true,
                List.of("Maharashtrian"), 25);
        when(restaurantService.searchRestaurants(eq("Pune"), eq(null), eq(null), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new RestaurantPageResponse(List.of(summary), 0, 20, 1));

        publicApi.perform(get("/api/v1/restaurants").param("city", "Pune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Misal House"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMenu_returnsMenuBundle() throws Exception {
        RestaurantMenuResponse menu = new RestaurantMenuResponse(
                RESTAURANT_ID,
                "Misal House",
                true,
                25,
                List.of(new MenuItemResponse(UUID.randomUUID(), "Kolhapuri Misal", "Main", BigDecimal.valueOf(120), true)),
                Instant.parse("2026-08-21T18:00:00Z"));
        when(restaurantService.getMenu(RESTAURANT_ID)).thenReturn(menu);

        publicApi.perform(get("/api/v1/restaurants/{id}/menu", RESTAURANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Misal House"))
                .andExpect(jsonPath("$.menuItems[0].name").value("Kolhapuri Misal"));
    }

    @Test
    void adminCreate_requiresApiKey() throws Exception {
        adminApi.perform(post("/api/v1/admin/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "address": "Addr",
                                  "city": "Pune",
                                  "cuisines": ["Biryani"]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
