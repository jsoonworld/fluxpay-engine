package com.fluxpay.engine.infrastructure.idempotency;

/**
 * Status of idempotency check result.
 */
public enum IdempotencyStatus {
    /**
     * Cache hit - return stored response.
     */
    HIT,

    /**
     * Cache miss - lock acquired, process the request.
     */
    MISS,

    /**
     * Conflict - same key with different payload.
     */
    CONFLICT,

    /**
     * Processing - another request is currently being processed with this key.
     * The client should retry after a short delay.
     */
    PROCESSING
}
