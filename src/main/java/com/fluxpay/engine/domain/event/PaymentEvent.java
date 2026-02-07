package com.fluxpay.engine.domain.event;

/**
 * Sealed interface for all payment-related domain events.
 */
public sealed interface PaymentEvent extends DomainEvent
    permits PaymentApprovedEvent {

    String paymentId();

    @Override
    default String aggregateType() {
        return "PAYMENT";
    }

    @Override
    default String aggregateId() {
        return paymentId();
    }
}
