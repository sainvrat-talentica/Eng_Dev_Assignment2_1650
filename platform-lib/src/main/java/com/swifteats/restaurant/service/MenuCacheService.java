package com.swifteats.restaurant.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifteats.restaurant.config.RestaurantProperties;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@ServiceScope({ServiceName.ENTITIES, ServiceName.ORDER, ServiceName.BACKEND})
public class MenuCacheService {

    private static final Logger log = LoggerFactory.getLogger(MenuCacheService.class);
    private static final String MENU_KEY_PREFIX = "restaurant:";
    private static final String MENU_KEY_SUFFIX = ":menu";
    private static final String LIST_KEY_PREFIX = "restaurants:list:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestaurantProperties properties;

    public MenuCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RestaurantProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public RestaurantMenuResponse getMenu(UUID restaurantId) {
        if (!isCacheAvailable()) {
            return null;
        }
        try {
            String cached = redisTemplate.opsForValue().get(menuKey(restaurantId));
            if (cached == null) {
                return null;
            }
            return objectMapper.readValue(cached, RestaurantMenuResponse.class);
        } catch (Exception ex) {
            log.warn("Redis menu cache read failed for {}: {}", restaurantId, ex.getMessage());
            return null;
        }
    }

    public void putMenu(UUID restaurantId, RestaurantMenuResponse response) {
        if (!isCacheAvailable()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    menuKey(restaurantId),
                    objectMapper.writeValueAsString(response),
                    properties.getMenuCacheTtl());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize menu cache for {}: {}", restaurantId, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Redis menu cache write failed for {}: {}", restaurantId, ex.getMessage());
        }
    }

    public RestaurantPageResponse getList(String cacheKeyHash) {
        if (!isCacheAvailable()) {
            return null;
        }
        try {
            String cached = redisTemplate.opsForValue().get(LIST_KEY_PREFIX + cacheKeyHash);
            if (cached == null) {
                return null;
            }
            return objectMapper.readValue(cached, RestaurantPageResponse.class);
        } catch (Exception ex) {
            log.warn("Redis list cache read failed: {}", ex.getMessage());
            return null;
        }
    }

    public void putList(String cacheKeyHash, RestaurantPageResponse response) {
        if (!isCacheAvailable()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    LIST_KEY_PREFIX + cacheKeyHash,
                    objectMapper.writeValueAsString(response),
                    properties.getListCacheTtl());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize list cache: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Redis list cache write failed: {}", ex.getMessage());
        }
    }

    public void invalidateMenu(UUID restaurantId) {
        if (!isCacheAvailable()) {
            return;
        }
        try {
            redisTemplate.delete(menuKey(restaurantId));
        } catch (Exception ex) {
            log.warn("Redis menu cache invalidation failed for {}: {}", restaurantId, ex.getMessage());
        }
    }

    public void invalidateListCaches() {
        if (!isCacheAvailable()) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(LIST_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Redis list cache invalidation failed: {}", ex.getMessage());
        }
    }

    public String hashListQuery(
            String city,
            String cuisine,
            Boolean isOpen,
            java.math.BigDecimal minRating,
            String name,
            int page,
            int size) {
        String raw = String.join("|",
                nullSafe(city),
                nullSafe(cuisine),
                isOpen == null ? "" : isOpen.toString(),
                minRating == null ? "" : minRating.toPlainString(),
                nullSafe(name),
                Integer.toString(page),
                Integer.toString(size));
        return sha256(raw);
    }

    private String menuKey(UUID restaurantId) {
        return MENU_KEY_PREFIX + restaurantId + MENU_KEY_SUFFIX;
    }

    private boolean isCacheAvailable() {
        return properties.isCacheEnabled() && redisTemplate != null;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
