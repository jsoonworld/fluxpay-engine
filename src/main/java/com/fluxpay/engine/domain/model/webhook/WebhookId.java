package com.fluxpay.engine.domain.model.webhook;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Webhook identifier.
 * Immutable and compared by value.
 */
public record WebhookId(String value) {

    private static final String PREFIX = "whk_";

    public WebhookId {
        Objects.requireNonNull(value, "Webhook ID value is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Webhook ID value cannot be blank");
        }
    }

    /**
     * Generates a new unique WebhookId with whk_ prefix.
     */
    public static WebhookId generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return new WebhookId(PREFIX + uuid);
    }

    /**
     * Creates a WebhookId from an existing value.
     */
    public static WebhookId of(String value) {
        return new WebhookId(value);
    }

    @Override
    public String toString() {
        return "WebhookId[" + value + "]";
    }
}
