package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.refund.Refund;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a refund fails.
 */
public record RefundFailedEvent(
    String eventId,
    String refundId,
    String paymentId,
    String errorMessage,
    Instant occurredAt
) implements RefundEvent {

    public RefundFailedEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(refundId, "refundId is required");
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(errorMessage, "errorMessage is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    /**
     * Creates a RefundFailedEvent from a failed Refund domain object.
     *
     * @param refund the failed refund domain object
     * @return a new RefundFailedEvent
     */
    public static RefundFailedEvent from(Refund refund) {
        Objects.requireNonNull(refund, "refund is required");
        return new RefundFailedEvent(
            UUID.randomUUID().toString(),
            refund.getId().value(),
            refund.getPaymentId().value().toString(),
            refund.getErrorMessage(),
            Instant.now()
        );
    }
}
