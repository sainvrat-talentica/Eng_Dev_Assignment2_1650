package com.swifteats.order.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.order.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
@ServiceScope(ServiceName.ORDER)
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.PENDING_PAYMENT,
                EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.CONFIRMED,
                EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PREPARING,
                EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELAYED, OrderStatus.FAILED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.DELAYED,
                EnumSet.of(OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.FAILED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.OUT_FOR_DELIVERY,
                EnumSet.of(OrderStatus.DELIVERED, OrderStatus.FAILED));
        TRANSITIONS.put(OrderStatus.PAYMENT_FAILED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.RETURNED));
        TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED));
        TRANSITIONS.put(OrderStatus.FAILED, EnumSet.of(OrderStatus.RETURNED));
        TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.of(OrderStatus.RETURNED));
        TRANSITIONS.put(OrderStatus.RETURNED, EnumSet.noneOf(OrderStatus.class));
    }

    public void validate(OrderStatus from, OrderStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
