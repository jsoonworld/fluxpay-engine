package com.fluxpay.engine.domain.event;

/**
 * Sealed interface for all order-related domain events.
 */
public sealed interface OrderEvent extends DomainEvent
    permits OrderCreatedEvent {

    String orderId();

    @Override
    default String aggregateType() {
        return "ORDER";
    }

    @Override
    default String aggregateId() {
        return orderId();
    }
}
