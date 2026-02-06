package com.fluxpay.engine.domain.model.refund;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Refund identifier.
 * Immutable and compared by value.
 */
public record RefundId(String value) {

    private static final String PREFIX = "ref_";

    public RefundId {
        Objects.requireNonNull(value, "Refund ID value is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Refund ID value cannot be blank");
        }
    }

    /**
     * Generates a new unique RefundId with ref_ prefix.
     */
    public static RefundId generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return new RefundId(PREFIX + uuid);
    }

    /**
     * Creates a RefundId from an existing value.
     */
    public static RefundId of(String value) {
        return new RefundId(value);
    }

    @Override
    public String toString() {
        return "RefundId[" + value + "]";
    }
}
