package com.swifteats.auth;

import com.swifteats.auth.dto.AdminCustomerDetailResponse;
import com.swifteats.auth.dto.AdminCustomerSummaryResponse;
import com.swifteats.common.domain.CustomerStatus;
import com.swifteats.common.security.AdminApiKeyFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCustomerControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final String ADMIN_API_KEY = "dev-admin-key";

    @Mock
    private AdminCustomerService adminCustomerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCustomerController(adminCustomerService))
                .addFilters(new AdminApiKeyFilter(ADMIN_API_KEY))
                .build();
    }

    @Test
    void listCustomers_returnsSummaries() throws Exception {
        when(adminCustomerService.listForAdmin("ACTIVE")).thenReturn(List.of(
                new AdminCustomerSummaryResponse(
                        CUSTOMER_ID, "Ravi", "ravi@example.com", "9876543210", "Pune",
                        CustomerStatus.ACTIVE, Instant.now(), Instant.now())));

        mockMvc.perform(get("/api/v1/admin/customers")
                        .param("status", "ACTIVE")
                        .header("X-Admin-Api-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ravi"));
    }

    @Test
    void getCustomer_returnsDetail() throws Exception {
        when(adminCustomerService.getForAdmin(CUSTOMER_ID)).thenReturn(sampleDetail());

        mockMvc.perform(get("/api/v1/admin/customers/{id}", CUSTOMER_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ravi@example.com"));
    }

    @Test
    void updateStatus_delegatesToService() throws Exception {
        when(adminCustomerService.updateStatus(CUSTOMER_ID, CustomerStatus.SUSPENDED)).thenReturn(sampleDetail());

        mockMvc.perform(patch("/api/v1/admin/customers/{id}/status", CUSTOMER_ID)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDED"}
                                """))
                .andExpect(status().isOk());

        verify(adminCustomerService).updateStatus(CUSTOMER_ID, CustomerStatus.SUSPENDED);
    }

    private static AdminCustomerDetailResponse sampleDetail() {
        return new AdminCustomerDetailResponse(
                CUSTOMER_ID, "Ravi", "ravi@example.com", "9876543210",
                "FC Road", null, "Pune", "MH", "411001",
                CustomerStatus.ACTIVE, Instant.now(), Instant.now());
    }
}
