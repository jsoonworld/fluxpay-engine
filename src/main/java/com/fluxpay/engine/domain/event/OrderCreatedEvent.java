package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.order.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an order is created.
 */
public record OrderCreatedEvent(
    String eventId,
    String orderId,
    String userId,
    BigDecimal totalAmount,
    String currency,
    Instant occurredAt
) implements OrderEvent {

    public OrderCreatedEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(orderId, "orderId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(totalAmount, "totalAmount is required");
        Objects.requireNonNull(currency, "currency is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    @Override
    public String eventType() {
        return "order.created";
    }

    /**
     * Creates an OrderCreatedEvent from an Order domain object.
     */
    public static OrderCreatedEvent from(Order order) {
        Objects.requireNonNull(order, "order is required");
        return new OrderCreatedEvent(
            UUID.randomUUID().toString(),
            order.getId().value().toString(),
            order.getUserId(),
            order.getTotalAmount().amount(),
            order.getCurrency().name(),
            Instant.now()
        );
    }
}
