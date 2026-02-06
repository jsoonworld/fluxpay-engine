package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.refund.Refund;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a refund is requested.
 */
public record RefundRequestedEvent(
    String eventId,
    String refundId,
    String paymentId,
    BigDecimal amount,
    String currency,
    String reason,
    Instant occurredAt
) implements RefundEvent {

    public RefundRequestedEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(refundId, "refundId is required");
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    /**
     * Creates a RefundRequestedEvent from a Refund domain object.
     *
     * @param refund the refund domain object
     * @return a new RefundRequestedEvent
     */
    @Override
    public String eventType() {
        return "refund.requested";
    }

    public static RefundRequestedEvent from(Refund refund) {
        Objects.requireNonNull(refund, "refund is required");
        return new RefundRequestedEvent(
            UUID.randomUUID().toString(),
            refund.getId().value(),
            refund.getPaymentId().value().toString(),
            refund.getAmount().amount(),
            refund.getAmount().currency().name(),
            refund.getReason(),
            Instant.now()
        );
    }
}
