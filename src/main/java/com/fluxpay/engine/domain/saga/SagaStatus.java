package com.fluxpay.engine.domain.saga;

import java.util.Set;

/**
 * Enum representing the possible states of a Saga.
 *
 * State transitions:
 * STARTED -> PROCESSING (when saga execution begins)
 * PROCESSING -> COMPLETED (when all steps succeed)
 * PROCESSING -> COMPENSATING (when a step fails)
 * COMPENSATING -> COMPENSATED (when compensation completes)
 * COMPENSATING -> FAILED (when compensation fails)
 */
public enum SagaStatus {
    /**
     * Saga has been created and is ready to start.
     */
    STARTED(Set.of("PROCESSING"), false),

    /**
     * Saga is actively processing steps.
     */
    PROCESSING(Set.of("COMPLETED", "COMPENSATING"), false),

    /**
     * Saga has successfully completed all steps.
     */
    COMPLETED(Set.of(), true),

    /**
     * Saga is compensating due to a failed step.
     */
    COMPENSATING(Set.of("COMPENSATED", "FAILED"), false),

    /**
     * Saga has successfully compensated all executed steps.
     */
    COMPENSATED(Set.of(), true),

    /**
     * Saga has failed (either during execution or compensation).
     */
    FAILED(Set.of(), true);

    private final Set<String> allowedTransitions;
    private final boolean terminal;

    SagaStatus(Set<String> allowedTransitions, boolean terminal) {
        this.allowedTransitions = allowedTransitions;
        this.terminal = terminal;
    }

    /**
     * Checks if transition to the given status is allowed.
     */
    public boolean canTransitionTo(SagaStatus target) {
        return allowedTransitions.contains(target.name());
    }

    /**
     * Returns true if this is a terminal state (no further transitions allowed).
     * COMPLETED, COMPENSATED, and FAILED are terminal states.
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Returns true if the saga is actively processing (not in a terminal state).
     */
    public boolean isActive() {
        return !terminal;
    }
}
