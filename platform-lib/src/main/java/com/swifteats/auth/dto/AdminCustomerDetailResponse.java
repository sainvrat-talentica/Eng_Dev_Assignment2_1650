package com.swifteats.auth.dto;

import com.swifteats.common.domain.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminCustomerDetailResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt) {}
