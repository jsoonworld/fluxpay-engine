package com.fluxpay.engine.domain.model.webhook;

/**
 * Exception thrown when an invalid webhook state transition is attempted.
 */
public class InvalidWebhookStateException extends RuntimeException {

    private final WebhookStatus currentStatus;
    private final WebhookStatus targetStatus;

    public InvalidWebhookStateException(WebhookStatus currentStatus, WebhookStatus targetStatus) {
        super(String.format("Invalid state transition from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public WebhookStatus getCurrentStatus() {
        return currentStatus;
    }

    public WebhookStatus getTargetStatus() {
        return targetStatus;
    }
}
