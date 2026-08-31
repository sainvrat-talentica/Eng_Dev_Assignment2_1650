package com.swifteats.order.mapper;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.PaymentStatus;
import com.swifteats.order.dto.OrderAcceptedResponse;
import com.swifteats.order.dto.OrderResponse;
import com.swifteats.order.dto.OrderStateHistoryResponse;
import com.swifteats.order.dto.OrderSummaryResponse;
import com.swifteats.order.entity.Order;
import com.swifteats.order.entity.OrderItem;
import com.swifteats.order.entity.OrderStateHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID DRIVER_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID MENU_ITEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333301");
    private static final UUID ITEM_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID HISTORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private OrderMapper mapper;
    private Instant now;

    @BeforeEach
    void setUp() {
        mapper = new OrderMapper();
        now = Instant.parse("2025-08-15T12:00:00Z");
    }

    @Test
    void toAccepted_mapsOrderFields() {
        Order order = sampleOrder();

        OrderAcceptedResponse response = mapper.toAccepted(order);

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("350.00");
        assertThat(response.message()).contains("payment processing asynchronously");
    }

    @Test
    void toResponse_mapsOrderAndLineItems() {
        Order order = sampleOrder();
        OrderItem item = sampleOrderItem();

        OrderResponse response = mapper.toResponse(order, List.of(item));

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(response.driverId()).isEqualTo(DRIVER_ID);
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("350.00");
        assertThat(response.deliveryAddressLine1()).isEqualTo("42 FC Road");
        assertThat(response.city()).isEqualTo("Pune");
        assertThat(response.failureReason()).isEqualTo("Traffic congestion");
        assertThat(response.delayReason()).isEqualTo("Heavy rain");
        assertThat(response.promisedDeliveryAt()).isEqualTo(now);
        assertThat(response.prepStartedAt()).isEqualTo(now);
        assertThat(response.outForDeliveryAt()).isEqualTo(now);
        assertThat(response.deliveredAt()).isEqualTo(now);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
        assertThat(response.paymentProcessedAt()).isEqualTo(now);

        assertThat(response.items()).hasSize(1);
        OrderResponse.OrderItemResponse line = response.items().get(0);
        assertThat(line.id()).isEqualTo(ITEM_ID);
        assertThat(line.menuItemId()).isEqualTo(MENU_ITEM_ID);
        assertThat(line.name()).isEqualTo("Kolhapuri Misal");
        assertThat(line.quantity()).isEqualTo(2);
        assertThat(line.unitPrice()).isEqualByComparingTo("120.00");
        assertThat(line.lineTotal()).isEqualByComparingTo("240.00");
    }

    @Test
    void toSummary_mapsOrderWithRestaurantName() {
        Order order = sampleOrder();

        OrderSummaryResponse response = mapper.toSummary(order, "Misal House");

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(response.restaurantName()).isEqualTo("Misal House");
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("350.00");
        assertThat(response.deliveryAddressLine1()).isEqualTo("42 FC Road");
        assertThat(response.city()).isEqualTo("Pune");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
        assertThat(response.paymentProcessedAt()).isEqualTo(now);
    }

    @Test
    void toHistoryResponse_mapsStateTransition() {
        OrderStateHistory history = new OrderStateHistory();
        history.setId(HISTORY_ID);
        history.setOrderId(ORDER_ID);
        history.setFromStatus("PENDING");
        history.setToStatus("CONFIRMED");
        history.setChangedBy("PAYMENT_WORKER");
        history.setReason("Payment approved");
        history.setChangedAt(now);

        OrderStateHistoryResponse response = mapper.toHistoryResponse(history);

        assertThat(response.id()).isEqualTo(HISTORY_ID);
        assertThat(response.fromStatus()).isEqualTo("PENDING");
        assertThat(response.toStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.changedBy()).isEqualTo("PAYMENT_WORKER");
        assertThat(response.reason()).isEqualTo("Payment approved");
        assertThat(response.changedAt()).isEqualTo(now);
    }

    private Order sampleOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setRestaurantId(RESTAURANT_ID);
        order.setDriverId(DRIVER_ID);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(new BigDecimal("350.00"));
        order.setDeliveryAddressLine1("42 FC Road");
        order.setCity("Pune");
        order.setFailureReason("Traffic congestion");
        order.setDelayReason("Heavy rain");
        order.setPromisedDeliveryAt(now);
        order.setPrepStartedAt(now);
        order.setOutForDeliveryAt(now);
        order.setDeliveredAt(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setPaymentProcessedAt(now);
        return order;
    }

    private OrderItem sampleOrderItem() {
        OrderItem item = new OrderItem();
        item.setId(ITEM_ID);
        item.setMenuItemId(MENU_ITEM_ID);
        item.setName("Kolhapuri Misal");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("120.00"));
        item.setLineTotal(new BigDecimal("240.00"));
        return item;
    }
}
