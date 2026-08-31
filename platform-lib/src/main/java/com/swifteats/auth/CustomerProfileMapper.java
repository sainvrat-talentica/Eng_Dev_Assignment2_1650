package com.swifteats.auth;

import com.swifteats.auth.dto.CustomerProfileResponse;
import com.swifteats.order.entity.Customer;

public final class CustomerProfileMapper {

    private CustomerProfileMapper() {
    }

    public static CustomerProfileResponse toResponse(Customer customer) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddressLine1(),
                customer.getAddressLine2(),
                customer.getCity(),
                customer.getState(),
                customer.getPincode(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
