package com.fluxpay.engine.infrastructure.messaging.outbox;

/**
 * Status of an outbox event.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
