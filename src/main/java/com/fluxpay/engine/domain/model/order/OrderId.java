package com.fluxpay.engine.domain.model.order;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing an Order's unique identifier.
 * UUID-based immutable identifier.
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "OrderId value is required");
    }

    /**
     * Creates a new OrderId with a random UUID.
     */
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    /**
     * Creates an OrderId from an existing UUID.
     */
    public static OrderId of(UUID value) {
        Objects.requireNonNull(value, "OrderId value is required");
        return new OrderId(value);
    }

    /**
     * Creates an OrderId from a string representation of a UUID.
     *
     * @param value the string representation of a UUID
     * @return a new OrderId
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is not a valid UUID format
     */
    public static OrderId of(String value) {
        Objects.requireNonNull(value, "OrderId value is required");
        try {
            return new OrderId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + value, e);
        }
    }

    @Override
    public String toString() {
        return "OrderId[" + value + "]";
    }
}
