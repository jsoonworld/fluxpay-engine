package com.fluxpay.engine.domain.model.payment;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Payment's unique identifier.
 * UUID-based immutable identifier.
 */
public record PaymentId(UUID value) {

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId value is required");
    }

    /**
     * Creates a new PaymentId with a random UUID.
     */
    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    /**
     * Creates a PaymentId from an existing UUID.
     */
    public static PaymentId of(UUID value) {
        Objects.requireNonNull(value, "PaymentId value is required");
        return new PaymentId(value);
    }

    /**
     * Creates a PaymentId from a string representation of a UUID.
     *
     * @param value the string representation of a UUID
     * @return a new PaymentId
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a valid UUID format
     */
    public static PaymentId of(String value) {
        Objects.requireNonNull(value, "PaymentId value is required");
        try {
            return new PaymentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
