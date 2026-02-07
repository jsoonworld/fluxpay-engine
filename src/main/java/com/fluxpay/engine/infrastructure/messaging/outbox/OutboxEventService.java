package com.fluxpay.engine.infrastructure.messaging.outbox;

import com.fluxpay.engine.domain.event.DomainEvent;
import com.fluxpay.engine.domain.port.outbound.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Implements EventPublisher by storing domain events in the outbox table.
 * Events are saved within the same database transaction as the domain operation,
 * ensuring atomicity between state changes and event publication.
 */
@Service
public class OutboxEventService implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventService.class);
    private static final String DEFAULT_TENANT = "__default__";

    private final OutboxEventR2dbcRepository outboxRepository;
    private final CloudEventMapper cloudEventMapper;

    public OutboxEventService(OutboxEventR2dbcRepository outboxRepository,
                              CloudEventMapper cloudEventMapper) {
        this.outboxRepository = outboxRepository;
        this.cloudEventMapper = cloudEventMapper;
    }

    @Override
    public Mono<Void> publish(DomainEvent event) {
        Objects.requireNonNull(event, "event is required");
        return Mono.deferContextual(ctx -> {
            String tenantId = ctx.getOrDefault("tenantId", DEFAULT_TENANT);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventId(event.eventId());
            outboxEvent.setTenantId(tenantId);
            outboxEvent.setAggregateType(event.aggregateType());
            outboxEvent.setAggregateId(event.aggregateId());
            outboxEvent.setEventType(event.eventType());
            outboxEvent.setPayload(cloudEventMapper.toJson(event, tenantId));
            outboxEvent.setStatus(OutboxStatus.PENDING.name());
            outboxEvent.setRetryCount(0);
            outboxEvent.setCreatedAt(Instant.now());

            log.debug("Saving outbox event: eventId={}, type={}, aggregate={}:{}",
                event.eventId(), event.eventType(), event.aggregateType(), event.aggregateId());

            return outboxRepository.save(outboxEvent)
                .doOnSuccess(saved -> log.debug("Outbox event saved: eventId={}", event.eventId()))
                .doOnError(error -> log.error("Failed to save outbox event: eventId={}",
                    event.eventId(), error))
                .then();
        });
    }
}
