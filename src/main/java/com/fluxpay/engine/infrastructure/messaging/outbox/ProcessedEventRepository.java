package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for ProcessedEvent.
 * Used by consumers to check for duplicate event processing.
 */
public interface ProcessedEventRepository extends ReactiveCrudRepository<ProcessedEvent, Long> {

    Mono<Boolean> existsByEventId(String eventId);
}
