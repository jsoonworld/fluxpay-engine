package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Polls the outbox_events table for PENDING events and publishes them to Kafka.
 * Uses scheduled polling with configurable batch size and retry logic.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventR2dbcRepository outboxRepository;
    private final KafkaEventPublisher kafkaPublisher;
    private final int batchSize;
    private final int maxRetries;

    public OutboxPublisher(
            OutboxEventR2dbcRepository outboxRepository,
            KafkaEventPublisher kafkaPublisher,
            @Value("${fluxpay.outbox.batch-size:100}") int batchSize,
            @Value("${fluxpay.outbox.max-retries:3}") int maxRetries) {
        this.outboxRepository = outboxRepository;
        this.kafkaPublisher = kafkaPublisher;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    /**
     * Scheduled entry point - polls for PENDING events and publishes them.
     */
    @Scheduled(fixedRateString = "${fluxpay.outbox.polling-interval-ms:100}")
    public void scheduledPublish() {
        publishPendingEvents().subscribe();
    }

    /**
     * Processes PENDING outbox events and publishes them to Kafka.
     * Each event is processed independently - one failure does not affect others.
     *
     * @return Flux completing when all events in this batch are processed
     */
    Flux<Void> publishPendingEvents() {
        return outboxRepository.findPendingEvents(batchSize)
            .concatMap(event -> publishToKafka(event)
                .onErrorResume(error -> handlePublishError(event, error)));
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

        return outboxRepository.incrementRetryCount(event.getId()).then();
    }
}
