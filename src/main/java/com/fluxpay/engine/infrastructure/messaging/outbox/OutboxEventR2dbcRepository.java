package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for OutboxEvent.
 */
public interface OutboxEventR2dbcRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

    Mono<OutboxEvent> findByEventId(String eventId);

    Flux<OutboxEvent> findByStatus(String status);

    @Query("SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at LIMIT :limit")
    Flux<OutboxEvent> findPendingEvents(int limit);

    @Modifying
    @Query("UPDATE outbox_events SET status = 'PUBLISHED', published_at = NOW() WHERE id = :id")
    Mono<Integer> markAsPublished(Long id);

    @Modifying
    @Query("UPDATE outbox_events SET status = 'FAILED', error_message = :errorMessage, retry_count = retry_count + 1 WHERE id = :id")
    Mono<Integer> markAsFailed(Long id, String errorMessage);

    @Modifying
    @Query("UPDATE outbox_events SET retry_count = retry_count + 1 WHERE id = :id")
    Mono<Integer> incrementRetryCount(Long id);

    Flux<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);
}
