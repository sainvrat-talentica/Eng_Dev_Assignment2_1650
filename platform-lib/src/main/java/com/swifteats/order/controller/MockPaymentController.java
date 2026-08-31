package com.swifteats.order.controller;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import com.swifteats.order.payment.MockPaymentGateway;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/mock")
@ServiceScope(ServiceName.PAYMENT)
public class MockPaymentController {

    private final MockPaymentGateway paymentGateway;

    public MockPaymentController(MockPaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    @PostMapping("/process")
    public PaymentResult process(@Valid @RequestBody PaymentRequest request) {
        return paymentGateway.charge(request);
    }
}
