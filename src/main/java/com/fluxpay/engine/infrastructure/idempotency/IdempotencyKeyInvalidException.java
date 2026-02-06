package com.fluxpay.engine.infrastructure.idempotency;

/**
 * Exception thrown when the idempotency key has an invalid format.
 */
public class IdempotencyKeyInvalidException extends RuntimeException {

    private final String providedKey;

    public IdempotencyKeyInvalidException(String providedKey) {
        super("Invalid idempotency key format. Expected UUID v4, got: " + providedKey);
        this.providedKey = providedKey;
    }

    public IdempotencyKeyInvalidException(String providedKey, String message) {
        super(message);
        this.providedKey = providedKey;
    }

    public String getProvidedKey() {
        return providedKey;
    }
}
