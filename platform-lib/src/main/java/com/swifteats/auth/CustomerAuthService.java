package com.swifteats.auth;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.auth.dto.CustomerAuthResponse;
import com.swifteats.auth.dto.CustomerProfileResponse;
import com.swifteats.auth.dto.LoginCustomerRequest;
import com.swifteats.auth.dto.RegisterCustomerRequest;
import com.swifteats.auth.dto.UpdateCustomerProfileRequest;
import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.common.exception.ConflictException;
import com.swifteats.common.exception.InvalidCredentialsException;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.entity.Customer;
import com.swifteats.order.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@ServiceScope(ServiceName.BACKEND)
public class CustomerAuthService {

    private static final String DEFAULT_STATE = "Maharashtra";

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAuthService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerAuthResponse register(RegisterCustomerRequest request) {
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();

        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (customerRepository.existsByPhone(phone)) {
            throw new ConflictException("An account with this phone number already exists");
        }

        Customer customer = new Customer();
        customer.setName(request.name().trim());
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setPasswordHash(passwordEncoder.encode(request.password()));
        customer.setAddressLine1(request.addressLine1().trim());
        customer.setAddressLine2(trimToNull(request.addressLine2()));
        customer.setCity(request.city().trim());
        customer.setState(StringUtils.hasText(request.state()) ? request.state().trim() : DEFAULT_STATE);
        customer.setPincode(trimToNull(request.pincode()));
        customer.setApiToken(ApiTokenGenerator.newToken());

        Customer saved = customerRepository.save(customer);
        return toAuthResponse(saved);
    }

    @Transactional
    public CustomerAuthResponse login(LoginCustomerRequest request) {
        Customer customer = findByLoginId(request.loginId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email/phone or password"));

        if (customer.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email/phone or password");
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        customer.setApiToken(ApiTokenGenerator.newToken());
        Customer saved = customerRepository.save(customer);
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(UUID customerId) {
        return CustomerProfileMapper.toResponse(requireCustomer(customerId));
    }

    @Transactional
    public CustomerProfileResponse updateProfile(UUID customerId, UpdateCustomerProfileRequest request) {
        Customer customer = requireCustomer(customerId);
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();

        if (!email.equalsIgnoreCase(customer.getEmail()) && customerRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (!phone.equals(customer.getPhone()) && customerRepository.existsByPhone(phone)) {
            throw new ConflictException("An account with this phone number already exists");
        }

        customer.setName(request.name().trim());
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddressLine1(request.addressLine1().trim());
        customer.setAddressLine2(trimToNull(request.addressLine2()));
        customer.setCity(request.city().trim());
        customer.setState(StringUtils.hasText(request.state()) ? request.state().trim() : DEFAULT_STATE);
        customer.setPincode(trimToNull(request.pincode()));

        return CustomerProfileMapper.toResponse(customerRepository.save(customer));
    }

    private Customer requireCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private java.util.Optional<Customer> findByLoginId(String loginId) {
        String trimmed = loginId.trim();
        if (trimmed.contains("@")) {
            return customerRepository.findByEmailIgnoreCase(normalizeEmail(trimmed));
        }
        return customerRepository.findByPhone(trimmed);
    }

    private static CustomerAuthResponse toAuthResponse(Customer customer) {
        CustomerProfileResponse profile = CustomerProfileMapper.toResponse(customer);
        return new CustomerAuthResponse(profile, customer.getId().toString(), customer.getApiToken());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
