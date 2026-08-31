package com.swifteats.restaurant.controller;

import com.swifteats.common.domain.RestaurantStatus;
import com.swifteats.common.security.AdminApiKeyFilter;
import com.swifteats.restaurant.dto.AdminRestaurantSummaryResponse;
import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantDetailResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminRestaurantControllerTest {

    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final String ADMIN_API_KEY = "dev-admin-key";

    @Mock
    private RestaurantService restaurantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRestaurantController(restaurantService))
                .addFilters(new AdminApiKeyFilter(ADMIN_API_KEY))
                .build();
    }

    @Test
    void listRestaurants_returnsSummaries() throws Exception {
        when(restaurantService.listForAdmin("ACTIVE")).thenReturn(List.of(
                new AdminRestaurantSummaryResponse(
                        RESTAURANT_ID, "Misal House", "Pune", RestaurantStatus.ACTIVE, true,
                        Instant.now(), Instant.now())));

        mockMvc.perform(get("/api/v1/admin/restaurants")
                        .param("status", "ACTIVE")
                        .header("X-Admin-Api-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Misal House"));
    }

    private static RestaurantDetailResponse sampleDetail(RestaurantStatus status, boolean open) {
        return new RestaurantDetailResponse(
                RESTAURANT_ID, "Misal House", "Addr", "Pune", "MH", BigDecimal.valueOf(4.5), open, status,
                List.of("Maharashtrian"), "a@b.com", null, null, 25, Instant.now(), Instant.now());
    }

    @Test
    void getRestaurant_returnsDetail() throws Exception {
        when(restaurantService.getRestaurantForAdmin(RESTAURANT_ID))
                .thenReturn(sampleDetail(RestaurantStatus.ACTIVE, true));

        mockMvc.perform(get("/api/v1/admin/restaurants/{id}", RESTAURANT_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Misal House"));
    }

    @Test
    void createRestaurant_returnsCreated() throws Exception {
        when(restaurantService.createRestaurant(org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleDetail(RestaurantStatus.PENDING, false));

        mockMvc.perform(post("/api/v1/admin/restaurants")
                        .header("X-Admin-Api-Key", ADMIN_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Place","address":"Addr","city":"Pune","cuisines":["Biryani"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void updateRestaurant_delegatesToService() throws Exception {
        RestaurantDetailResponse updated = new RestaurantDetailResponse(
                RESTAURANT_ID, "Updated", "Addr", "Pune", "MH", BigDecimal.valueOf(4.5), true,
                RestaurantStatus.ACTIVE, List.of("Biryani"), "a@b.com", null, null, 25, Instant.now(), Instant.now());
        when(restaurantService.updateRestaurant(eq(RESTAURANT_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/v1/admin/restaurants/{id}", RESTAURANT_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void approveRestaurant_delegatesToService() throws Exception {
        when(restaurantService.approveRestaurant(RESTAURANT_ID))
                .thenReturn(sampleDetail(RestaurantStatus.ACTIVE, true));

        mockMvc.perform(patch("/api/v1/admin/restaurants/{id}/approve", RESTAURANT_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void addMenuItem_returnsCreated() throws Exception {
        when(restaurantService.addMenuItem(eq(RESTAURANT_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MenuItemResponse(ITEM_ID, "Thali", "Main", BigDecimal.valueOf(150), true));

        mockMvc.perform(post("/api/v1/admin/restaurants/{id}/menu-items", RESTAURANT_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Thali","category":"Main","price":150,"available":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Thali"));
    }

    @Test
    void updateMenuItem_delegatesToService() throws Exception {
        when(restaurantService.updateMenuItem(eq(RESTAURANT_ID), eq(ITEM_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MenuItemResponse(ITEM_ID, "Thali Special", "Main", BigDecimal.valueOf(175), true));

        mockMvc.perform(patch("/api/v1/admin/restaurants/{id}/menu-items/{itemId}", RESTAURANT_ID, ITEM_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Thali Special","price":175}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thali Special"));
    }

    @Test
    void deleteMenuItem_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/restaurants/{id}/menu-items/{itemId}", RESTAURANT_ID, ITEM_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY))
                .andExpect(status().isNoContent());

        verify(restaurantService).deleteMenuItem(RESTAURANT_ID, ITEM_ID);
    }

    @Test
    void softDeleteRestaurant_delegatesToService() throws Exception {
        when(restaurantService.softDeleteRestaurant(RESTAURANT_ID))
                .thenReturn(sampleDetail(RestaurantStatus.SUSPENDED, false));

        mockMvc.perform(delete("/api/v1/admin/restaurants/{id}", RESTAURANT_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }
}
