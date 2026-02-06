package com.fluxpay.engine.domain.model.refund;

import java.util.Set;

/**
 * Enum representing the possible states of a Refund.
 *
 * State transitions:
 * REQUESTED -> PROCESSING (when PG refund is initiated)
 * PROCESSING -> COMPLETED (when PG confirms refund)
 * PROCESSING -> FAILED (when PG refund fails)
 */
public enum RefundStatus {
    /**
     * Refund has been requested and is pending PG processing.
     */
    REQUESTED(Set.of("PROCESSING"), false),

    /**
     * Refund is being processed by the PG.
     */
    PROCESSING(Set.of("COMPLETED", "FAILED"), false),

    /**
     * Refund has been completed successfully.
     */
    COMPLETED(Set.of(), true),

    /**
     * Refund has failed.
     */
    FAILED(Set.of(), true);

    private final Set<String> allowedTransitions;
    private final boolean terminal;

    RefundStatus(Set<String> allowedTransitions, boolean terminal) {
        this.allowedTransitions = allowedTransitions;
        this.terminal = terminal;
    }

    /**
     * Checks if transition to the given status is allowed.
     */
    public boolean canTransitionTo(RefundStatus target) {
        return allowedTransitions.contains(target.name());
    }

    /**
     * Returns true if this is a terminal state (no further transitions allowed).
     * COMPLETED and FAILED are terminal states.
     */
    public boolean isTerminal() {
        return terminal;
    }
}
