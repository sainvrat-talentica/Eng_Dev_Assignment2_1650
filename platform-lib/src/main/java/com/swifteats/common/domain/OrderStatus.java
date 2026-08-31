package com.swifteats.common.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    PAYMENT_FAILED,
    DELAYED,
    FAILED,
    CANCELLED,
    PENDING,
    IN_TRANSIT,
    RETURNED
}
