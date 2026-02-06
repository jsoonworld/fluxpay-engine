package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.model.webhook.Webhook;
import com.fluxpay.engine.domain.model.webhook.WebhookId;
import com.fluxpay.engine.domain.model.webhook.WebhookStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Repository port for Webhook persistence.
 * Defines the contract for storing and retrieving webhooks.
 */
public interface WebhookRepository {

    Mono<Webhook> save(Webhook webhook);

    Mono<Webhook> findById(WebhookId id);

    Flux<Webhook> findByStatus(WebhookStatus status);

    Flux<Webhook> findPendingRetries(Instant before);

    Mono<Void> delete(WebhookId id);
}
