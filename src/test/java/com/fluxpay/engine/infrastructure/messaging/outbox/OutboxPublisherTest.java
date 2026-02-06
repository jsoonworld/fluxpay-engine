package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisher")
class OutboxPublisherTest {

    @Mock
    private OutboxEventR2dbcRepository outboxRepository;

    @Mock
    private KafkaEventPublisher kafkaPublisher;

    private OutboxPublisher outboxPublisher;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxRepository, kafkaPublisher, BATCH_SIZE, MAX_RETRIES);
    }

    @Nested
    @DisplayName("publishPendingEvents")
    class PublishPendingEvents {

        @Test
        @DisplayName("should publish pending events to Kafka and mark as published")
        void shouldPublishPendingEvents() {
            // Given
            OutboxEvent event1 = createOutboxEvent(1L, "evt-1");
            OutboxEvent event2 = createOutboxEvent(2L, "evt-2");
            when(outboxRepository.findPendingEvents(BATCH_SIZE))
                .thenReturn(Flux.just(event1, event2));
            when(kafkaPublisher.send(any(OutboxEvent.class)))
                .thenReturn(Mono.empty());
            when(outboxRepository.markAsPublished(anyLong()))
                .thenReturn(Mono.just(1));

            // When & Then
            StepVerifier.create(outboxPublisher.publishPendingEvents())
                .verifyComplete();

            verify(kafkaPublisher).send(event1);
            verify(kafkaPublisher).send(event2);
            verify(outboxRepository).markAsPublished(1L);
            verify(outboxRepository).markAsPublished(2L);
        }

        @Test
        @DisplayName("should do nothing when no pending events")
        void shouldDoNothingWhenNoPendingEvents() {
            // Given
            when(outboxRepository.findPendingEvents(BATCH_SIZE))
                .thenReturn(Flux.empty());

            // When & Then
            StepVerifier.create(outboxPublisher.publishPendingEvents())
                .verifyComplete();

            verify(kafkaPublisher, never()).send(any());
            verify(outboxRepository, never()).markAsPublished(anyLong());
        }

        @Test
        @DisplayName("should increment retry count on Kafka failure")
        void shouldIncrementRetryCountOnKafkaFailure() {
            // Given
            OutboxEvent event = createOutboxEvent(1L, "evt-1");
            event.setRetryCount(0);
            when(outboxRepository.findPendingEvents(BATCH_SIZE))
                .thenReturn(Flux.just(event));
            when(kafkaPublisher.send(any(OutboxEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("Kafka unavailable")));
            when(outboxRepository.incrementRetryCount(1L))
                .thenReturn(Mono.just(1));

            // When & Then
            StepVerifier.create(outboxPublisher.publishPendingEvents())
                .verifyComplete();

            verify(outboxRepository).incrementRetryCount(1L);
            verify(outboxRepository, never()).markAsPublished(anyLong());
        }

        @Test
        @DisplayName("should mark as FAILED after max retries exceeded")
        void shouldMarkAsFailedAfterMaxRetries() {
            // Given
            OutboxEvent event = createOutboxEvent(1L, "evt-1");
            event.setRetryCount(MAX_RETRIES);
            when(outboxRepository.findPendingEvents(BATCH_SIZE))
                .thenReturn(Flux.just(event));
            when(kafkaPublisher.send(any(OutboxEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("Kafka unavailable")));
            when(outboxRepository.markAsFailed(eq(1L), anyString()))
                .thenReturn(Mono.just(1));

            // When & Then
            StepVerifier.create(outboxPublisher.publishPendingEvents())
                .verifyComplete();

            verify(outboxRepository).markAsFailed(eq(1L), anyString());
            verify(outboxRepository, never()).incrementRetryCount(anyLong());
        }

        @Test
        @DisplayName("should continue processing other events when one fails")
        void shouldContinueOnIndividualEventError() {
            // Given
            OutboxEvent event1 = createOutboxEvent(1L, "evt-1");
            OutboxEvent event2 = createOutboxEvent(2L, "evt-2");
            OutboxEvent event3 = createOutboxEvent(3L, "evt-3");

            when(outboxRepository.findPendingEvents(BATCH_SIZE))
                .thenReturn(Flux.just(event1, event2, event3));

            when(kafkaPublisher.send(event1)).thenReturn(Mono.empty());
            when(kafkaPublisher.send(event2))
                .thenReturn(Mono.error(new RuntimeException("Kafka error")));
            when(kafkaPublisher.send(event3)).thenReturn(Mono.empty());

            when(outboxRepository.markAsPublished(1L)).thenReturn(Mono.just(1));
            when(outboxRepository.markAsPublished(3L)).thenReturn(Mono.just(1));
            when(outboxRepository.incrementRetryCount(2L)).thenReturn(Mono.just(1));

            // When & Then
            StepVerifier.create(outboxPublisher.publishPendingEvents())
                .verifyComplete();

            verify(outboxRepository).markAsPublished(1L);
            verify(outboxRepository).incrementRetryCount(2L);
            verify(outboxRepository).markAsPublished(3L);
        }
    }

    private OutboxEvent createOutboxEvent(Long id, String eventId) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setEventId(eventId);
        event.setTenantId("tenant-a");
        event.setAggregateType("ORDER");
        event.setAggregateId("ord-123");
        event.setEventType("order.created");
        event.setPayload("{}");
        event.setStatus(OutboxStatus.PENDING.name());
        event.setRetryCount(0);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
