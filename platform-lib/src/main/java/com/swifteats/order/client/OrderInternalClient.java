package com.swifteats.order.client;

import com.swifteats.common.domain.OrderStatus;

import java.util.UUID;

public interface OrderInternalClient {

    void transition(UUID orderId, OrderStatus newStatus, String changedBy, String reason);
}
