package com.swifteats.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginCustomerRequest(
        @NotBlank @Size(max = 255) String loginId,
        @NotBlank @Size(min = 8, max = 72) String password) {}
