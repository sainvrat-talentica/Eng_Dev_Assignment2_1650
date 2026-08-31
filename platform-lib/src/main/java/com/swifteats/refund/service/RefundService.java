package com.swifteats.refund.service;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.RefundStatus;
import com.swifteats.common.exception.AccessDeniedException;
import com.swifteats.common.exception.ConflictException;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.order.client.OrderInternalClient;
import com.swifteats.order.entity.Order;
import com.swifteats.order.repository.OrderRepository;
import com.swifteats.refund.dto.InitiateRefundRequest;
import com.swifteats.refund.dto.RefundAcceptedResponse;
import com.swifteats.refund.dto.RefundResponse;
import com.swifteats.refund.entity.Refund;
import com.swifteats.refund.repository.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@ServiceScope(ServiceName.REFUND)
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);
    private static final Set<OrderStatus> REFUNDABLE = EnumSet.of(
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED,
            OrderStatus.PAYMENT_FAILED,
            OrderStatus.FAILED);

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final RefundProcessingService refundProcessingService;
    private final RefundWorker refundWorker;
    private final OrderInternalClient orderInternalClient;

    public RefundService(
            RefundRepository refundRepository,
            OrderRepository orderRepository,
            RefundProcessingService refundProcessingService,
            @Lazy RefundWorker refundWorker,
            @Qualifier("httpOrderInternalClient") OrderInternalClient orderInternalClient) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.refundProcessingService = refundProcessingService;
        this.refundWorker = refundWorker;
        this.orderInternalClient = orderInternalClient;
    }

    @Transactional
    public RefundAcceptedResponse initiate(UUID customerId, String idempotencyKey, InitiateRefundRequest request) {
        return refundRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> toAcceptedForCustomer(existing, customerId))
                .orElseGet(() -> createRefund(customerId, idempotencyKey, request));
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefundById(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        return toResponse(refund);
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(UUID refundId, UUID customerId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        if (!refund.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Refund not found");
        }
        return toResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> listCustomerRefunds(UUID customerId) {
        return refundRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void process(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        if (refund.getStatus() == RefundStatus.SUCCESSFUL || refund.getStatus() == RefundStatus.FAILED) {
            return;
        }
        Instant now = Instant.now();
        refund.setStatus(RefundStatus.PROCESSING);
        refund.setUpdatedAt(now);
        refundRepository.save(refund);

        var result = refundProcessingService.process(refund);
        if (result.success()) {
            refund.setStatus(RefundStatus.SUCCESSFUL);
            refund.setCompletedAt(now);
            refund.setUpdatedAt(now);
            refundRepository.save(refund);
            orderInternalClient.transition(refund.getOrderId(), OrderStatus.RETURNED, "REFUND_WORKER", refund.getReason());
            log.info("Refund {} completed for order {}", refundId, refund.getOrderId());
        } else {
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(result.message());
            refund.setUpdatedAt(now);
            refundRepository.save(refund);
            log.warn("Refund {} failed for order {}: {}", refundId, refund.getOrderId(), result.message());
        }
    }

    private RefundAcceptedResponse createRefund(UUID customerId, String idempotencyKey, InitiateRefundRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        if (!REFUNDABLE.contains(order.getStatus())) {
            throw new ConflictException("Order is not eligible for refund in status " + order.getStatus());
        }
        boolean inFlight = refundRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .anyMatch(r -> r.getStatus() == RefundStatus.INITIATED || r.getStatus() == RefundStatus.PROCESSING);
        if (inFlight) {
            throw new ConflictException("A refund is already in progress for this order");
        }

        Instant now = Instant.now();
        Refund refund = new Refund();
        refund.setOrderId(order.getId());
        refund.setCustomerId(customerId);
        refund.setAmount(order.getTotalAmount());
        refund.setStatus(RefundStatus.INITIATED);
        refund.setReason(request.reason());
        refund.setIdempotencyKey(idempotencyKey);
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        try {
            refund = refundRepository.save(refund);
        } catch (DataIntegrityViolationException ex) {
            return refundRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> toAcceptedForCustomer(existing, customerId))
                    .orElseThrow(() -> ex);
        }
        refundWorker.enqueue(refund.getId());
        return new RefundAcceptedResponse(refund.getId(), refund.getStatus().name());
    }

    private RefundAcceptedResponse toAcceptedForCustomer(Refund existing, UUID customerId) {
        if (!existing.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Refund access denied");
        }
        return new RefundAcceptedResponse(existing.getId(), existing.getStatus().name());
    }

    private RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getOrderId(),
                refund.getCustomerId(),
                refund.getAmount(),
                refund.getStatus(),
                refund.getReason(),
                refund.getFailureReason(),
                refund.getCreatedAt(),
                refund.getUpdatedAt(),
                refund.getCompletedAt());
    }
}
