package com.fluxpay.engine.domain.model.order;

import java.util.Set;

/**
 * Enum representing the lifecycle states of an Order.
 *
 * State transitions:
 * PENDING -> PAID -> COMPLETED
 * PENDING -> CANCELLED
 * PENDING -> FAILED
 * PAID -> CANCELLED
 * PAID -> FAILED
 */
public enum OrderStatus {
    PENDING(Set.of("PAID", "CANCELLED", "FAILED")),
    PAID(Set.of("COMPLETED", "CANCELLED", "FAILED")),
    COMPLETED(Set.of()),
    CANCELLED(Set.of()),
    FAILED(Set.of());

    private final Set<String> allowedTransitions;

    OrderStatus(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    /**
     * Checks if this status can transition to the target status.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions.contains(target.name());
    }

    /**
     * Returns true if this is a terminal state (no further transitions allowed).
     */
    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }
}
