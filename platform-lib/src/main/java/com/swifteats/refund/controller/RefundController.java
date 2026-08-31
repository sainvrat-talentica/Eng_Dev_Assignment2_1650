package com.swifteats.refund.controller;

import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.common.security.RequestAuthAttributes;
import com.swifteats.refund.dto.InitiateRefundRequest;
import com.swifteats.refund.dto.RefundAcceptedResponse;
import com.swifteats.refund.dto.RefundResponse;
import com.swifteats.refund.service.RefundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refunds")
@ServiceScope(ServiceName.REFUND)
@SecurityRequirements({
        @SecurityRequirement(name = OpenApiConfig.CUSTOMER_ID),
        @SecurityRequirement(name = OpenApiConfig.CUSTOMER_API_KEY)
})
public class RefundController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RefundAcceptedResponse initiateRefund(
            HttpServletRequest httpRequest,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody InitiateRefundRequest request) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return refundService.initiate(customerId, idempotencyKey, request);
    }

    @GetMapping
    public List<RefundResponse> listRefunds(HttpServletRequest httpRequest) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return refundService.listCustomerRefunds(customerId);
    }

    @GetMapping("/{refundId}")
    public RefundResponse getRefund(HttpServletRequest httpRequest, @PathVariable UUID refundId) {
        UUID customerId = RequestAuthAttributes.customerId(httpRequest);
        return refundService.getRefund(refundId, customerId);
    }
}
