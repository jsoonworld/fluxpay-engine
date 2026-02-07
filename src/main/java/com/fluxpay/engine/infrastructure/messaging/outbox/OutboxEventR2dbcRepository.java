package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Spring Data R2DBC repository for OutboxEvent.
 */
public interface OutboxEventR2dbcRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

    Mono<OutboxEvent> findByEventId(String eventId);

    Flux<OutboxEvent> findByStatus(String status);

    @Query("SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at LIMIT :limit")
    Flux<OutboxEvent> findPendingEvents(int limit);

    @Query("UPDATE outbox_events SET status = 'PROCESSING' WHERE id IN ("
         + "SELECT id FROM outbox_events WHERE status = 'PENDING' "
         + "ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED"
         + ") RETURNING *")
    Flux<OutboxEvent> claimPendingEvents(int limit);

    @Modifying
    @Query("UPDATE outbox_events SET status = 'PUBLISHED', published_at = NOW() WHERE id = :id AND status = 'PROCESSING'")
    Mono<Integer> markAsPublished(Long id);

    @Modifying
    @Query("UPDATE outbox_events SET status = 'FAILED', error_message = :errorMessage, retry_count = retry_count + 1 WHERE id = :id AND status = 'PROCESSING'")
    Mono<Integer> markAsFailed(Long id, String errorMessage);

    @Modifying
    @Query("UPDATE outbox_events SET status = 'PENDING', retry_count = retry_count + 1 WHERE id = :id AND status = 'PROCESSING'")
    Mono<Integer> resetToPending(Long id);

    @Modifying
    @Query("UPDATE outbox_events SET retry_count = retry_count + 1 WHERE id = :id")
    Mono<Integer> incrementRetryCount(Long id);

    @Modifying
    @Query("DELETE FROM outbox_events WHERE status = 'PUBLISHED' AND published_at < :before")
    Mono<Integer> deletePublishedBefore(Instant before);

    Flux<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);
}
