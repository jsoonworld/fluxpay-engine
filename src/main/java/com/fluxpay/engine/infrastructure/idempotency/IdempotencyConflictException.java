package com.fluxpay.engine.infrastructure.idempotency;

/**
 * Exception thrown when the same idempotency key is used with a different payload.
 */
public class IdempotencyConflictException extends RuntimeException {

    private final String idempotencyKey;

    public IdempotencyConflictException(String idempotencyKey) {
        super("Payload mismatch for idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public IdempotencyConflictException(String idempotencyKey, String message) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
