package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for the Outbox pattern.
 * Enables scheduling only when outbox is enabled.
 */
@Configuration
@ConditionalOnProperty(name = "fluxpay.outbox.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxConfig {
}
