package com.swifteats.restaurant.service;

import com.swifteats.common.domain.RestaurantStatus;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.restaurant.dto.CreateMenuItemRequest;
import com.swifteats.restaurant.dto.CreateRestaurantRequest;
import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantDetailResponse;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import com.swifteats.restaurant.dto.UpdateMenuItemRequest;
import com.swifteats.restaurant.dto.UpdateRestaurantRequest;
import com.swifteats.restaurant.entity.MenuItem;
import com.swifteats.restaurant.entity.Restaurant;
import com.swifteats.restaurant.mapper.RestaurantMapper;
import com.swifteats.restaurant.repository.CuisineRepository;
import com.swifteats.restaurant.repository.MenuItemRepository;
import com.swifteats.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private CuisineRepository cuisineRepository;
    @Mock
    private RestaurantMapper restaurantMapper;
    @Mock
    private MenuCacheService menuCacheService;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    void getMenu_returnsCachedValueWhenPresent() {
        RestaurantMenuResponse cached = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.now());
        when(menuCacheService.getMenu(RESTAURANT_ID)).thenReturn(cached);

        RestaurantMenuResponse result = restaurantService.getMenu(RESTAURANT_ID);

        assertThat(result).isSameAs(cached);
        verify(restaurantRepository, never()).findActiveWithMenu(any(), any());
    }

    @Test
    void getMenu_loadsFromDatabaseOnCacheMiss() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        RestaurantMenuResponse loaded = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.now());

        when(menuCacheService.getMenu(RESTAURANT_ID)).thenReturn(null);
        when(restaurantRepository.findActiveWithMenu(RESTAURANT_ID, RestaurantStatus.ACTIVE))
                .thenReturn(Optional.of(restaurant));
        when(restaurantMapper.toMenuResponse(eq(restaurant), any(Instant.class))).thenReturn(loaded);

        RestaurantMenuResponse result = restaurantService.getMenu(RESTAURANT_ID);

        assertThat(result).isSameAs(loaded);
        verify(menuCacheService).putMenu(RESTAURANT_ID, loaded);
    }

    @Test
    void getMenu_throwsWhenRestaurantNotActive() {
        when(menuCacheService.getMenu(RESTAURANT_ID)).thenReturn(null);
        when(restaurantRepository.findActiveWithMenu(RESTAURANT_ID, RestaurantStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getMenu(RESTAURANT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveRestaurant_setsActiveAndOpen() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.PENDING);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toDetail(restaurant)).thenReturn(null);

        restaurantService.approveRestaurant(RESTAURANT_ID);

        assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.ACTIVE);
        assertThat(restaurant.isOpen()).isTrue();
        verify(menuCacheService).invalidateMenu(RESTAURANT_ID);
        verify(menuCacheService).invalidateListCaches();
    }

    @Test
    void searchRestaurants_usesCacheWhenAvailable() {
        RestaurantPageResponse cached = new RestaurantPageResponse(List.of(), 0, 20, 0);
        when(menuCacheService.hashListQuery(null, null, null, null, null, 0, 20)).thenReturn("abc");
        when(menuCacheService.getList("abc")).thenReturn(cached);

        RestaurantPageResponse result = restaurantService.searchRestaurants(
                null, null, null, null, null, 0, 20);

        assertThat(result).isSameAs(cached);
        verify(restaurantRepository, never()).searchActive(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createRestaurant_startsAsPending() {
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "New Place", "FC Road", "Pune", List.of("Biryani"), "a@b.com", null, null);
        Restaurant saved = new Restaurant();
        saved.setId(UUID.randomUUID());
        saved.setStatus(RestaurantStatus.PENDING);

        when(cuisineRepository.findByName("Biryani")).thenReturn(Optional.empty());
        when(cuisineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(saved);
        when(restaurantMapper.toDetail(saved)).thenReturn(null);

        restaurantService.createRestaurant(request);

        verify(restaurantRepository).save(any(Restaurant.class));
        verify(menuCacheService).invalidateListCaches();
    }

    @Test
    void searchRestaurants_loadsFromDatabaseOnCacheMiss() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setName("Misal House");
        Page<Restaurant> page = new PageImpl<>(List.of(restaurant), Pageable.ofSize(20), 1);

        when(menuCacheService.hashListQuery("Pune", null, null, null, null, 0, 20)).thenReturn("hash");
        when(menuCacheService.getList("hash")).thenReturn(null);
        when(restaurantRepository.searchActive(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);
        when(restaurantMapper.toSummary(restaurant)).thenReturn(null);

        RestaurantPageResponse result = restaurantService.searchRestaurants("Pune", null, null, null, null, 0, 20);

        assertThat(result.totalElements()).isEqualTo(1);
        verify(menuCacheService).putList(eq("hash"), any());
    }

    @Test
    void requireAcceptingOrders_throwsWhenClosed() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(false);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.requireAcceptingOrders(RESTAURANT_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approveRestaurant_rejectsAlreadyActive() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.approveRestaurant(RESTAURANT_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addMenuItem_persistsAndInvalidatesCache() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        MenuItem saved = new MenuItem();
        saved.setId(UUID.randomUUID());
        saved.setRestaurant(restaurant);
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(saved);
        when(restaurantMapper.toMenuItem(saved)).thenReturn(null);

        restaurantService.addMenuItem(RESTAURANT_ID,
                new com.swifteats.restaurant.dto.CreateMenuItemRequest("Item", "desc", "Main", BigDecimal.TEN, true));

        verify(menuCacheService).invalidateMenu(RESTAURANT_ID);
        verify(menuCacheService).invalidateListCaches();
    }

    @Test
    void listForAdmin_filtersByStatus() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setName("Misal House");
        restaurant.setCity("Pune");
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        when(restaurantRepository.findForAdmin(RestaurantStatus.ACTIVE)).thenReturn(List.of(restaurant));

        assertThat(restaurantService.listForAdmin("ACTIVE")).hasSize(1);
    }

    @Test
    void getRestaurantForAdmin_returnsDetail() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(restaurantMapper.toDetail(restaurant)).thenReturn(null);

        restaurantService.getRestaurantForAdmin(RESTAURANT_ID);

        verify(restaurantMapper).toDetail(restaurant);
    }

    @Test
    void updateRestaurant_rejectsSuspended() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.SUSPENDED);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.updateRestaurant(
                        RESTAURANT_ID,
                        new UpdateRestaurantRequest("X", null, null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void softDeleteRestaurant_suspendsAndCloses() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toDetail(restaurant)).thenReturn(null);

        restaurantService.softDeleteRestaurant(RESTAURANT_ID);

        assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.SUSPENDED);
        assertThat(restaurant.isOpen()).isFalse();
    }

    @Test
    void updateRestaurant_appliesFieldsAndInvalidatesCache() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(cuisineRepository.findByName("Biryani")).thenReturn(Optional.empty());
        when(cuisineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toDetail(restaurant)).thenReturn(null);

        restaurantService.updateRestaurant(RESTAURANT_ID, new UpdateRestaurantRequest(
                "Renamed", "New Addr", "Mumbai", List.of("Biryani"), "x@y.com", null, null, true, 40));

        assertThat(restaurant.getName()).isEqualTo("Renamed");
        assertThat(restaurant.getCity()).isEqualTo("Mumbai");
        assertThat(restaurant.isOpen()).isTrue();
        verify(menuCacheService).invalidateMenu(RESTAURANT_ID);
    }

    @Test
    void updateMenuItem_updatesFieldsWhenOwnedByRestaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setRestaurant(restaurant);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);
        when(restaurantMapper.toMenuItem(item)).thenReturn(null);

        restaurantService.updateMenuItem(RESTAURANT_ID, item.getId(),
                new UpdateMenuItemRequest("New Name", "desc", "Dessert", BigDecimal.valueOf(99), false));

        assertThat(item.getName()).isEqualTo("New Name");
        assertThat(item.getCategory()).isEqualTo("Dessert");
        verify(menuCacheService).invalidateMenu(RESTAURANT_ID);
    }

    @Test
    void updateMenuItem_rejectsWrongRestaurant() {
        Restaurant other = new Restaurant();
        other.setId(UUID.randomUUID());
        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setRestaurant(other);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> restaurantService.updateMenuItem(
                        RESTAURANT_ID, item.getId(), new UpdateMenuItemRequest("X", null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteMenuItem_removesOwnedItem() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setRestaurant(restaurant);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        restaurantService.deleteMenuItem(RESTAURANT_ID, item.getId());

        verify(menuItemRepository).delete(item);
        verify(menuCacheService).invalidateMenu(RESTAURANT_ID);
    }

    @Test
    void softDeleteRestaurant_rejectsAlreadySuspended() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.SUSPENDED);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.softDeleteRestaurant(RESTAURANT_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireAcceptingOrders_returnsRestaurantWhenOpen() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(true);
        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        assertThat(restaurantService.requireAcceptingOrders(RESTAURANT_ID)).isSameAs(restaurant);
    }

    @Test
    void listForAdmin_rejectsInvalidStatusFilter() {
        assertThatThrownBy(() -> restaurantService.listForAdmin("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status filter");
    }

    @Test
    void listForAdmin_withoutFilterListsAll() {
        when(restaurantRepository.findForAdmin(null)).thenReturn(List.of());

        assertThat(restaurantService.listForAdmin(null)).isEmpty();
    }
}
