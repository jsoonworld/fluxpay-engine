package com.fluxpay.engine.infrastructure.idempotency;

import java.util.Objects;

/**
 * Value Object representing an idempotency key composed of tenant, endpoint, and key.
 */
public record IdempotencyKey(
    String tenantId,
    String endpoint,
    String idempotencyKey
) {
    public IdempotencyKey {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(endpoint, "endpoint is required");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
    }

    public String toRedisKey() {
        return "idempotency:" + tenantId + ":" + endpoint + ":" + idempotencyKey;
    }

    public String toCompositeKey() {
        return tenantId + ":" + endpoint + ":" + idempotencyKey;
    }
}
