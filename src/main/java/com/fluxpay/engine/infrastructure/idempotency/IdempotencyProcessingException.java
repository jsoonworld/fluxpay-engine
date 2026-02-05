package com.fluxpay.engine.infrastructure.idempotency;

/**
 * Exception thrown when a request with the same idempotency key is already being processed.
 * The client should retry after a short delay.
 */
public class IdempotencyProcessingException extends RuntimeException {

    private final String idempotencyKey;

    public IdempotencyProcessingException(String idempotencyKey) {
        super("Request already being processed for idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public IdempotencyProcessingException(String idempotencyKey, String message) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
