package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Polls the outbox_events table for PENDING events and publishes them to Kafka.
 * Uses atomic claim (FOR UPDATE SKIP LOCKED) to prevent duplicate publishing
 * across multiple instances.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventR2dbcRepository outboxRepository;
    private final KafkaEventPublisher kafkaPublisher;
    private final int batchSize;
    private final int maxRetries;
    private final boolean cleanupEnabled;
    private final int retentionDays;

    public OutboxPublisher(
            OutboxEventR2dbcRepository outboxRepository,
            KafkaEventPublisher kafkaPublisher,
            @Value("${fluxpay.outbox.batch-size:100}") int batchSize,
            @Value("${fluxpay.outbox.max-retries:3}") int maxRetries,
            @Value("${fluxpay.outbox.cleanup.enabled:true}") boolean cleanupEnabled,
            @Value("${fluxpay.outbox.cleanup.retention-days:7}") int retentionDays) {
        this.outboxRepository = outboxRepository;
        this.kafkaPublisher = kafkaPublisher;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.cleanupEnabled = cleanupEnabled;
        this.retentionDays = retentionDays;
    }

    /**
     * Scheduled entry point - claims and publishes pending events.
     * Uses fixedDelay to prevent overlapping runs.
     */
    @Scheduled(fixedDelayString = "${fluxpay.outbox.polling-interval-ms:100}")
    public void scheduledPublish() {
        publishPendingEvents()
            .doOnError(error -> log.error("Outbox publishing cycle failed", error))
            .onErrorComplete()
            .subscribe();
    }

    /**
     * Scheduled cleanup of old published events.
     */
    @Scheduled(cron = "${fluxpay.outbox.cleanup.cron:0 0 3 * * *}")
    public void scheduledCleanup() {
        if (!cleanupEnabled) {
            return;
        }
        cleanupPublishedEvents()
            .doOnError(error -> log.error("Outbox cleanup failed", error))
            .onErrorComplete()
            .subscribe();
    }

    /**
     * Atomically claims PENDING outbox events and publishes them to Kafka.
     * Uses FOR UPDATE SKIP LOCKED to prevent duplicate processing across instances.
     * Each event is processed independently - one failure does not affect others.
     *
     * @return Flux completing when all events in this batch are processed
     */
    Flux<Void> publishPendingEvents() {
        return outboxRepository.claimPendingEvents(batchSize)
            .concatMap(event -> publishToKafka(event)
                .onErrorResume(error -> handlePublishError(event, error)));
    }

    /**
     * Deletes published events older than the retention period.
     *
     * @return Mono completing when cleanup is done
     */
    Mono<Void> cleanupPublishedEvents() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        return outboxRepository.deletePublishedBefore(cutoff)
            .doOnSuccess(count -> log.info("Cleaned up {} published outbox events", count))
            .then();
    }

    private Mono<Void> publishToKafka(OutboxEvent event) {
        return kafkaPublisher.send(event)
            .then(Mono.defer(() -> outboxRepository.markAsPublished(event.getId())))
            .doOnSuccess(v -> log.debug("Outbox event published: eventId={}", event.getEventId()))
            .then();
    }

    private Mono<Void> handlePublishError(OutboxEvent event, Throwable error) {
        log.warn("Failed to publish outbox event: eventId={}, retryCount={}, error={}",
            event.getEventId(), event.getRetryCount(), error.getMessage());

        if (event.getRetryCount() >= maxRetries) {
            log.error("Outbox event exceeded max retries, marking as FAILED: eventId={}",
                event.getEventId());
            return outboxRepository.markAsFailed(event.getId(), error.getMessage()).then();
        }

        return outboxRepository.resetToPending(event.getId()).then();
    }
}
