package com.swifteats.refund.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.refund.dto.RefundResponse;
import com.swifteats.refund.service.RefundService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/refunds")
@ServiceScope(ServiceName.REFUND)
public class InternalRefundController {

    private final RefundService refundService;

    public InternalRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/{refundId}/process")
    public RefundResponse processRefund(@PathVariable UUID refundId) {
        refundService.process(refundId);
        return refundService.getRefundById(refundId);
    }
}
