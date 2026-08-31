package com.swifteats.restaurant.mapper;

import com.swifteats.common.domain.RestaurantStatus;
import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantDetailResponse;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantSummaryResponse;
import com.swifteats.restaurant.entity.Cuisine;
import com.swifteats.restaurant.entity.MenuItem;
import com.swifteats.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RestaurantMapperTest {

    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID MENU_ITEM_A_ID = UUID.fromString("33333333-3333-3333-3333-333333333301");
    private static final UUID MENU_ITEM_B_ID = UUID.fromString("33333333-3333-3333-3333-333333333302");

    private RestaurantMapper mapper;
    private Instant now;

    @BeforeEach
    void setUp() {
        mapper = new RestaurantMapper();
        now = Instant.parse("2025-08-15T12:00:00Z");
    }

    @Test
    void toSummary_mapsRestaurantWithSortedCuisines() {
        Restaurant restaurant = sampleRestaurant();

        RestaurantSummaryResponse response = mapper.toSummary(restaurant);

        assertThat(response.id()).isEqualTo(RESTAURANT_ID);
        assertThat(response.name()).isEqualTo("Misal House");
        assertThat(response.city()).isEqualTo("Pune");
        assertThat(response.rating()).isEqualByComparingTo("4.5");
        assertThat(response.isOpen()).isTrue();
        assertThat(response.cuisines()).containsExactly("Maharashtrian", "Street Food");
        assertThat(response.estimatedWaitMins()).isEqualTo(25);
    }

    @Test
    void toDetail_mapsFullRestaurantProfile() {
        Restaurant restaurant = sampleRestaurant();

        RestaurantDetailResponse response = mapper.toDetail(restaurant);

        assertThat(response.id()).isEqualTo(RESTAURANT_ID);
        assertThat(response.name()).isEqualTo("Misal House");
        assertThat(response.addressLine1()).isEqualTo("FC Road");
        assertThat(response.city()).isEqualTo("Pune");
        assertThat(response.state()).isEqualTo("Maharashtra");
        assertThat(response.rating()).isEqualByComparingTo("4.5");
        assertThat(response.isOpen()).isTrue();
        assertThat(response.status()).isEqualTo(RestaurantStatus.ACTIVE);
        assertThat(response.cuisines()).containsExactly("Maharashtrian", "Street Food");
        assertThat(response.contactEmail()).isEqualTo("misal@example.com");
        assertThat(response.openingTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.closingTime()).isEqualTo(LocalTime.of(22, 0));
        assertThat(response.estimatedWaitMins()).isEqualTo(25);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void toMenuResponse_sortsItemsByCategoryThenName() {
        Restaurant restaurant = sampleRestaurant();
        restaurant.setMenuItems(List.of(
                menuItem(MENU_ITEM_B_ID, "Poha", "Breakfast", "60.00", true),
                menuItem(MENU_ITEM_A_ID, "Kolhapuri Misal", "Main", "120.00", true)));

        RestaurantMenuResponse response = mapper.toMenuResponse(restaurant, now);

        assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(response.name()).isEqualTo("Misal House");
        assertThat(response.isOpen()).isTrue();
        assertThat(response.estimatedWaitMins()).isEqualTo(25);
        assertThat(response.cachedAt()).isEqualTo(now);
        assertThat(response.menuItems()).extracting(MenuItemResponse::name)
                .containsExactly("Poha", "Kolhapuri Misal");
    }

    @Test
    void toMenuItem_mapsMenuItemFields() {
        MenuItem item = menuItem(MENU_ITEM_A_ID, "Kolhapuri Misal", "Main", "120.00", true);

        MenuItemResponse response = mapper.toMenuItem(item);

        assertThat(response.id()).isEqualTo(MENU_ITEM_A_ID);
        assertThat(response.name()).isEqualTo("Kolhapuri Misal");
        assertThat(response.category()).isEqualTo("Main");
        assertThat(response.price()).isEqualByComparingTo("120.00");
        assertThat(response.available()).isTrue();
    }

    private Restaurant sampleRestaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setName("Misal House");
        restaurant.setAddressLine1("FC Road");
        restaurant.setCity("Pune");
        restaurant.setState("Maharashtra");
        restaurant.setRating(new BigDecimal("4.5"));
        restaurant.setOpen(true);
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setContactEmail("misal@example.com");
        restaurant.setOpeningTime(LocalTime.of(8, 0));
        restaurant.setClosingTime(LocalTime.of(22, 0));
        restaurant.setEstimatedWaitMins(25);
        restaurant.setCreatedAt(now);
        restaurant.setUpdatedAt(now);
        restaurant.setCuisines(cuisines("Street Food", "Maharashtrian"));
        return restaurant;
    }

    private static Set<Cuisine> cuisines(String... names) {
        Set<Cuisine> cuisines = new LinkedHashSet<>();
        for (String name : names) {
            Cuisine cuisine = new Cuisine();
            cuisine.setName(name);
            cuisines.add(cuisine);
        }
        return cuisines;
    }

    private static MenuItem menuItem(UUID id, String name, String category, String price, boolean available) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setCategory(category);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(available);
        return item;
    }
}
