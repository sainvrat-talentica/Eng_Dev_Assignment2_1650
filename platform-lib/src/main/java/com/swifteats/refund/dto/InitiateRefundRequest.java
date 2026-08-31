package com.swifteats.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InitiateRefundRequest(
        @NotNull UUID orderId,
        @Size(max = 500) String reason) {
}
