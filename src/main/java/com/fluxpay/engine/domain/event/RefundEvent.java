package com.fluxpay.engine.domain.event;

/**
 * Sealed interface for all refund-related domain events.
 * Permits only specific refund event types.
 */
public sealed interface RefundEvent extends DomainEvent
    permits RefundRequestedEvent, RefundCompletedEvent, RefundFailedEvent {

    String refundId();

    String paymentId();
}
