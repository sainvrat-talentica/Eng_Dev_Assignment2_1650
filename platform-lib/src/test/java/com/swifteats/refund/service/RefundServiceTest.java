package com.swifteats.refund.service;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.RefundStatus;
import com.swifteats.common.exception.AccessDeniedException;
import com.swifteats.common.exception.ConflictException;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.client.OrderInternalClient;
import com.swifteats.order.entity.Order;
import com.swifteats.order.repository.OrderRepository;
import com.swifteats.refund.dto.InitiateRefundRequest;
import com.swifteats.refund.dto.RefundAcceptedResponse;
import com.swifteats.refund.dto.RefundResponse;
import com.swifteats.refund.entity.Refund;
import com.swifteats.refund.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID REFUND_ID = UUID.randomUUID();

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RefundProcessingService refundProcessingService;
    @Mock
    private RefundWorker refundWorker;
    @Mock
    private OrderInternalClient orderInternalClient;

    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundService(
                refundRepository,
                orderRepository,
                refundProcessingService,
                refundWorker,
                orderInternalClient);
    }

    @Test
    void initiate_createsRefundAndEnqueuesWorker() {
        Order order = eligibleOrder();
        when(refundRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(ORDER_ID)).thenReturn(List.of());
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            r.setId(REFUND_ID);
            return r;
        });

        RefundAcceptedResponse response = refundService.initiate(
                CUSTOMER_ID, "key-1", new InitiateRefundRequest(ORDER_ID, "Late delivery"));

        assertThat(response.refundId()).isEqualTo(REFUND_ID);
        assertThat(response.status()).isEqualTo(RefundStatus.INITIATED.name());
        verify(refundWorker).enqueue(REFUND_ID);
    }

    @Test
    void initiate_returnsExistingForSameCustomerIdempotencyKey() {
        Refund existing = sampleRefund(RefundStatus.INITIATED);
        when(refundRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        RefundAcceptedResponse response = refundService.initiate(
                CUSTOMER_ID, "key-1", new InitiateRefundRequest(ORDER_ID, "reason"));

        assertThat(response.refundId()).isEqualTo(REFUND_ID);
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void initiate_deniesIdempotencyReplayForOtherCustomer() {
        Refund existing = sampleRefund(RefundStatus.INITIATED);
        existing.setCustomerId(UUID.randomUUID());
        when(refundRepository.findByIdempotencyKey("shared-key")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> refundService.initiate(
                        CUSTOMER_ID, "shared-key", new InitiateRefundRequest(ORDER_ID, "reason")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void initiate_rejectsOrderNotFound() {
        when(refundRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.initiate(
                        CUSTOMER_ID, "key-2", new InitiateRefundRequest(ORDER_ID, "reason")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void initiate_rejectsWrongCustomerOrder() {
        Order order = eligibleOrder();
        order.setCustomerId(UUID.randomUUID());
        when(refundRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundService.initiate(
                        CUSTOMER_ID, "key-3", new InitiateRefundRequest(ORDER_ID, "reason")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void initiate_rejectsNonRefundableStatus() {
        Order order = eligibleOrder();
        order.setStatus(OrderStatus.PREPARING);
        when(refundRepository.findByIdempotencyKey("key-4")).thenReturn(Optional.empty());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundService.initiate(
                        CUSTOMER_ID, "key-4", new InitiateRefundRequest(ORDER_ID, "reason")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void initiate_rejectsInFlightRefund() {
        Order order = eligibleOrder();
        Refund inFlight = sampleRefund(RefundStatus.PROCESSING);
        when(refundRepository.findByIdempotencyKey("key-5")).thenReturn(Optional.empty());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(ORDER_ID)).thenReturn(List.of(inFlight));

        assertThatThrownBy(() -> refundService.initiate(
                        CUSTOMER_ID, "key-5", new InitiateRefundRequest(ORDER_ID, "reason")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void initiate_handlesIdempotencyRace() {
        Order order = eligibleOrder();
        Refund existing = sampleRefund(RefundStatus.INITIATED);
        when(refundRepository.findByIdempotencyKey("race-key")).thenReturn(Optional.empty(), Optional.of(existing));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(ORDER_ID)).thenReturn(List.of());
        when(refundRepository.save(any(Refund.class))).thenThrow(new DataIntegrityViolationException("dup"));

        RefundAcceptedResponse response = refundService.initiate(
                CUSTOMER_ID, "race-key", new InitiateRefundRequest(ORDER_ID, "reason"));

        assertThat(response.refundId()).isEqualTo(REFUND_ID);
    }

    @Test
    void getRefundById_returnsResponse() {
        Refund refund = sampleRefund(RefundStatus.SUCCESSFUL);
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

        RefundResponse response = refundService.getRefundById(REFUND_ID);

        assertThat(response.id()).isEqualTo(REFUND_ID);
        assertThat(response.status()).isEqualTo(RefundStatus.SUCCESSFUL);
    }

    @Test
    void getRefund_hidesOtherCustomersRefund() {
        Refund refund = sampleRefund(RefundStatus.INITIATED);
        refund.setCustomerId(UUID.randomUUID());
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

        assertThatThrownBy(() -> refundService.getRefund(REFUND_ID, CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listCustomerRefunds_mapsAll() {
        Refund refund = sampleRefund(RefundStatus.INITIATED);
        when(refundRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID)).thenReturn(List.of(refund));

        List<RefundResponse> responses = refundService.listCustomerRefunds(CUSTOMER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void process_marksSuccessfulAndTransitionsOrder() {
        Refund refund = sampleRefund(RefundStatus.INITIATED);
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));
        when(refundProcessingService.process(refund)).thenReturn(new RefundResult(true, null));

        refundService.process(REFUND_ID);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESSFUL);
        verify(orderInternalClient).transition(eq(ORDER_ID), eq(OrderStatus.RETURNED), eq("REFUND_WORKER"), any());
    }

    @Test
    void process_marksFailedOnGatewayError() {
        Refund refund = sampleRefund(RefundStatus.INITIATED);
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));
        when(refundProcessingService.process(refund))
                .thenReturn(new RefundResult(false, "Gateway timeout"));

        refundService.process(REFUND_ID);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
        assertThat(refund.getFailureReason()).isEqualTo("Gateway timeout");
        verify(orderInternalClient, never()).transition(any(), any(), any(), any());
    }

    @Test
    void process_skipsTerminalRefunds() {
        Refund refund = sampleRefund(RefundStatus.SUCCESSFUL);
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

        refundService.process(REFUND_ID);

        verify(refundProcessingService, never()).process(any());
    }

    private static Order eligibleOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotalAmount(BigDecimal.valueOf(499));
        return order;
    }

    private static Refund sampleRefund(RefundStatus status) {
        Refund refund = new Refund();
        refund.setId(REFUND_ID);
        refund.setOrderId(ORDER_ID);
        refund.setCustomerId(CUSTOMER_ID);
        refund.setAmount(BigDecimal.valueOf(499));
        refund.setStatus(status);
        refund.setReason("Late delivery");
        refund.setCreatedAt(Instant.now());
        refund.setUpdatedAt(Instant.now());
        return refund;
    }
}
