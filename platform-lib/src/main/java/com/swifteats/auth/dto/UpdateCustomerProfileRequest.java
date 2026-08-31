package com.swifteats.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
                @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a 10-digit Indian mobile number")
                String phone,
        @NotBlank @Size(max = 500) String addressLine1,
        @Size(max = 500) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 20) String pincode) {}
