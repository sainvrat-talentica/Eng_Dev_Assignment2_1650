package com.swifteats.order.mapper;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.order.dto.OrderAcceptedResponse;
import com.swifteats.order.dto.OrderResponse;
import com.swifteats.order.dto.OrderStateHistoryResponse;
import com.swifteats.order.dto.OrderSummaryResponse;
import com.swifteats.order.entity.Order;
import com.swifteats.order.entity.OrderItem;
import com.swifteats.order.entity.OrderStateHistory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ServiceScope(ServiceName.ORDER)
public class OrderMapper {

    public OrderAcceptedResponse toAccepted(Order order) {
        return new OrderAcceptedResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                "Order accepted; payment processing asynchronously");
    }

    public OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderResponse.OrderItemResponse> lineItems = items.stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.getId(),
                        item.getMenuItemId(),
                        item.getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getDriverId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getDeliveryAddressLine1(),
                order.getCity(),
                order.getFailureReason(),
                order.getDelayReason(),
                order.getPromisedDeliveryAt(),
                order.getPrepStartedAt(),
                order.getOutForDeliveryAt(),
                order.getDeliveredAt(),
                lineItems,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getPaymentProcessedAt());
    }

    public OrderSummaryResponse toSummary(Order order, String restaurantName) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getRestaurantId(),
                restaurantName,
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getDeliveryAddressLine1(),
                order.getCity(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getPaymentProcessedAt());
    }

    public OrderStateHistoryResponse toHistoryResponse(OrderStateHistory history) {
        return new OrderStateHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                com.swifteats.common.domain.OrderStatus.valueOf(history.getToStatus()),
                history.getChangedBy(),
                history.getReason(),
                history.getChangedAt());
    }
}
