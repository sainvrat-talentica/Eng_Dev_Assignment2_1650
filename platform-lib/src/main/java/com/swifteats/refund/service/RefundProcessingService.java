package com.swifteats.refund.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.refund.config.RefundProperties;
import com.swifteats.refund.entity.Refund;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@ServiceScope(ServiceName.REFUND)
public class RefundProcessingService {

    private final RefundProperties refundProperties;

    public RefundProcessingService(RefundProperties refundProperties) {
        this.refundProperties = refundProperties;
    }

    public RefundResult process(Refund refund) {
        sleep(refundProperties.getMockDelayMs());
        if (ThreadLocalRandom.current().nextDouble() < refundProperties.getMockFailureRate()) {
            return new RefundResult(false, "Mock refund gateway declined the request");
        }
        return new RefundResult(true, "Refund processed");
    }

    private void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
