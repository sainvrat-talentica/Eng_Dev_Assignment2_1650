package com.swifteats.auth;

import com.swifteats.auth.dto.CustomerAuthResponse;
import com.swifteats.auth.dto.CustomerProfileResponse;
import com.swifteats.common.security.RequestAuthAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerAuthControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Mock
    private CustomerAuthService customerAuthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerAuthController(customerAuthService)).build();
    }

    @Test
    void register_returnsCreated() throws Exception {
        CustomerProfileResponse profile = sampleProfile();
        when(customerAuthService.register(any())).thenReturn(new CustomerAuthResponse(profile, CUSTOMER_ID.toString(), "token"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ravi","email":"ravi@example.com","phone":"9876543210",
                                  "password":"password1","addressLine1":"FC Road","city":"Pune"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiToken").value("token"));
    }

    @Test
    void login_returnsAuthResponse() throws Exception {
        CustomerProfileResponse profile = sampleProfile();
        when(customerAuthService.login(any())).thenReturn(new CustomerAuthResponse(profile, CUSTOMER_ID.toString(), "token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"ravi@example.com","password":"password1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()));
    }

    @Test
    void me_returnsProfile() throws Exception {
        when(customerAuthService.getProfile(CUSTOMER_ID)).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/v1/auth/me")
                        .requestAttr(RequestAuthAttributes.CUSTOMER_ID, CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ravi@example.com"));
    }

    @Test
    void updateProfile_delegatesToService() throws Exception {
        when(customerAuthService.updateProfile(eq(CUSTOMER_ID), any())).thenReturn(sampleProfile());

        mockMvc.perform(patch("/api/v1/auth/profile")
                        .requestAttr(RequestAuthAttributes.CUSTOMER_ID, CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Ravi Updated","email":"ravi@example.com","phone":"9876543210",
                                  "addressLine1":"FC Road","city":"Mumbai"
                                }
                                """))
                .andExpect(status().isOk());

        verify(customerAuthService).updateProfile(eq(CUSTOMER_ID), any());
    }

    private static CustomerProfileResponse sampleProfile() {
        return new CustomerProfileResponse(
                CUSTOMER_ID, "Ravi", "ravi@example.com", "9876543210",
                "FC Road", null, "Pune", "MH", "411001", Instant.now(), Instant.now());
    }
}
