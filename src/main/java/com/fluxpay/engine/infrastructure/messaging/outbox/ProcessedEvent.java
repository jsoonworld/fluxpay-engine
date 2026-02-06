package com.fluxpay.engine.infrastructure.messaging.outbox;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * R2DBC entity for the processed_events table.
 * Used by consumers to track already-processed events for idempotency.
 */
@Table("processed_events")
public class ProcessedEvent {

    @Id
    private Long id;

    @Column("event_id")
    private String eventId;

    @Column("processed_at")
    private Instant processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
