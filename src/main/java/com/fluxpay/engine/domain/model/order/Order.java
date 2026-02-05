package com.fluxpay.engine.domain.model.order;

import com.fluxpay.engine.domain.exception.InvalidOrderStateException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregate Root representing an Order in the domain.
 *
 * An Order has identity, mutable state with controlled transitions,
 * and its lifecycle is tracked over time.
 *
 * State transitions:
 * PENDING -> PAID -> COMPLETED
 * PENDING -> CANCELLED
 * PENDING -> FAILED
 * PAID -> CANCELLED
 * PAID -> FAILED
 */
public class Order {

    private final OrderId id;
    private final String userId;
    private final List<OrderLineItem> lineItems;
    private final Currency currency;
    private final Map<String, Object> metadata;
    private final Instant createdAt;

    private OrderStatus status;
    private Money totalAmount;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant completedAt;

    private Order(OrderId id, String userId, List<OrderLineItem> lineItems,
                  Currency currency, Map<String, Object> metadata,
                  OrderStatus status, Money totalAmount,
                  Instant createdAt, Instant updatedAt,
                  Instant paidAt, Instant completedAt) {
        this.id = Objects.requireNonNull(id, "Order ID is required");
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.lineItems = new ArrayList<>(lineItems);
        this.currency = Objects.requireNonNull(currency, "Currency is required");
        this.metadata = new HashMap<>(metadata != null ? metadata : Map.of());
        this.status = Objects.requireNonNull(status, "Status is required");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at is required");
        this.updatedAt = updatedAt;
        this.paidAt = paidAt;
        this.completedAt = completedAt;
    }

    /**
     * Factory method to create a new Order.
     *
     * @param userId    the user ID placing the order
     * @param lineItems the list of line items (must not be empty)
     * @param currency  the currency for the order
     * @param metadata  optional metadata map
     * @return a new Order with PENDING status
     * @throws IllegalArgumentException if lineItems is null or empty, or userId is blank
     */
    public static Order create(String userId, List<OrderLineItem> lineItems,
                               Currency currency, Map<String, Object> metadata) {
        Objects.requireNonNull(userId, "User ID is required");
        validateUserId(userId);
        Objects.requireNonNull(currency, "Currency is required");

        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line item");
        }
        if (lineItems.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Line items cannot contain null elements");
        }

        OrderId id = OrderId.generate();
        Money totalAmount = calculateTotalAmount(lineItems, currency);
        Instant now = Instant.now();

        return new Order(id, userId, lineItems, currency, metadata,
                        OrderStatus.PENDING, totalAmount, now, now, null, null);
    }

    private static void validateUserId(String userId) {
        if (userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be blank");
        }
    }

    /**
     * Restores an Order from persistence.
     * Used when reconstituting an order from the database.
     *
     * @throws IllegalArgumentException if userId is blank
     * @throws IllegalStateException if state/timestamp invariants are violated
     */
    public static Order restore(OrderId id, String userId, List<OrderLineItem> lineItems,
                                Currency currency, Map<String, Object> metadata,
                                OrderStatus status, Money totalAmount,
                                Instant createdAt, Instant updatedAt,
                                Instant paidAt, Instant completedAt) {
        Objects.requireNonNull(userId, "User ID is required");
        validateUserId(userId);
        validateStateTimestampInvariants(status, paidAt, completedAt);

        return new Order(id, userId, lineItems, currency, metadata,
                        status, totalAmount, createdAt, updatedAt, paidAt, completedAt);
    }

    private static void validateStateTimestampInvariants(OrderStatus status,
                                                          Instant paidAt,
                                                          Instant completedAt) {
        if (status == OrderStatus.PAID || status == OrderStatus.COMPLETED) {
            if (paidAt == null) {
                throw new IllegalStateException(
                    "Order with status " + status + " must have paidAt timestamp");
            }
        }

        if (status == OrderStatus.COMPLETED) {
            if (completedAt == null) {
                throw new IllegalStateException(
                    "Order with status COMPLETED must have completedAt timestamp");
            }
        }
    }

    private static Money calculateTotalAmount(List<OrderLineItem> lineItems, Currency currency) {
        return lineItems.stream()
            .map(OrderLineItem::getTotalPrice)
            .reduce(Money.zero(currency), Money::add);
    }

    // State transition methods

    /**
     * Marks the order as paid.
     *
     * @throws InvalidOrderStateException if the order is not in PENDING state
     */
    public void markAsPaid() {
        validateStateTransition(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.paidAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Completes the order.
     *
     * @throws InvalidOrderStateException if the order is not in PAID state
     */
    public void complete() {
        validateStateTransition(OrderStatus.COMPLETED);
        this.status = OrderStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Cancels the order.
     *
     * @throws InvalidOrderStateException if the order cannot be cancelled from current state
     */
    public void cancel() {
        validateStateTransition(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the order as failed.
     *
     * @throws InvalidOrderStateException if the order cannot be marked as failed from current state
     */
    public void fail() {
        validateStateTransition(OrderStatus.FAILED);
        this.status = OrderStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void validateStateTransition(OrderStatus targetStatus) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new InvalidOrderStateException(this.status, targetStatus);
        }
    }

    // Status helper methods

    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    public boolean isCompleted() {
        return this.status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public boolean isFailed() {
        return this.status == OrderStatus.FAILED;
    }

    // Getters

    public OrderId getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public List<OrderLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public Currency getCurrency() {
        return currency;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
               "id=" + id +
               ", userId='" + userId + '\'' +
               ", status=" + status +
               ", totalAmount=" + totalAmount +
               ", createdAt=" + createdAt +
               '}';
    }
}
