package com.swifteats.refund.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ServiceScope(ServiceName.REFUND)
public class RefundWorker {

    private static final Logger log = LoggerFactory.getLogger(RefundWorker.class);

    private final RefundService refundService;

    public RefundWorker(RefundService refundService) {
        this.refundService = refundService;
    }

    @Async
    public void enqueue(UUID refundId) {
        try {
            refundService.process(refundId);
        } catch (Exception ex) {
            log.error("Refund worker failed for {}", refundId, ex);
        }
    }
}
