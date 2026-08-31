package com.swifteats.order.service;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.order.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    private final OrderStateMachine stateMachine = new OrderStateMachine();

    @Test
    void allowsPendingPaymentToConfirmed() {
        assertThatCode(() -> stateMachine.validate(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDeliveredToPreparing() {
        assertThatThrownBy(() -> stateMachine.validate(OrderStatus.DELIVERED, OrderStatus.PREPARING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void allowsPreparingToOutForDelivery() {
        assertThatCode(() -> stateMachine.validate(OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDeliveredToReturnedForRefund() {
        assertThatCode(() -> stateMachine.validate(OrderStatus.DELIVERED, OrderStatus.RETURNED))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsCancelledToReturnedForRefund() {
        assertThatCode(() -> stateMachine.validate(OrderStatus.CANCELLED, OrderStatus.RETURNED))
                .doesNotThrowAnyException();
    }
}
