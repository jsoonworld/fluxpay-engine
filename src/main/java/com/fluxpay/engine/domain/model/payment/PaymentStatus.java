package com.fluxpay.engine.domain.model.payment;

import java.util.Set;

/**
 * Enum representing the possible states of a Payment.
 *
 * State transitions:
 * READY -> PROCESSING (when approval is requested)
 * PROCESSING -> APPROVED (when PG approval succeeds)
 * PROCESSING -> FAILED (when PG approval fails)
 * APPROVED -> CONFIRMED (when payment is confirmed)
 * APPROVED -> FAILED (when confirmation fails)
 * CONFIRMED -> REFUNDED (when payment is refunded)
 */
public enum PaymentStatus {
    /**
     * Payment is created and ready for processing.
     */
    READY(Set.of("PROCESSING", "FAILED"), false),

    /**
     * Payment is being processed by the PG.
     */
    PROCESSING(Set.of("APPROVED", "FAILED"), false),

    /**
     * Payment has been approved by the PG (funds held).
     */
    APPROVED(Set.of("CONFIRMED", "FAILED"), false),

    /**
     * Payment has been confirmed (funds captured).
     * Can transition to REFUNDED for refund processing.
     */
    CONFIRMED(Set.of("REFUNDED"), false),

    /**
     * Payment has failed.
     */
    FAILED(Set.of(), true),

    /**
     * Payment has been refunded.
     */
    REFUNDED(Set.of(), true);

    private final Set<String> allowedTransitions;
    private final boolean terminal;

    PaymentStatus(Set<String> allowedTransitions, boolean terminal) {
        this.allowedTransitions = allowedTransitions;
        this.terminal = terminal;
    }

    /**
     * Checks if transition to the given status is allowed.
     */
    public boolean canTransitionTo(PaymentStatus target) {
        return allowedTransitions.contains(target.name());
    }

    /**
     * Returns true if this is a terminal state (no further transitions allowed).
     * FAILED and REFUNDED are terminal states.
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Returns true if the payment can be confirmed from this state.
     * Only APPROVED status can be confirmed.
     */
    public boolean canBeConfirmed() {
        return this == APPROVED;
    }
}
