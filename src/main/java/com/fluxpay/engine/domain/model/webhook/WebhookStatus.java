package com.fluxpay.engine.domain.model.webhook;

import java.util.Set;

/**
 * Enum representing the possible states of a Webhook delivery.
 *
 * State transitions:
 * PENDING -> SENDING (when delivery attempt starts)
 * SENDING -> DELIVERED (when successfully delivered)
 * SENDING -> FAILED (when permanent failure or max retries exhausted)
 * SENDING -> RETRYING (when delivery fails but retries remain)
 * RETRYING -> SENDING (when retry attempt starts)
 */
public enum WebhookStatus {
    /**
     * Webhook is pending initial delivery.
     */
    PENDING(Set.of("SENDING"), false),

    /**
     * Webhook delivery is in progress.
     */
    SENDING(Set.of("DELIVERED", "FAILED", "RETRYING"), false),

    /**
     * Webhook delivery failed, waiting to retry.
     */
    RETRYING(Set.of("SENDING"), false),

    /**
     * Webhook has been successfully delivered.
     */
    DELIVERED(Set.of(), true),

    /**
     * Webhook delivery has permanently failed.
     */
    FAILED(Set.of(), true);

    private final Set<String> allowedTransitions;
    private final boolean terminal;

    WebhookStatus(Set<String> allowedTransitions, boolean terminal) {
        this.allowedTransitions = allowedTransitions;
        this.terminal = terminal;
    }

    /**
     * Checks if transition to the given status is allowed.
     */
    public boolean canTransitionTo(WebhookStatus target) {
        return allowedTransitions.contains(target.name());
    }

    /**
     * Returns true if this is a terminal state (no further transitions allowed).
     * DELIVERED and FAILED are terminal states.
     */
    public boolean isTerminal() {
        return terminal;
    }
}
