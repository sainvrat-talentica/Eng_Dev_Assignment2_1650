package com.swifteats.order.dto;

public record PaymentResult(
        boolean success,
        String transactionId,
        String message) {
}
