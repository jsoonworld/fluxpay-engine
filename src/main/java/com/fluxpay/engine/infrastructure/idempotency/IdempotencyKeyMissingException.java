package com.fluxpay.engine.infrastructure.idempotency;

/**
 * Exception thrown when a required idempotency key header is missing.
 */
public class IdempotencyKeyMissingException extends RuntimeException {

    public IdempotencyKeyMissingException() {
        super("X-Idempotency-Key header is required");
    }

    public IdempotencyKeyMissingException(String message) {
        super(message);
    }
}
