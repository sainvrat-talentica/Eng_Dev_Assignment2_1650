package com.swifteats.auth.dto;

import com.swifteats.common.domain.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCustomerStatusRequest(@NotNull CustomerStatus status) {}
