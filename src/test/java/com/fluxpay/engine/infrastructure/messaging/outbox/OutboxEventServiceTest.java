package com.fluxpay.engine.infrastructure.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fluxpay.engine.domain.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventService")
class OutboxEventServiceTest {

    @Mock
    private OutboxEventR2dbcRepository outboxRepository;

    private CloudEventMapper cloudEventMapper;
    private OutboxEventService outboxEventService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        cloudEventMapper = new CloudEventMapper(objectMapper);
        outboxEventService = new OutboxEventService(outboxRepository, cloudEventMapper);
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("should save event to outbox with PENDING status")
        void shouldSaveEventToOutboxWithPendingStatus() {
            // Given
            TestDomainEvent event = new TestDomainEvent(
                "evt-123", "ord-456", Instant.now());
            when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.just(new OutboxEvent()));

            // When
            Mono<Void> result = outboxEventService.publish(event)
                .contextWrite(ctx -> ctx.put("tenantId", "tenant-a"));

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getEventId()).isEqualTo("evt-123");
            assertThat(saved.getAggregateType()).isEqualTo("ORDER");
            assertThat(saved.getAggregateId()).isEqualTo("ord-456");
            assertThat(saved.getEventType()).isEqualTo("order.created");
            assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING.name());
            assertThat(saved.getRetryCount()).isZero();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should include tenant ID from Reactor context")
        void shouldIncludeTenantIdFromContext() {
            // Given
            TestDomainEvent event = new TestDomainEvent(
                "evt-123", "ord-456", Instant.now());
            when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.just(new OutboxEvent()));

            // When
            Mono<Void> result = outboxEventService.publish(event)
                .contextWrite(ctx -> ctx.put("tenantId", "service-a"));

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo("service-a");
        }

        @Test
        @DisplayName("should convert event to CloudEvents JSON payload")
        void shouldConvertToCloudEventsPayload() {
            // Given
            TestDomainEvent event = new TestDomainEvent(
                "evt-123", "ord-456", Instant.now());
            when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.just(new OutboxEvent()));

            // When
            Mono<Void> result = outboxEventService.publish(event)
                .contextWrite(ctx -> ctx.put("tenantId", "tenant-a"));

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository).save(captor.capture());

            String payload = captor.getValue().getPayload();
            assertThat(payload).contains("\"specversion\":\"1.0\"");
            assertThat(payload).contains("\"source\":\"fluxpay-engine\"");
            assertThat(payload).contains("\"type\":\"com.fluxpay.order.created\"");
        }

        @Test
        @DisplayName("should use default tenant when context has no tenant")
        void shouldUseDefaultTenantWhenNoContext() {
            // Given
            TestDomainEvent event = new TestDomainEvent(
                "evt-123", "ord-456", Instant.now());
            when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.just(new OutboxEvent()));

            // When
            Mono<Void> result = outboxEventService.publish(event);

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo("__default__");
        }
    }

    // Test helper
    record TestDomainEvent(String eventId, String orderId, Instant occurredAt) implements DomainEvent {
        @Override public String aggregateType() { return "ORDER"; }
        @Override public String aggregateId() { return orderId; }
        @Override public String eventType() { return "order.created"; }
    }
}
