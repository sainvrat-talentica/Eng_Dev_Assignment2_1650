package com.swifteats.order.service;

import com.swifteats.common.exception.AccessDeniedException;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.entity.Order;
import com.swifteats.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAccessValidatorTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderAccessValidator validator;

    @Test
    void requireOwnedOrder_returnsOrderForOwner() {
        Order order = new Order();
        order.setCustomerId(OWNER_ID);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        Order result = validator.requireOwnedOrder(ORDER_ID, OWNER_ID);

        assertThat(result).isSameAs(order);
    }

    @Test
    void requireOwnedOrder_throwsWhenOrderMissing() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.requireOwnedOrder(ORDER_ID, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireOwnedOrder_throwsWhenCustomerMismatch() {
        Order order = new Order();
        order.setCustomerId(OWNER_ID);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> validator.requireOwnedOrder(ORDER_ID, OTHER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }
}
