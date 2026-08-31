package com.swifteats.auth;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.auth.dto.CustomerAuthResponse;
import com.swifteats.auth.dto.CustomerProfileResponse;
import com.swifteats.auth.dto.LoginCustomerRequest;
import com.swifteats.auth.dto.RegisterCustomerRequest;
import com.swifteats.auth.dto.UpdateCustomerProfileRequest;
import com.swifteats.common.security.RequestAuthAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@ServiceScope(ServiceName.BACKEND)
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    public CustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAuthResponse register(@Valid @RequestBody RegisterCustomerRequest request) {
        return customerAuthService.register(request);
    }

    @PostMapping("/login")
    public CustomerAuthResponse login(@Valid @RequestBody LoginCustomerRequest request) {
        return customerAuthService.login(request);
    }

    @GetMapping("/me")
    public CustomerProfileResponse me(HttpServletRequest httpRequest) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return customerAuthService.getProfile(customerId);
    }

    @PatchMapping("/profile")
    public CustomerProfileResponse updateProfile(
            HttpServletRequest httpRequest, @Valid @RequestBody UpdateCustomerProfileRequest request) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return customerAuthService.updateProfile(customerId, request);
    }
}
