package com.fluxpay.engine.infrastructure.idempotency;

/**
 * Result of an idempotency check operation.
 */
public record IdempotencyResult(
    IdempotencyStatus status,
    String cachedResponse,
    int cachedHttpStatus
) {
    public static IdempotencyResult hit(String response, int httpStatus) {
        return new IdempotencyResult(IdempotencyStatus.HIT, response, httpStatus);
    }

    public static IdempotencyResult miss() {
        return new IdempotencyResult(IdempotencyStatus.MISS, null, 0);
    }

    public static IdempotencyResult conflict() {
        return new IdempotencyResult(IdempotencyStatus.CONFLICT, null, 0);
    }

    public static IdempotencyResult processing() {
        return new IdempotencyResult(IdempotencyStatus.PROCESSING, null, 0);
    }

    public boolean isHit() {
        return status == IdempotencyStatus.HIT;
    }

    public boolean isMiss() {
        return status == IdempotencyStatus.MISS;
    }

    public boolean isConflict() {
        return status == IdempotencyStatus.CONFLICT;
    }

    public boolean isProcessing() {
        return status == IdempotencyStatus.PROCESSING;
    }
}
