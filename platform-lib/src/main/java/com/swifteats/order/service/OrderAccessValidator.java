package com.swifteats.order.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.exception.AccessDeniedException;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.entity.Order;
import com.swifteats.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@ServiceScope({ServiceName.ORDER, ServiceName.BACKEND})
public class OrderAccessValidator {

    private final OrderRepository orderRepository;

    public OrderAccessValidator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Order requireOwnedOrder(UUID orderId, UUID customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Order access denied");
        }
        return order;
    }
}
