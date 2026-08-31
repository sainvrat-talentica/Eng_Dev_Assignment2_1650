package com.swifteats.restaurant.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.domain.RestaurantStatus;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.restaurant.dto.AdminRestaurantSummaryResponse;
import com.swifteats.restaurant.dto.CreateMenuItemRequest;
import com.swifteats.restaurant.dto.CreateRestaurantRequest;
import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantDetailResponse;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import com.swifteats.restaurant.dto.UpdateMenuItemRequest;
import com.swifteats.restaurant.dto.UpdateRestaurantRequest;
import com.swifteats.restaurant.entity.Cuisine;
import com.swifteats.restaurant.entity.MenuItem;
import com.swifteats.restaurant.entity.Restaurant;
import com.swifteats.restaurant.mapper.RestaurantMapper;
import com.swifteats.restaurant.repository.CuisineRepository;
import com.swifteats.restaurant.repository.MenuItemRepository;
import com.swifteats.restaurant.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@ServiceScope({ServiceName.ENTITIES, ServiceName.ORDER, ServiceName.BACKEND})
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CuisineRepository cuisineRepository;
    private final RestaurantMapper restaurantMapper;
    private final MenuCacheService menuCacheService;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            CuisineRepository cuisineRepository,
            RestaurantMapper restaurantMapper,
            MenuCacheService menuCacheService) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.cuisineRepository = cuisineRepository;
        this.restaurantMapper = restaurantMapper;
        this.menuCacheService = menuCacheService;
    }

    @Transactional(readOnly = true)
    public RestaurantPageResponse searchRestaurants(
            String city,
            String cuisine,
            Boolean isOpen,
            BigDecimal minRating,
            String name,
            int page,
            int size) {
        String cacheHash = menuCacheService.hashListQuery(city, cuisine, isOpen, minRating, name, page, size);
        RestaurantPageResponse cached = menuCacheService.getList(cacheHash);
        if (cached != null) {
            return cached;
        }

        Page<Restaurant> results = restaurantRepository.searchActive(
                RestaurantStatus.ACTIVE,
                blankToNull(city),
                blankToNull(cuisine),
                isOpen,
                minRating,
                toNamePattern(name),
                PageRequest.of(page, size));

        RestaurantPageResponse response = new RestaurantPageResponse(
                results.getContent().stream().map(restaurantMapper::toSummary).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements());

        menuCacheService.putList(cacheHash, response);
        return response;
    }

    @Transactional(readOnly = true)
    public Restaurant requireAcceptingOrders(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (restaurant.getStatus() != RestaurantStatus.ACTIVE || !restaurant.isOpen()) {
            throw new IllegalArgumentException("Restaurant is not accepting orders");
        }
        return restaurant;
    }

    @Transactional(readOnly = true)
    public RestaurantMenuResponse getMenu(UUID restaurantId) {
        RestaurantMenuResponse cached = menuCacheService.getMenu(restaurantId);
        if (cached != null) {
            return cached;
        }

        Restaurant restaurant = restaurantRepository
                .findActiveWithMenu(restaurantId, RestaurantStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found or not active"));

        Instant cachedAt = Instant.now();
        RestaurantMenuResponse response = restaurantMapper.toMenuResponse(restaurant, cachedAt);
        menuCacheService.putMenu(restaurantId, response);
        return response;
    }

    @Transactional
    public RestaurantDetailResponse createRestaurant(CreateRestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.name().trim());
        restaurant.setAddressLine1(request.address().trim());
        restaurant.setCity(request.city().trim());
        restaurant.setContactEmail(request.contactEmail());
        restaurant.setOpeningTime(request.openingTime());
        restaurant.setClosingTime(request.closingTime());
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setOpen(false);
        restaurant.setCuisines(resolveCuisines(request.cuisines()));

        Restaurant saved = restaurantRepository.save(restaurant);
        menuCacheService.invalidateListCaches();
        log.info("Created restaurant {} with status PENDING", saved.getId());
        return restaurantMapper.toDetail(saved);
    }

    @Transactional
    public RestaurantDetailResponse approveRestaurant(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (restaurant.getStatus() == RestaurantStatus.ACTIVE) {
            throw new IllegalArgumentException("Restaurant is already active");
        }

        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        restaurant.setUpdatedAt(Instant.now());
        Restaurant saved = restaurantRepository.save(restaurant);

        invalidateRestaurantCaches(saved.getId());
        log.info("Approved restaurant {}", saved.getId());
        return restaurantMapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminRestaurantSummaryResponse> listForAdmin(String statusFilter) {
        RestaurantStatus status = parseStatusFilter(statusFilter);
        return restaurantRepository.findForAdmin(status).stream()
                .map(r -> new AdminRestaurantSummaryResponse(
                        r.getId(), r.getName(), r.getCity(), r.getStatus(), r.isOpen(),
                        r.getCreatedAt(), r.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantDetailResponse getRestaurantForAdmin(UUID restaurantId) {
        return restaurantMapper.toDetail(requireRestaurant(restaurantId));
    }

    @Transactional
    public RestaurantDetailResponse updateRestaurant(UUID restaurantId, UpdateRestaurantRequest request) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        if (restaurant.getStatus() == RestaurantStatus.SUSPENDED) {
            throw new IllegalArgumentException("Cannot update a suspended restaurant");
        }

        if (StringUtils.hasText(request.name())) {
            restaurant.setName(request.name().trim());
        }
        if (StringUtils.hasText(request.address())) {
            restaurant.setAddressLine1(request.address().trim());
        }
        if (StringUtils.hasText(request.city())) {
            restaurant.setCity(request.city().trim());
        }
        if (request.cuisines() != null && !request.cuisines().isEmpty()) {
            restaurant.setCuisines(resolveCuisines(request.cuisines()));
        }
        if (request.contactEmail() != null) {
            restaurant.setContactEmail(request.contactEmail());
        }
        if (request.openingTime() != null) {
            restaurant.setOpeningTime(request.openingTime());
        }
        if (request.closingTime() != null) {
            restaurant.setClosingTime(request.closingTime());
        }
        if (request.isOpen() != null) {
            restaurant.setOpen(request.isOpen());
        }
        if (request.estimatedWaitMins() != null) {
            restaurant.setEstimatedWaitMins(request.estimatedWaitMins());
        }
        restaurant.setUpdatedAt(Instant.now());

        Restaurant saved = restaurantRepository.save(restaurant);
        invalidateRestaurantCaches(saved.getId());
        log.info("Updated restaurant {}", saved.getId());
        return restaurantMapper.toDetail(saved);
    }

    @Transactional
    public RestaurantDetailResponse softDeleteRestaurant(UUID restaurantId) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        if (restaurant.getStatus() == RestaurantStatus.SUSPENDED) {
            throw new IllegalArgumentException("Restaurant is already suspended");
        }

        restaurant.setStatus(RestaurantStatus.SUSPENDED);
        restaurant.setOpen(false);
        restaurant.setUpdatedAt(Instant.now());
        Restaurant saved = restaurantRepository.save(restaurant);

        invalidateRestaurantCaches(saved.getId());
        log.info("Soft-deleted (suspended) restaurant {}", saved.getId());
        return restaurantMapper.toDetail(saved);
    }

    @Transactional
    public MenuItemResponse addMenuItem(UUID restaurantId, CreateMenuItemRequest request) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setCategory(request.category().trim());
        item.setPrice(request.price());
        item.setAvailable(request.available());

        MenuItem saved = menuItemRepository.save(item);
        invalidateRestaurantCaches(restaurantId);
        return restaurantMapper.toMenuItem(saved);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(UUID restaurantId, UUID itemId, UpdateMenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!item.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Menu item not found for this restaurant");
        }

        if (StringUtils.hasText(request.name())) {
            item.setName(request.name().trim());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (StringUtils.hasText(request.category())) {
            item.setCategory(request.category().trim());
        }
        if (request.price() != null) {
            item.setPrice(request.price());
        }
        if (request.available() != null) {
            item.setAvailable(request.available());
        }
        item.setUpdatedAt(Instant.now());

        MenuItem saved = menuItemRepository.save(item);
        invalidateRestaurantCaches(restaurantId);
        return restaurantMapper.toMenuItem(saved);
    }

    @Transactional
    public void deleteMenuItem(UUID restaurantId, UUID itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!item.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Menu item not found for this restaurant");
        }

        menuItemRepository.delete(item);
        invalidateRestaurantCaches(restaurantId);
    }

    private Restaurant requireRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
    }

    private Set<Cuisine> resolveCuisines(List<String> cuisineNames) {
        Set<Cuisine> cuisines = new HashSet<>();
        for (String rawName : cuisineNames) {
            String name = rawName.trim();
            Cuisine cuisine = cuisineRepository.findByName(name)
                    .orElseGet(() -> {
                        Cuisine created = new Cuisine();
                        created.setName(name);
                        return cuisineRepository.save(created);
                    });
            cuisines.add(cuisine);
        }
        return cuisines;
    }

    private void invalidateRestaurantCaches(UUID restaurantId) {
        menuCacheService.invalidateMenu(restaurantId);
        menuCacheService.invalidateListCaches();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String toNamePattern(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return "%" + name.trim().toLowerCase() + "%";
    }

    private static RestaurantStatus parseStatusFilter(String statusFilter) {
        if (!StringUtils.hasText(statusFilter)) {
            return null;
        }
        try {
            return RestaurantStatus.valueOf(statusFilter.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status filter: " + statusFilter);
        }
    }
}
