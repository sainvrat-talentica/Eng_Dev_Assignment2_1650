package com.swifteats.restaurant.repository;

import com.swifteats.restaurant.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurant_IdAndAvailableTrue(UUID restaurantId);

    List<MenuItem> findByIdInAndRestaurant_Id(Collection<UUID> ids, UUID restaurantId);
}
