package com.fluxpay.engine.infrastructure.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for idempotency operations.
 */
@ConfigurationProperties(prefix = "fluxpay.idempotency")
public record IdempotencyProperties(
    boolean enabled,
    Duration ttl,
    Redis redis
) {
    /**
     * Redis-specific configuration for idempotency.
     */
    public record Redis(
        String keyPrefix,
        Duration timeout
    ) {
        /**
         * Create Redis config with defaults.
         */
        public Redis {
            if (keyPrefix == null || keyPrefix.isBlank()) {
                keyPrefix = "idempotency";
            }
            if (timeout == null) {
                timeout = Duration.ofSeconds(3);
            }
        }
    }

    /**
     * Create IdempotencyProperties with defaults.
     */
    public IdempotencyProperties {
        if (ttl == null) {
            ttl = Duration.ofHours(24);
        }
        if (redis == null) {
            redis = new Redis("idempotency", Duration.ofSeconds(3));
        }
    }

    /**
     * Default configuration.
     */
    public static IdempotencyProperties defaults() {
        return new IdempotencyProperties(true, Duration.ofHours(24), new Redis("idempotency", Duration.ofSeconds(3)));
    }
}
