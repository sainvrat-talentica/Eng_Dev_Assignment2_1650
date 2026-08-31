package com.swifteats.tracking.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.entity.Order;
import com.swifteats.order.service.OrderAccessValidator;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@ServiceScope(ServiceName.BACKEND)
public class TrackingService {

    private final OrderAccessValidator orderAccessValidator;
    private final DriverLocationCacheService locationCacheService;

    public TrackingService(OrderAccessValidator orderAccessValidator, DriverLocationCacheService locationCacheService) {
        this.orderAccessValidator = orderAccessValidator;
        this.locationCacheService = locationCacheService;
    }

    @Transactional(readOnly = true)
    public DriverLocationSnapshot getSnapshot(UUID orderId, UUID customerId) {
        Order order = orderAccessValidator.requireOwnedOrder(orderId, customerId);
        UUID driverId = resolveDriverId(order, orderId);
        return locationCacheService.getLatestLocation(driverId)
                .map(snapshot -> snapshot.withOrderId(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("No live location available for this order"));
    }

    @Transactional(readOnly = true)
    public UUID requireDriverId(UUID orderId, UUID customerId) {
        Order order = orderAccessValidator.requireOwnedOrder(orderId, customerId);
        return resolveDriverId(order, orderId);
    }

    private UUID resolveDriverId(Order order, UUID orderId) {
        if (order.getDriverId() != null) {
            return order.getDriverId();
        }
        return locationCacheService.getCachedOrderDriver(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver not assigned to this order yet. Advance the order to OUT_FOR_DELIVERY first (current status: "
                                + order.getStatus() + ")."));
    }
}
