package com.swifteats.tracking.service;

import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.entity.Order;
import com.swifteats.order.service.OrderAccessValidator;
import com.swifteats.tracking.dto.DriverLocationSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    @Mock
    private OrderAccessValidator orderAccessValidator;
    @Mock
    private DriverLocationCacheService locationCacheService;

    @InjectMocks
    private TrackingService trackingService;

    @Test
    void getSnapshot_returnsCachedLocation() {
        Order order = new Order();
        order.setCustomerId(CUSTOMER_ID);
        order.setDriverId(DRIVER_ID);
        DriverLocationSnapshot snapshot = new DriverLocationSnapshot(
                DRIVER_ID, null, BigDecimal.ONE, BigDecimal.TEN, null, Instant.now());

        when(orderAccessValidator.requireOwnedOrder(ORDER_ID, CUSTOMER_ID)).thenReturn(order);
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.of(snapshot));

        DriverLocationSnapshot result = trackingService.getSnapshot(ORDER_ID, CUSTOMER_ID);

        assertThat(result.orderId()).isEqualTo(ORDER_ID);
        assertThat(result.driverId()).isEqualTo(DRIVER_ID);
    }

    @Test
    void getSnapshot_throwsWhenNoLocation() {
        Order order = new Order();
        order.setCustomerId(CUSTOMER_ID);
        order.setDriverId(DRIVER_ID);
        when(orderAccessValidator.requireOwnedOrder(eq(ORDER_ID), eq(CUSTOMER_ID))).thenReturn(order);
        when(locationCacheService.getLatestLocation(DRIVER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.getSnapshot(ORDER_ID, CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
