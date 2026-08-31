package com.swifteats.refund.controller;

import com.swifteats.common.domain.RefundStatus;
import com.swifteats.common.security.RequestAuthAttributes;
import com.swifteats.refund.dto.RefundAcceptedResponse;
import com.swifteats.refund.dto.RefundResponse;
import com.swifteats.refund.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID REFUND_ID = UUID.randomUUID();

    @Mock
    private RefundService refundService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RefundController(refundService)).build();
    }

    @Test
    void initiateRefund_returnsAccepted() throws Exception {
        when(refundService.initiate(eq(CUSTOMER_ID), eq("idem-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new RefundAcceptedResponse(REFUND_ID, "PENDING"));

        mockMvc.perform(post("/api/v1/refunds")
                        .requestAttr(RequestAuthAttributes.CUSTOMER_ID, CUSTOMER_ID)
                        .header(RefundController.IDEMPOTENCY_HEADER, "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","reason":"Late delivery"}
                                """.formatted(ORDER_ID)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.refundId").value(REFUND_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void listRefunds_returnsCustomerRefunds() throws Exception {
        when(refundService.listCustomerRefunds(CUSTOMER_ID)).thenReturn(List.of(
                new RefundResponse(REFUND_ID, ORDER_ID, CUSTOMER_ID, BigDecimal.TEN, RefundStatus.INITIATED,
                        "Late", null, Instant.now(), Instant.now(), null)));

        mockMvc.perform(get("/api/v1/refunds")
                        .requestAttr(RequestAuthAttributes.CUSTOMER_ID, CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(REFUND_ID.toString()));

        verify(refundService).listCustomerRefunds(CUSTOMER_ID);
    }

    @Test
    void getRefund_returnsSingleRefund() throws Exception {
        when(refundService.getRefund(REFUND_ID, CUSTOMER_ID))
                .thenReturn(new RefundResponse(REFUND_ID, ORDER_ID, CUSTOMER_ID, BigDecimal.TEN,
                        RefundStatus.SUCCESSFUL, "Late", null, Instant.now(), Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/refunds/{refundId}", REFUND_ID)
                        .requestAttr(RequestAuthAttributes.CUSTOMER_ID, CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));
    }
}
