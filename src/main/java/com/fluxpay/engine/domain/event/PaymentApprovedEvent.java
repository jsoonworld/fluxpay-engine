package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.payment.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a payment is approved.
 */
public record PaymentApprovedEvent(
    String eventId,
    String paymentId,
    String orderId,
    BigDecimal amount,
    String currency,
    String pgPaymentKey,
    Instant occurredAt
) implements PaymentEvent {

    public PaymentApprovedEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(orderId, "orderId is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        // pgPaymentKey can be null
    }

    @Override
    public String eventType() {
        return "payment.approved";
    }

    /**
     * Creates a PaymentApprovedEvent from a Payment domain object.
     */
    public static PaymentApprovedEvent from(Payment payment) {
        Objects.requireNonNull(payment, "payment is required");
        return new PaymentApprovedEvent(
            UUID.randomUUID().toString(),
            payment.getId().value().toString(),
            payment.getOrderId().value().toString(),
            payment.getAmount().amount(),
            payment.getAmount().currency().name(),
            payment.getPgPaymentKey(),
            Instant.now()
        );
    }
}
