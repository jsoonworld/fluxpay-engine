package com.fluxpay.engine.infrastructure.idempotency;

/**
 * Exception thrown when Redis idempotency operations fail.
 */
public class RedisIdempotencyException extends RuntimeException {

    public RedisIdempotencyException(String message) {
        super(message);
    }

    public RedisIdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
