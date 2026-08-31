package com.swifteats.order.client;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.order.service.OrderService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ServiceScope({ServiceName.ORDER})
public class LocalOrderInternalClient implements OrderInternalClient {

    private final OrderService orderService;

    public LocalOrderInternalClient(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void transition(UUID orderId, OrderStatus newStatus, String changedBy, String reason) {
        orderService.transition(orderId, newStatus, changedBy, reason);
    }
}
