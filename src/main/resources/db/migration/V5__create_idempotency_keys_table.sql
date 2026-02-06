-- V5: Create idempotency_keys table for idempotent API operations
-- This table stores request/response pairs to ensure idempotent operations

-- ==============================================================================
-- 1. Create idempotency_keys table
-- ==============================================================================

CREATE TABLE idempotency_keys (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL,
    endpoint        VARCHAR(200) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload_hash    VARCHAR(64) NOT NULL,  -- SHA-256 hash of request payload
    response        TEXT NOT NULL,          -- Cached JSON response body (stored as TEXT for R2DBC compatibility)
    http_status     INT NOT NULL,           -- HTTP status code of cached response
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Unique constraint: one idempotency key per tenant/endpoint combination
    CONSTRAINT uk_idempotency UNIQUE (tenant_id, endpoint, idempotency_key)
);

-- ==============================================================================
-- 2. Create indexes for efficient lookups and cleanup
-- ==============================================================================

-- Index for cleanup of expired keys (scheduled job)
CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);

-- Index for tenant-based lookups
CREATE INDEX idx_idempotency_tenant_id ON idempotency_keys (tenant_id);

-- ==============================================================================
-- 3. Enable Row Level Security (RLS)
-- ==============================================================================

ALTER TABLE idempotency_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_keys FORCE ROW LEVEL SECURITY;

-- ==============================================================================
-- 4. Create tenant isolation policy
-- ==============================================================================
-- Uses the same pattern as other tables for consistency

CREATE POLICY tenant_isolation_idempotency ON idempotency_keys
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'));

-- ==============================================================================
-- 5. Grant permissions to admin role
-- ==============================================================================

GRANT ALL ON idempotency_keys TO fluxpay_admin;
GRANT USAGE, SELECT ON SEQUENCE idempotency_keys_id_seq TO fluxpay_admin;

-- ==============================================================================
-- 6. Add comments for documentation
-- ==============================================================================

COMMENT ON TABLE idempotency_keys IS 'Stores idempotency keys for ensuring idempotent API operations';
COMMENT ON COLUMN idempotency_keys.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN idempotency_keys.endpoint IS 'API endpoint path (e.g., /api/v1/payments)';
COMMENT ON COLUMN idempotency_keys.idempotency_key IS 'Client-provided idempotency key (UUID recommended)';
COMMENT ON COLUMN idempotency_keys.payload_hash IS 'SHA-256 hash of request payload for conflict detection';
COMMENT ON COLUMN idempotency_keys.response IS 'Cached JSON response body';
COMMENT ON COLUMN idempotency_keys.http_status IS 'HTTP status code of the cached response';
COMMENT ON COLUMN idempotency_keys.created_at IS 'Timestamp when the idempotency key was created';
COMMENT ON COLUMN idempotency_keys.expires_at IS 'Timestamp when the idempotency key expires (default: 24 hours)';

-- ==============================================================================
-- Usage Instructions:
-- ==============================================================================
-- Idempotency keys are stored when a request is processed for the first time.
-- On subsequent requests with the same key:
--   1. If payload_hash matches: return cached response (HIT)
--   2. If payload_hash differs: return 422 Unprocessable Entity (CONFLICT)
--   3. If not found: process request normally (MISS)
--
-- TTL: Default 24 hours, configurable per endpoint
-- Cleanup: A scheduled job should periodically delete expired keys:
--   DELETE FROM idempotency_keys WHERE expires_at < NOW();
-- ==============================================================================
