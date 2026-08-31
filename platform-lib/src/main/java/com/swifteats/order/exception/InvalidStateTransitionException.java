package com.swifteats.order.exception;

import com.swifteats.common.domain.OrderStatus;

public class InvalidStateTransitionException extends IllegalStateException {

    private final OrderStatus from;
    private final OrderStatus to;

    public InvalidStateTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public OrderStatus getFrom() {
        return from;
    }

    public OrderStatus getTo() {
        return to;
    }
}
