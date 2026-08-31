package com.swifteats.order.repository;

import com.swifteats.order.entity.OrderStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStateHistoryRepository extends JpaRepository<OrderStateHistory, UUID> {

    List<OrderStateHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);
}
