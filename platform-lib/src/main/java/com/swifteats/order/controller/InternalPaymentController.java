package com.swifteats.order.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import com.swifteats.order.dto.PaymentProcessMessage;
import com.swifteats.order.payment.PaymentProcessingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/payments")
@ServiceScope(ServiceName.PAYMENT)
public class InternalPaymentController {

    private final PaymentProcessingService paymentProcessingService;

    public InternalPaymentController(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    @PostMapping("/process")
    public void process(@RequestBody PaymentProcessMessage message) {
        paymentProcessingService.process(message);
    }
}
