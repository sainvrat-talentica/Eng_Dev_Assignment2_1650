package com.swifteats.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull UUID orderId,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
