package com.swifteats.auth;

import com.swifteats.auth.dto.AdminCustomerDetailResponse;
import com.swifteats.auth.dto.AdminCustomerSummaryResponse;
import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.entity.Customer;
import com.swifteats.order.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCustomerServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final Instant NOW = Instant.parse("2025-08-15T12:00:00Z");

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AdminCustomerService adminCustomerService;

    @Test
    void listForAdmin_returnsAllCustomersWhenNoFilter() {
        Customer customer = sampleCustomer(CustomerStatus.ACTIVE);
        when(customerRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(customer));

        List<AdminCustomerSummaryResponse> result = adminCustomerService.listForAdmin(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(CUSTOMER_ID);
        assertThat(result.get(0).name()).isEqualTo("Demo Customer");
        assertThat(result.get(0).email()).isEqualTo("demo@swifteats.local");
        assertThat(result.get(0).status()).isEqualTo(CustomerStatus.ACTIVE);
        verify(customerRepository, never()).findByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void listForAdmin_filtersByStatus() {
        Customer customer = sampleCustomer(CustomerStatus.SUSPENDED);
        when(customerRepository.findByStatusOrderByCreatedAtDesc(CustomerStatus.SUSPENDED))
                .thenReturn(List.of(customer));

        List<AdminCustomerSummaryResponse> result = adminCustomerService.listForAdmin("suspended");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(CustomerStatus.SUSPENDED);
        verify(customerRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void listForAdmin_rejectsInvalidStatusFilter() {
        assertThatThrownBy(() -> adminCustomerService.listForAdmin("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status filter");
    }

    @Test
    void getForAdmin_returnsCustomerDetail() {
        Customer customer = sampleCustomer(CustomerStatus.ACTIVE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        AdminCustomerDetailResponse result = adminCustomerService.getForAdmin(CUSTOMER_ID);

        assertThat(result.id()).isEqualTo(CUSTOMER_ID);
        assertThat(result.name()).isEqualTo("Demo Customer");
        assertThat(result.email()).isEqualTo("demo@swifteats.local");
        assertThat(result.phone()).isEqualTo("9876543210");
        assertThat(result.addressLine1()).isEqualTo("42 FC Road");
        assertThat(result.city()).isEqualTo("Pune");
        assertThat(result.state()).isEqualTo("Maharashtra");
        assertThat(result.pincode()).isEqualTo("411004");
        assertThat(result.status()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void getForAdmin_throwsWhenCustomerNotFound() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCustomerService.getForAdmin(CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void updateStatus_changesStatusAndPersists() {
        Customer customer = sampleCustomer(CustomerStatus.ACTIVE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCustomerDetailResponse result = adminCustomerService.updateStatus(
                CUSTOMER_ID, CustomerStatus.SUSPENDED);

        assertThat(result.status()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        verify(customerRepository).save(customer);
    }

    @Test
    void updateStatus_clearsApiTokenWhenSuspended() {
        Customer customer = sampleCustomer(CustomerStatus.ACTIVE);
        customer.setApiToken("demo-customer-token");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenAnswer(invocation -> invocation.getArgument(0));

        adminCustomerService.updateStatus(CUSTOMER_ID, CustomerStatus.SUSPENDED);

        assertThat(customer.getApiToken()).isNull();
    }

    @Test
    void updateStatus_isIdempotentWhenStatusUnchanged() {
        Customer customer = sampleCustomer(CustomerStatus.ACTIVE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        AdminCustomerDetailResponse result = adminCustomerService.updateStatus(
                CUSTOMER_ID, CustomerStatus.ACTIVE);

        assertThat(result.status()).isEqualTo(CustomerStatus.ACTIVE);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateStatus_throwsWhenCustomerNotFound() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminCustomerService.updateStatus(CUSTOMER_ID, CustomerStatus.SUSPENDED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    private static Customer sampleCustomer(CustomerStatus status) {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setName("Demo Customer");
        customer.setEmail("demo@swifteats.local");
        customer.setPhone("9876543210");
        customer.setAddressLine1("42 FC Road");
        customer.setCity("Pune");
        customer.setState("Maharashtra");
        customer.setPincode("411004");
        customer.setStatus(status);
        customer.setCreatedAt(NOW);
        customer.setUpdatedAt(NOW);
        return customer;
    }
}
