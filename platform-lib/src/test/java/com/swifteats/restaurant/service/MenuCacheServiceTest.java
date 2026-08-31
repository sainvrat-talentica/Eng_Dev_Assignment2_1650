package com.swifteats.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swifteats.restaurant.config.RestaurantProperties;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuCacheServiceTest {

    private static final UUID RESTAURANT_ID = UUID.randomUUID();

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private MenuCacheService menuCacheService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestaurantProperties properties = new RestaurantProperties();
        properties.setCacheEnabled(true);
        properties.setMenuCacheTtl(Duration.ofMinutes(10));

        menuCacheService = new MenuCacheService(redisTemplate, objectMapper, properties);
    }

    @Test
    void putAndGetMenu_roundTripsJson() throws Exception {
        RestaurantMenuResponse response = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.parse("2026-08-21T18:00:00Z"));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("restaurant:" + RESTAURANT_ID + ":menu"))
                .thenReturn(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(response));

        RestaurantMenuResponse cached = menuCacheService.getMenu(RESTAURANT_ID);

        assertThat(cached.restaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(cached.name()).isEqualTo("Misal House");
    }

    @Test
    void putMenu_writesToRedis() throws Exception {
        RestaurantMenuResponse response = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        menuCacheService.putMenu(RESTAURANT_ID, response);

        verify(valueOperations).set(
                eq("restaurant:" + RESTAURANT_ID + ":menu"),
                anyString(),
                eq(Duration.ofMinutes(10)));
    }

    @Test
    void getMenu_returnsNullWhenCacheDisabled() {
        RestaurantProperties disabled = new RestaurantProperties();
        disabled.setCacheEnabled(false);
        MenuCacheService disabledCache = new MenuCacheService(redisTemplate, new ObjectMapper(), disabled);

        assertThat(disabledCache.getMenu(RESTAURANT_ID)).isNull();
    }

    @Test
    void getMenu_returnsNullWhenRedisMissing() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestaurantProperties properties = new RestaurantProperties();
        properties.setCacheEnabled(true);
        MenuCacheService noRedis = new MenuCacheService(null, objectMapper, properties);

        assertThat(noRedis.getMenu(RESTAURANT_ID)).isNull();
    }

    @Test
    void listCache_roundTripsAndInvalidates() throws Exception {
        RestaurantPageResponse page = new RestaurantPageResponse(List.of(), 0, 20, 0);
        String hash = menuCacheService.hashListQuery("Pune", null, true, BigDecimal.valueOf(4.0), "Misal", 0, 20);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("restaurants:list:" + hash))
                .thenReturn(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(page));
        when(redisTemplate.keys("restaurants:list:*")).thenReturn(Set.of("restaurants:list:" + hash));

        assertThat(menuCacheService.getList(hash).totalElements()).isZero();

        menuCacheService.putList(hash, page);
        menuCacheService.invalidateListCaches();
        menuCacheService.invalidateMenu(RESTAURANT_ID);

        verify(valueOperations).set(eq("restaurants:list:" + hash), anyString(), eq(Duration.ofMinutes(5)));
        verify(redisTemplate).delete(Set.of("restaurants:list:" + hash));
        verify(redisTemplate).delete("restaurant:" + RESTAURANT_ID + ":menu");
    }

    @Test
    void getMenu_returnsNullWhenCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("restaurant:" + RESTAURANT_ID + ":menu")).thenReturn(null);

        assertThat(menuCacheService.getMenu(RESTAURANT_ID)).isNull();
    }

    @Test
    void getMenu_returnsNullOnInvalidJson() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("restaurant:" + RESTAURANT_ID + ":menu")).thenReturn("{not-json");

        assertThat(menuCacheService.getMenu(RESTAURANT_ID)).isNull();
    }

    @Test
    void getList_returnsNullOnInvalidJson() {
        String hash = menuCacheService.hashListQuery("Pune", null, true, null, null, 0, 20);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("restaurants:list:" + hash)).thenReturn("not-json");

        assertThat(menuCacheService.getList(hash)).isNull();
    }

    @Test
    void putMenu_skipsWhenCacheDisabled() {
        RestaurantProperties disabled = new RestaurantProperties();
        disabled.setCacheEnabled(false);
        MenuCacheService disabledCache = new MenuCacheService(redisTemplate, new ObjectMapper(), disabled);
        RestaurantMenuResponse response = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.now());

        disabledCache.putMenu(RESTAURANT_ID, response);

        verify(valueOperations, never()).set(anyString(), anyString(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void hashListQuery_isDeterministic() {
        String a = menuCacheService.hashListQuery("Pune", "Biryani", true, null, null, 0, 20);
        String b = menuCacheService.hashListQuery("Pune", "Biryani", true, null, null, 0, 20);
        assertThat(a).isEqualTo(b);
    }
}
