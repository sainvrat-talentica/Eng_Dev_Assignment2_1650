package com.swifteats.auth.dto;

import com.swifteats.common.domain.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminCustomerSummaryResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String city,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt) {}
