package com.fluxpay.engine.domain.event;

import java.time.Instant;

/**
 * Base interface for all domain events.
 * Domain events represent something important that happened in the domain.
 */
public interface DomainEvent {

    String eventId();

    Instant occurredAt();
}
