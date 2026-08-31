package com.swifteats.restaurant.repository;

import com.swifteats.restaurant.entity.KitchenPrepLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KitchenPrepLogRepository extends JpaRepository<KitchenPrepLog, UUID> {

    Optional<KitchenPrepLog> findByOrderId(UUID orderId);
}
