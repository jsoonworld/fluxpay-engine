package com.fluxpay.engine.infrastructure.webhook;

/**
 * Exception thrown when webhook signature operations fail.
 */
public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException(String message) {
        super(message);
    }

    public WebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
