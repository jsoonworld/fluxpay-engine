package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

/**
 * Outbound port for publishing domain events.
 * Infrastructure layer provides implementation (e.g., Outbox pattern).
 */
public interface EventPublisher {

    /**
     * Publishes a domain event.
     * In the Outbox implementation, this stores the event in the outbox table
     * within the same transaction as the domain operation.
     *
     * @param event the domain event to publish
     * @return Mono completing when the event is stored
     */
    Mono<Void> publish(DomainEvent event);
}
