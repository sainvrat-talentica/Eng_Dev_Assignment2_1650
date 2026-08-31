package com.swifteats.order.entity;

import com.swifteats.common.domain.OrderStatus;
import com.swifteats.common.domain.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.CascadeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "\"order\"")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "delivery_address_line1", nullable = false)
    private String deliveryAddressLine1;

    @Column(name = "delivery_address_line2")
    private String deliveryAddressLine2;

    @Column(nullable = false)
    private String city;

    private String state;
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "payment_processed_at")
    private Instant paymentProcessedAt;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    /** CSV-aligned literals — see DomainLabels.FailureReason */
    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "delay_reason")
    private String delayReason;

    @Column(name = "order_date", nullable = false)
    private Instant orderDate = Instant.now();

    @Column(name = "promised_delivery_at")
    private Instant promisedDeliveryAt;

    @Column(name = "actual_delivery_at")
    private Instant actualDeliveryAt;

    @Column(name = "prep_started_at")
    private Instant prepStartedAt;

    @Column(name = "prep_completed_at")
    private Instant prepCompletedAt;

    @Column(name = "out_for_delivery_at")
    private Instant outForDeliveryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "is_delayed", nullable = false)
    private boolean delayed;

    @Column(name = "is_failed", nullable = false)
    private boolean failed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void refreshDerivedFlags() {
        failed = OrderStatus.FAILED == status || PaymentStatus.FAILED == paymentStatus;
        delayed = actualDeliveryAt != null && promisedDeliveryAt != null
                && actualDeliveryAt.isAfter(promisedDeliveryAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddressLine1() {
        return deliveryAddressLine1;
    }

    public void setDeliveryAddressLine1(String deliveryAddressLine1) {
        this.deliveryAddressLine1 = deliveryAddressLine1;
    }

    public String getDeliveryAddressLine2() {
        return deliveryAddressLine2;
    }

    public void setDeliveryAddressLine2(String deliveryAddressLine2) {
        this.deliveryAddressLine2 = deliveryAddressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public Instant getPaymentProcessedAt() {
        return paymentProcessedAt;
    }

    public void setPaymentProcessedAt(Instant paymentProcessedAt) {
        this.paymentProcessedAt = paymentProcessedAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public void setDelayReason(String delayReason) {
        this.delayReason = delayReason;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Instant orderDate) {
        this.orderDate = orderDate;
    }

    public Instant getPromisedDeliveryAt() {
        return promisedDeliveryAt;
    }

    public void setPromisedDeliveryAt(Instant promisedDeliveryAt) {
        this.promisedDeliveryAt = promisedDeliveryAt;
    }

    public Instant getActualDeliveryAt() {
        return actualDeliveryAt;
    }

    public void setActualDeliveryAt(Instant actualDeliveryAt) {
        this.actualDeliveryAt = actualDeliveryAt;
    }

    public Instant getPrepStartedAt() {
        return prepStartedAt;
    }

    public void setPrepStartedAt(Instant prepStartedAt) {
        this.prepStartedAt = prepStartedAt;
    }

    public Instant getPrepCompletedAt() {
        return prepCompletedAt;
    }

    public void setPrepCompletedAt(Instant prepCompletedAt) {
        this.prepCompletedAt = prepCompletedAt;
    }

    public Instant getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    public void setOutForDeliveryAt(Instant outForDeliveryAt) {
        this.outForDeliveryAt = outForDeliveryAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public boolean isDelayed() {
        return delayed;
    }

    public void setDelayed(boolean delayed) {
        this.delayed = delayed;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
