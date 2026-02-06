-- Outbox Events Table for Transactional Outbox Pattern
CREATE TABLE outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(50) NOT NULL UNIQUE,
    tenant_id       VARCHAR(50) NOT NULL,
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    VARCHAR(50) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    published_at    TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,

    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

-- Pending events polling index (partial index for performance)
CREATE INDEX idx_outbox_pending ON outbox_events (status, created_at)
    WHERE status = 'PENDING';

-- Tenant-based query index
CREATE INDEX idx_outbox_tenant ON outbox_events (tenant_id, created_at);

-- Aggregate-based query index (debugging / event sourcing)
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);

-- Processed Events Table (Consumer-side idempotency)
CREATE TABLE processed_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(50) NOT NULL UNIQUE,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- TTL-based cleanup index
CREATE INDEX idx_processed_events_time ON processed_events (processed_at);

-- Note: No RLS on outbox_events - the outbox publisher is an infrastructure
-- component that must process events across all tenants. Tenant isolation is
-- enforced at the domain/application layer, not at the outbox infrastructure level.
