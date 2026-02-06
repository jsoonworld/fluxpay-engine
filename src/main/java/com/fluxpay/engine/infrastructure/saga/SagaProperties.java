package com.fluxpay.engine.infrastructure.saga;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuration properties for saga execution.
 *
 * @param enabled whether saga execution is enabled
 * @param timeout the total saga timeout
 * @param stepTimeout the individual step timeout
 * @param compensation compensation settings
 * @param cleanup cleanup settings
 */
@ConfigurationProperties(prefix = "fluxpay.saga")
public record SagaProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("30s") Duration timeout,
    @DefaultValue("10s") Duration stepTimeout,
    Compensation compensation,
    Cleanup cleanup
) {
    /**
     * Compensation settings.
     *
     * @param maxRetries maximum number of compensation retries
     * @param retryDelay delay between retries
     */
    public record Compensation(
        @DefaultValue("3") int maxRetries,
        @DefaultValue("1s") Duration retryDelay
    ) {}

    /**
     * Cleanup settings for completed sagas.
     *
     * @param enabled whether cleanup is enabled
     * @param retentionDays how long to keep completed sagas
     * @param cron cron expression for cleanup job
     */
    public record Cleanup(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("30") int retentionDays,
        @DefaultValue("0 0 2 * * *") String cron
    ) {}
}
