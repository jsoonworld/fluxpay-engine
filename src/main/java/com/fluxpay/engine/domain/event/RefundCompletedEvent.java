package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.refund.Refund;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a refund is completed successfully.
 */
public record RefundCompletedEvent(
    String eventId,
    String refundId,
    String paymentId,
    BigDecimal amount,
    String currency,
    String pgRefundId,
    Instant occurredAt
) implements RefundEvent {

    public RefundCompletedEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(refundId, "refundId is required");
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        Objects.requireNonNull(pgRefundId, "pgRefundId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    @Override
    public String eventType() {
        return "refund.completed";
    }

    /**
     * Creates a RefundCompletedEvent from a completed Refund domain object.
     *
     * @param refund the completed refund domain object
     * @return a new RefundCompletedEvent
     */
    public static RefundCompletedEvent from(Refund refund) {
        Objects.requireNonNull(refund, "refund is required");
        return new RefundCompletedEvent(
            UUID.randomUUID().toString(),
            refund.getId().value(),
            refund.getPaymentId().value().toString(),
            refund.getAmount().amount(),
            refund.getAmount().currency().name(),
            refund.getPgRefundId(),
            Instant.now()
        );
    }
}
