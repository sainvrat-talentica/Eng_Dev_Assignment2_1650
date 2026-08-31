package com.swifteats.auth;

import com.swifteats.auth.dto.AdminCustomerDetailResponse;
import com.swifteats.auth.dto.AdminCustomerSummaryResponse;
import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.order.entity.Customer;
import com.swifteats.order.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@ServiceScope(ServiceName.BACKEND)
public class AdminCustomerService {

    private final CustomerRepository customerRepository;

    public AdminCustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminCustomerSummaryResponse> listForAdmin(String statusFilter) {
        CustomerStatus status = parseStatusFilter(statusFilter);
        List<Customer> customers = status == null
                ? customerRepository.findAllByOrderByCreatedAtDesc()
                : customerRepository.findByStatusOrderByCreatedAtDesc(status);
        return customers.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public AdminCustomerDetailResponse getForAdmin(UUID customerId) {
        return toDetail(requireCustomer(customerId));
    }

    @Transactional
    public AdminCustomerDetailResponse updateStatus(UUID customerId, CustomerStatus status) {
        Customer customer = requireCustomer(customerId);
        if (customer.getStatus() == status) {
            return toDetail(customer);
        }
        customer.setStatus(status);
        if (status == CustomerStatus.SUSPENDED) {
            customer.setApiToken(null);
        }
        return toDetail(customerRepository.save(customer));
    }

    private Customer requireCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private AdminCustomerSummaryResponse toSummary(Customer customer) {
        return new AdminCustomerSummaryResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCity(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    private AdminCustomerDetailResponse toDetail(Customer customer) {
        return new AdminCustomerDetailResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddressLine1(),
                customer.getAddressLine2(),
                customer.getCity(),
                customer.getState(),
                customer.getPincode(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    private static CustomerStatus parseStatusFilter(String statusFilter) {
        if (!StringUtils.hasText(statusFilter)) {
            return null;
        }
        try {
            return CustomerStatus.valueOf(statusFilter.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status filter: " + statusFilter);
        }
    }
}
