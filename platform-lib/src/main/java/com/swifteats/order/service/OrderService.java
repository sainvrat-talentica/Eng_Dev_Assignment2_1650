package com.swifteats.order.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.domain.DomainLabels;
import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.PaymentStatus;
import com.swifteats.common.exception.AccessDeniedException;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.order.dto.CreateOrderRequest;
import com.swifteats.order.dto.OrderAcceptedResponse;
import com.swifteats.order.dto.OrderResponse;
import com.swifteats.order.dto.OrderStateHistoryResponse;
import com.swifteats.order.dto.OrderSummaryResponse;
import com.swifteats.order.dto.PaymentProcessMessage;
import com.swifteats.order.dto.PaymentRequest;
import com.swifteats.order.dto.PaymentResult;
import com.swifteats.order.payment.MockPaymentGateway;
import com.swifteats.order.entity.Customer;
import com.swifteats.order.entity.Order;
import com.swifteats.order.entity.OrderItem;
import com.swifteats.order.entity.OrderStateHistory;
import com.swifteats.order.mapper.OrderMapper;
import com.swifteats.order.repository.CustomerRepository;
import com.swifteats.order.repository.OrderItemRepository;
import com.swifteats.order.repository.OrderRepository;
import com.swifteats.order.repository.OrderStateHistoryRepository;
import com.swifteats.restaurant.entity.KitchenPrepLog;
import com.swifteats.restaurant.entity.MenuItem;
import com.swifteats.restaurant.entity.Restaurant;
import com.swifteats.restaurant.repository.KitchenPrepLogRepository;
import com.swifteats.restaurant.repository.MenuItemRepository;
import com.swifteats.restaurant.repository.RestaurantRepository;
import com.swifteats.restaurant.service.RestaurantService;
import com.swifteats.tracking.service.DriverAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ServiceScope(ServiceName.ORDER)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Set<OrderStatus> HISTORY_STATUSES = EnumSet.of(
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED,
            OrderStatus.PAYMENT_FAILED,
            OrderStatus.FAILED,
            OrderStatus.RETURNED);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStateHistoryRepository orderStateHistoryRepository;
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final KitchenPrepLogRepository kitchenPrepLogRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantService restaurantService;
    private final DriverAssignmentService driverAssignmentService;
    private final OrderStateMachine orderStateMachine;
    private final OutboxService outboxService;
    private final OrderMapper orderMapper;
    private final OrderAccessValidator orderAccessValidator;
    private final MockPaymentGateway mockPaymentGateway;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStateHistoryRepository orderStateHistoryRepository,
            CustomerRepository customerRepository,
            MenuItemRepository menuItemRepository,
            KitchenPrepLogRepository kitchenPrepLogRepository,
            RestaurantRepository restaurantRepository,
            RestaurantService restaurantService,
            DriverAssignmentService driverAssignmentService,
            OrderStateMachine orderStateMachine,
            OutboxService outboxService,
            OrderMapper orderMapper,
            OrderAccessValidator orderAccessValidator,
            MockPaymentGateway mockPaymentGateway) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStateHistoryRepository = orderStateHistoryRepository;
        this.customerRepository = customerRepository;
        this.menuItemRepository = menuItemRepository;
        this.kitchenPrepLogRepository = kitchenPrepLogRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantService = restaurantService;
        this.driverAssignmentService = driverAssignmentService;
        this.orderStateMachine = orderStateMachine;
        this.outboxService = outboxService;
        this.orderMapper = orderMapper;
        this.orderAccessValidator = orderAccessValidator;
        this.mockPaymentGateway = mockPaymentGateway;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID customerId) {
        Order order = orderAccessValidator.requireOwnedOrder(orderId, customerId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return orderMapper.toResponse(order, items);
    }

    @Transactional(readOnly = true)
    public List<OrderStateHistoryResponse> getOrderHistory(UUID orderId, UUID customerId) {
        orderAccessValidator.requireOwnedOrder(orderId, customerId);
        return orderStateHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId).stream()
                .map(orderMapper::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listCustomerOrders(UUID customerId, String scope) {
        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        Map<UUID, String> restaurantNames = loadRestaurantNames(orders);
        return orders.stream()
                .filter(order -> matchesScope(order.getStatus(), scope))
                .map(order -> orderMapper.toSummary(
                        order,
                        restaurantNames.getOrDefault(order.getRestaurantId(), "Restaurant")))
                .toList();
    }

    private Map<UUID, String> loadRestaurantNames(List<Order> orders) {
        List<UUID> restaurantIds =
                orders.stream().map(Order::getRestaurantId).distinct().toList();
        if (restaurantIds.isEmpty()) {
            return Map.of();
        }
        return restaurantRepository.findAllById(restaurantIds).stream()
                .collect(Collectors.toMap(Restaurant::getId, Restaurant::getName));
    }

    private static boolean matchesScope(OrderStatus status, String scope) {
        if (scope == null || scope.isBlank() || "all".equalsIgnoreCase(scope)) {
            return true;
        }
        boolean terminal = HISTORY_STATUSES.contains(status);
        if ("active".equalsIgnoreCase(scope)) {
            return !terminal;
        }
        if ("history".equalsIgnoreCase(scope)) {
            return terminal;
        }
        throw new IllegalArgumentException("scope must be active, history, or all");
    }

    @Transactional
    public OrderAcceptedResponse createOrder(UUID customerId, String idempotencyKey, CreateOrderRequest request) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> toAcceptedForCustomer(existing, customerId))
                .orElseGet(() -> doCreate(customerId, idempotencyKey, request));
    }

    private OrderAcceptedResponse toAcceptedForCustomer(Order existing, UUID customerId) {
        if (!existing.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Order access denied");
        }
        return orderMapper.toAccepted(existing);
    }

    private OrderAcceptedResponse doCreate(UUID customerId, String idempotencyKey, CreateOrderRequest request) {
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Restaurant restaurant = restaurantService.requireAcceptingOrders(request.restaurantId());
        Map<UUID, MenuItem> menuItems = loadAndValidateMenuItems(request, restaurant.getId());

        BigDecimal total = calculateTotal(request, menuItems);
        Instant now = Instant.now();
        int waitMins = restaurant.getEstimatedWaitMins() != null ? restaurant.getEstimatedWaitMins() : 30;

        Order order = new Order();
        order.setIdempotencyKey(idempotencyKey);
        order.setCustomerId(customerId);
        order.setCustomerName(customer.getName());
        order.setCustomerPhone(customer.getPhone());
        order.setRestaurantId(restaurant.getId());
        order.setDeliveryAddressLine1(request.deliveryAddress().trim());
        order.setCity(StringUtils.hasText(request.city()) ? request.city().trim() : restaurant.getCity());
        order.setState(StringUtils.hasText(request.state()) ? request.state().trim() : restaurant.getState());
        order.setPincode(request.pincode());
        order.setPaymentMode(request.paymentMode());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(total);
        order.setOrderDate(now);
        order.setPromisedDeliveryAt(now.plusSeconds(waitMins * 60L));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        for (CreateOrderRequest.OrderLineRequest line : request.items()) {
            MenuItem menuItem = menuItems.get(line.menuItemId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItemId(menuItem.getId());
            orderItem.setName(menuItem.getName());
            orderItem.setQuantity(line.quantity());
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItem.setLineTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            order.getItems().add(orderItem);
        }

        try {
            Order saved = orderRepository.save(order);
            recordHistory(saved, null, saved.getStatus(), "ORDER_API", null, now);
            outboxService.enqueue("Order", saved.getId(), "OrderCreated", Map.of("orderId", saved.getId()));
            outboxService.enqueue(
                    "Order",
                    saved.getId(),
                    "PaymentProcess",
                    new PaymentProcessMessage(saved.getId(), saved.getTotalAmount()));
            log.info("Order created id={} idempotencyKey={}", saved.getId(), idempotencyKey);
            return orderMapper.toAccepted(saved);
        } catch (DataIntegrityViolationException ex) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> toAcceptedForCustomer(existing, customerId))
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    public OrderResponse initiatePayment(UUID orderId, UUID customerId) {
        Order order = orderAccessValidator.requireOwnedOrder(orderId, customerId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalArgumentException("Order is not awaiting payment");
        }
        PaymentResult result = mockPaymentGateway.charge(new PaymentRequest(orderId, order.getTotalAmount()));
        if (result.success()) {
            return transition(orderId, OrderStatus.CONFIRMED, "CUSTOMER_PAY", null);
        }
        return transition(orderId, OrderStatus.PAYMENT_FAILED, "CUSTOMER_PAY", result.message());
    }

    @Transactional
    public OrderResponse transition(UUID orderId, OrderStatus newStatus, String changedBy, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus previous = order.getStatus();
        if (previous == newStatus) {
            return orderMapper.toResponse(order, orderItemRepository.findByOrderId(orderId));
        }

        orderStateMachine.validate(previous, newStatus);
        Instant now = Instant.now();
        applySideEffects(order, previous, newStatus, reason, now);
        order.setStatus(newStatus);
        order.setUpdatedAt(now);
        order.refreshDerivedFlags();

        Order saved = orderRepository.save(order);
        recordHistory(saved, previous.name(), newStatus, changedBy, reason, now);
        publishTransitionEvent(saved, newStatus);
        log.info("Order {} transitioned {} -> {} by {}", orderId, previous, newStatus, changedBy);
        return orderMapper.toResponse(saved, orderItemRepository.findByOrderId(orderId));
    }

    private void applySideEffects(
            Order order,
            OrderStatus previous,
            OrderStatus newStatus,
            String reason,
            Instant now) {
        switch (newStatus) {
            case CONFIRMED -> {
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setPaymentProcessedAt(now);
            }
            case PAYMENT_FAILED -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setFailureReason(
                        StringUtils.hasText(reason) ? reason : DomainLabels.FailureReason.PAYMENT_FAILED);
            }
            case PREPARING -> {
                order.setPrepStartedAt(now);
                createOrUpdateKitchenPrep(order, now, false);
            }
            case OUT_FOR_DELIVERY -> {
                order.setPrepCompletedAt(now);
                order.setOutForDeliveryAt(now);
                createOrUpdateKitchenPrep(order, now, true);
                UUID driverId = driverAssignmentService.assignDriver(order.getId());
                order.setDriverId(driverId);
            }
            case DELIVERED -> {
                order.setDeliveredAt(now);
                order.setActualDeliveryAt(now);
                if (order.getDriverId() != null) {
                    driverAssignmentService.releaseDriver(order.getDriverId());
                }
            }
            case FAILED -> order.setFailureReason(
                    StringUtils.hasText(reason) ? reason : DomainLabels.FailureReason.WAREHOUSE_DELAY);
            case DELAYED -> order.setDelayReason(
                    StringUtils.hasText(reason) ? reason : DomainLabels.FailureReason.TRAFFIC_CONGESTION);
            default -> {
                // no additional side effects
            }
        }
    }

    private void createOrUpdateKitchenPrep(Order order, Instant now, boolean dispatch) {
        KitchenPrepLog prepLog = kitchenPrepLogRepository.findByOrderId(order.getId())
                .orElseGet(() -> {
                    KitchenPrepLog log = new KitchenPrepLog();
                    log.setOrderId(order.getId());
                    Restaurant restaurant = restaurantRepository.getReferenceById(order.getRestaurantId());
                    log.setRestaurant(restaurant);
                    return log;
                });
        if (prepLog.getPickingStart() == null) {
            prepLog.setPickingStart(now);
        }
        if (dispatch) {
            prepLog.setPickingEnd(now);
            prepLog.setDispatchTime(now);
        }
        kitchenPrepLogRepository.save(prepLog);
    }

    private void publishTransitionEvent(Order order, OrderStatus newStatus) {
        String eventType = switch (newStatus) {
            case CONFIRMED -> "OrderConfirmed";
            case PREPARING -> "KitchenPrepStarted";
            case OUT_FOR_DELIVERY -> "OrderOutForDelivery";
            case DELIVERED -> "OrderDelivered";
            case FAILED, PAYMENT_FAILED -> "OrderFailed";
            case DELAYED -> "OrderDelayed";
            default -> "OrderStatusChanged";
        };
        outboxService.enqueue("Order", order.getId(), eventType, Map.of(
                "orderId", order.getId(),
                "status", newStatus.name()));
    }

    private void recordHistory(
            Order order,
            String fromStatus,
            OrderStatus toStatus,
            String changedBy,
            String reason,
            Instant changedAt) {
        OrderStateHistory history = new OrderStateHistory();
        history.setOrderId(order.getId());
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus.name());
        history.setChangedBy(changedBy);
        history.setReason(reason);
        history.setChangedAt(changedAt);
        orderStateHistoryRepository.save(history);
    }

    private Map<UUID, MenuItem> loadAndValidateMenuItems(CreateOrderRequest request, UUID restaurantId) {
        List<UUID> menuItemIds = request.items().stream()
                .map(CreateOrderRequest.OrderLineRequest::menuItemId)
                .toList();
        List<MenuItem> found = menuItemRepository.findByIdInAndRestaurant_Id(menuItemIds, restaurantId);
        Map<UUID, MenuItem> byId = new HashMap<>();
        for (MenuItem item : found) {
            byId.put(item.getId(), item);
        }
        if (byId.size() != menuItemIds.size()) {
            throw new IllegalArgumentException("One or more menu items are invalid for this restaurant");
        }
        for (MenuItem item : found) {
            if (!item.isAvailable()) {
                throw new IllegalArgumentException("Menu item unavailable: " + item.getName());
            }
        }
        return byId;
    }

    private BigDecimal calculateTotal(CreateOrderRequest request, Map<UUID, MenuItem> menuItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderLineRequest line : request.items()) {
            MenuItem menuItem = menuItems.get(line.menuItemId());
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        return total;
    }
}
