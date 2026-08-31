package com.swifteats.order.service;

import com.swifteats.common.exception.AccessDeniedException;
import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.PaymentStatus;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.payment.MockPaymentGateway;
import com.swifteats.order.dto.CreateOrderRequest;
import com.swifteats.order.dto.OrderAcceptedResponse;
import com.swifteats.order.entity.Customer;
import com.swifteats.order.entity.Order;
import com.swifteats.order.mapper.OrderMapper;
import com.swifteats.order.repository.CustomerRepository;
import com.swifteats.order.repository.OrderItemRepository;
import com.swifteats.order.repository.OrderRepository;
import com.swifteats.order.repository.OrderStateHistoryRepository;
import com.swifteats.restaurant.entity.MenuItem;
import com.swifteats.restaurant.entity.Restaurant;
import com.swifteats.restaurant.repository.KitchenPrepLogRepository;
import com.swifteats.restaurant.repository.MenuItemRepository;
import com.swifteats.restaurant.repository.RestaurantRepository;
import com.swifteats.restaurant.service.RestaurantService;
import com.swifteats.tracking.service.DriverAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class OrderServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID MENU_ITEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333301");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStateHistoryRepository orderStateHistoryRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private KitchenPrepLogRepository kitchenPrepLogRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private RestaurantService restaurantService;
    @Mock
    private DriverAssignmentService driverAssignmentService;
    @Mock
    private OrderStateMachine orderStateMachine;
    @Mock
    private OutboxService outboxService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderAccessValidator orderAccessValidator;
    @Mock
    private MockPaymentGateway mockPaymentGateway;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_returnsExistingOnIdempotencyHit() {
        Order existing = new Order();
        existing.setId(UUID.randomUUID());
        existing.setCustomerId(CUSTOMER_ID);
        OrderAcceptedResponse accepted = new OrderAcceptedResponse(
                existing.getId(), OrderStatus.PENDING_PAYMENT, PaymentStatus.PENDING, BigDecimal.TEN, "msg");

        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));
        when(orderMapper.toAccepted(existing)).thenReturn(accepted);

        CreateOrderRequest request = sampleRequest();
        OrderAcceptedResponse result = orderService.createOrder(CUSTOMER_ID, "key-1", request);

        assertThat(result).isSameAs(accepted);
        verify(customerRepository, org.mockito.Mockito.never()).findById(any());
    }

    @Test
    void createOrder_validatesCustomerAndRestaurant() {
        when(orderRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER_ID, "key-2", sampleRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_persistsOrderAndOutboxEvents() {
        when(orderRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(sampleCustomer()));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setCity("Pune");
        restaurant.setState("Maharashtra");
        restaurant.setEstimatedWaitMins(25);
        when(restaurantService.requireAcceptingOrders(RESTAURANT_ID)).thenReturn(restaurant);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(MENU_ITEM_ID);
        menuItem.setName("Kolhapuri Misal");
        menuItem.setPrice(BigDecimal.valueOf(120));
        menuItem.setAvailable(true);
        when(menuItemRepository.findByIdInAndRestaurant_Id(List.of(MENU_ITEM_ID), RESTAURANT_ID))
                .thenReturn(List.of(menuItem));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
        when(orderMapper.toAccepted(any(Order.class))).thenReturn(new OrderAcceptedResponse(
                UUID.randomUUID(), OrderStatus.PENDING_PAYMENT, PaymentStatus.PENDING, BigDecimal.valueOf(240), "msg"));

        orderService.createOrder(CUSTOMER_ID, "key-3", sampleRequest());

        verify(outboxService).enqueue(eq("Order"), any(UUID.class), eq("OrderCreated"), any());
        verify(outboxService).enqueue(eq("Order"), any(UUID.class), eq("PaymentProcess"), any());
    }

    @Test
    void createOrder_deniesIdempotencyReplayForOtherCustomer() {
        UUID otherCustomer = UUID.randomUUID();
        Order existing = new Order();
        existing.setId(UUID.randomUUID());
        existing.setCustomerId(otherCustomer);

        when(orderRepository.findByIdempotencyKey("shared-key")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER_ID, "shared-key", sampleRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createOrder_deniesIdempotencyRaceForOtherCustomer() {
        when(orderRepository.findByIdempotencyKey("race-key")).thenReturn(Optional.empty());
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(sampleCustomer()));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setCity("Pune");
        restaurant.setState("Maharashtra");
        restaurant.setEstimatedWaitMins(25);
        when(restaurantService.requireAcceptingOrders(RESTAURANT_ID)).thenReturn(restaurant);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(MENU_ITEM_ID);
        menuItem.setName("Kolhapuri Misal");
        menuItem.setPrice(BigDecimal.valueOf(120));
        menuItem.setAvailable(true);
        when(menuItemRepository.findByIdInAndRestaurant_Id(List.of(MENU_ITEM_ID), RESTAURANT_ID))
                .thenReturn(List.of(menuItem));

        when(orderRepository.save(any(Order.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        Order winnerOrder = new Order();
        winnerOrder.setId(UUID.randomUUID());
        winnerOrder.setCustomerId(UUID.randomUUID());
        when(orderRepository.findByIdempotencyKey("race-key")).thenReturn(Optional.empty(), Optional.of(winnerOrder));

        assertThatThrownBy(() -> orderService.createOrder(CUSTOMER_ID, "race-key", sampleRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listCustomerOrders_splitsActiveAndHistory() {
        Order active = new Order();
        active.setId(UUID.randomUUID());
        active.setCustomerId(CUSTOMER_ID);
        active.setRestaurantId(RESTAURANT_ID);
        active.setStatus(OrderStatus.PREPARING);
        active.setPaymentStatus(PaymentStatus.SUCCESS);
        active.setTotalAmount(BigDecimal.TEN);
        active.setDeliveryAddressLine1("Addr");
        active.setCity("Pune");
        active.setCreatedAt(Instant.now());

        Order delivered = new Order();
        delivered.setId(UUID.randomUUID());
        delivered.setCustomerId(CUSTOMER_ID);
        delivered.setRestaurantId(RESTAURANT_ID);
        delivered.setStatus(OrderStatus.DELIVERED);
        delivered.setPaymentStatus(PaymentStatus.SUCCESS);
        delivered.setTotalAmount(BigDecimal.TEN);
        delivered.setDeliveryAddressLine1("Addr");
        delivered.setCity("Pune");
        delivered.setCreatedAt(Instant.now());

        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID))
                .thenReturn(List.of(active, delivered));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setName("Misal House");
        when(restaurantRepository.findAllById(List.of(RESTAURANT_ID))).thenReturn(List.of(restaurant));

        when(orderMapper.toSummary(active, "Misal House"))
                .thenReturn(new com.swifteats.order.dto.OrderSummaryResponse(
                        active.getId(),
                        RESTAURANT_ID,
                        "Misal House",
                        OrderStatus.PREPARING,
                        PaymentStatus.SUCCESS,
                        BigDecimal.TEN,
                        "Addr",
                        "Pune",
                        active.getCreatedAt(),
                        active.getCreatedAt(),
                        null));

        assertThat(orderService.listCustomerOrders(CUSTOMER_ID, "active")).hasSize(1);
        assertThat(orderService.listCustomerOrders(CUSTOMER_ID, "history")).hasSize(1);
    }

    @Test
    void transition_movesOrderToOutForDeliveryAndAssignsDriver() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PREPARING);
        order.setRestaurantId(RESTAURANT_ID);

        UUID driverId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(driverAssignmentService.assignDriver(orderId)).thenReturn(driverId);
        when(restaurantRepository.getReferenceById(RESTAURANT_ID)).thenReturn(new Restaurant());
        when(kitchenPrepLogRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderMapper.toResponse(order, List.of())).thenReturn(null);

        orderService.transition(orderId, OrderStatus.OUT_FOR_DELIVERY, "admin", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(order.getDriverId()).isEqualTo(driverId);
        verify(outboxService).enqueue(eq("Order"), eq(orderId), eq("OrderOutForDelivery"), any());
    }

    @Test
    void transition_isIdempotentWhenStatusUnchanged() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of())).thenReturn(null);

        orderService.transition(orderId, OrderStatus.PREPARING, "admin", null);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void transition_toDeliveredReleasesDriver() {
        UUID orderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDriverId(driverId);
        order.setRestaurantId(RESTAURANT_ID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of())).thenReturn(null);

        orderService.transition(orderId, OrderStatus.DELIVERED, "admin", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(driverAssignmentService).releaseDriver(driverId);
    }

    private static Customer sampleCustomer() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setName("Demo Customer");
        customer.setPhone("9876543210");
        customer.setEmail("demo.customer@example.com");
        return customer;
    }

    private CreateOrderRequest sampleRequest() {
        return new CreateOrderRequest(
                RESTAURANT_ID,
                "123 MG Road, Pune",
                "Pune",
                "Maharashtra",
                "411001",
                "UPI",
                List.of(new CreateOrderRequest.OrderLineRequest(MENU_ITEM_ID, 2)));
    }
}
