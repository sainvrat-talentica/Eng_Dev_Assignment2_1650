package com.swifteats.auth;

import com.swifteats.auth.dto.LoginCustomerRequest;
import com.swifteats.auth.dto.RegisterCustomerRequest;
import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.common.exception.ConflictException;
import com.swifteats.common.exception.InvalidCredentialsException;
import com.swifteats.order.entity.Customer;
import com.swifteats.order.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock
    private CustomerRepository customerRepository;

    private PasswordEncoder passwordEncoder;
    private CustomerAuthService customerAuthService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        customerAuthService = new CustomerAuthService(customerRepository, passwordEncoder);
    }

    @Test
    void register_createsCustomerWithHashedPasswordAndToken() {
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "Jane Doe",
                "jane@example.com",
                "9876543210",
                "Secret123",
                "42 FC Road",
                null,
                "Pune",
                "Maharashtra",
                "411004");

        when(customerRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("9876543210")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(CUSTOMER_ID);
            return customer;
        });

        var response = customerAuthService.register(request);

        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(response.apiToken()).isNotBlank();
        assertThat(response.profile().email()).isEqualTo("jane@example.com");

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer saved = captor.getValue();
        assertThat(passwordEncoder.matches("Secret123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getName()).isEqualTo("Jane Doe");
        assertThat(saved.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(saved.getApiToken()).isEqualTo(response.apiToken());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(customerRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerAuthService.register(sampleRegisterRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_issuesFreshTokenForValidCredentials() {
        Customer customer = sampleCustomer();
        customer.setPasswordHash(passwordEncoder.encode("Secret123"));

        when(customerRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = customerAuthService.login(new LoginCustomerRequest("jane@example.com", "Secret123"));

        assertThat(response.apiToken()).isNotBlank();
        assertThat(response.apiToken()).isNotEqualTo("old-token");
    }

    @Test
    void login_rejectsInvalidPassword() {
        Customer customer = sampleCustomer();
        customer.setPasswordHash(passwordEncoder.encode("Secret123"));
        when(customerRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerAuthService.login(new LoginCustomerRequest("jane@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void login_rejectsSuspendedAccount() {
        Customer customer = sampleCustomer();
        customer.setPasswordHash(passwordEncoder.encode("Secret123"));
        customer.setStatus(CustomerStatus.SUSPENDED);
        when(customerRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerAuthService.login(new LoginCustomerRequest("jane@example.com", "Secret123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_acceptsPhoneLoginId() {
        Customer customer = sampleCustomer();
        customer.setPasswordHash(passwordEncoder.encode("Secret123"));
        when(customerRepository.findByPhone("9876543210")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = customerAuthService.login(new LoginCustomerRequest("9876543210", "Secret123"));

        assertThat(response.apiToken()).isNotBlank();
    }

    @Test
    void getProfile_returnsMappedProfile() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        var profile = customerAuthService.getProfile(CUSTOMER_ID);

        assertThat(profile.email()).isEqualTo("jane@example.com");
    }

    @Test
    void updateProfile_persistsChanges() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        var profile = customerAuthService.updateProfile(CUSTOMER_ID, new com.swifteats.auth.dto.UpdateCustomerProfileRequest(
                "Jane Updated",
                "jane@example.com",
                "9876543210",
                "42 FC Road",
                null,
                "Pune",
                "Maharashtra",
                "411004"));

        assertThat(profile.name()).isEqualTo("Jane Updated");
    }

    private static RegisterCustomerRequest sampleRegisterRequest() {
        return new RegisterCustomerRequest(
                "Jane Doe",
                "jane@example.com",
                "9876543210",
                "Secret123",
                "42 FC Road",
                null,
                "Pune",
                "Maharashtra",
                "411004");
    }

    private static Customer sampleCustomer() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setName("Jane Doe");
        customer.setEmail("jane@example.com");
        customer.setPhone("9876543210");
        customer.setAddressLine1("42 FC Road");
        customer.setCity("Pune");
        customer.setState("Maharashtra");
        customer.setApiToken("old-token");
        return customer;
    }
}
