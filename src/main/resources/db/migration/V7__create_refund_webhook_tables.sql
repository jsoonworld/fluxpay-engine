-- V7: Create refunds and webhooks tables for payment enhancement
-- This migration creates tables for refund tracking and webhook management

-- ==============================================================================
-- 1. Create refunds table
-- ==============================================================================

CREATE TABLE refunds (
    id              BIGSERIAL PRIMARY KEY,
    refund_id       VARCHAR(50) NOT NULL UNIQUE,
    tenant_id       VARCHAR(50) NOT NULL,
    payment_id      VARCHAR(50) NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    reason          VARCHAR(500),
    status          VARCHAR(20) NOT NULL,  -- REQUESTED, PROCESSING, COMPLETED, FAILED
    pg_refund_id    VARCHAR(100),          -- PG refund transaction ID
    requested_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,

    CONSTRAINT ck_refund_status CHECK (status IN ('REQUESTED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_refund_amount CHECK (amount > 0)
);

-- ==============================================================================
-- 2. Create webhooks table
-- ==============================================================================

CREATE TABLE webhooks (
    id              BIGSERIAL PRIMARY KEY,
    webhook_id      VARCHAR(50) NOT NULL UNIQUE,
    tenant_id       VARCHAR(50) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,       -- payment.confirmed, refund.completed
    payload         TEXT NOT NULL,               -- JSON payload (stored as TEXT for R2DBC compatibility)
    target_url      VARCHAR(500) NOT NULL,
    status          VARCHAR(20) NOT NULL,        -- PENDING, DELIVERED, FAILED
    retry_count     INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    delivered_at    TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT ck_webhook_status CHECK (status IN ('PENDING', 'SENDING', 'DELIVERED', 'FAILED', 'RETRYING'))
);

-- ==============================================================================
-- 3. Create webhook_secrets table
-- ==============================================================================

CREATE TABLE webhook_secrets (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   VARCHAR(50) NOT NULL UNIQUE,
    secret_key  VARCHAR(100) NOT NULL,          -- HMAC signing secret
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    rotated_at  TIMESTAMP WITH TIME ZONE
);

-- ==============================================================================
-- 4. Create indexes for efficient lookups
-- ==============================================================================

-- Refunds indexes
CREATE INDEX idx_refunds_payment ON refunds (payment_id);
CREATE INDEX idx_refunds_tenant ON refunds (tenant_id, requested_at);
CREATE INDEX idx_refunds_status ON refunds (status);

-- Webhooks indexes
CREATE INDEX idx_webhooks_status ON webhooks (status, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_webhooks_tenant ON webhooks (tenant_id, created_at);

-- ==============================================================================
-- 5. Enable Row Level Security (RLS)
-- ==============================================================================

ALTER TABLE refunds ENABLE ROW LEVEL SECURITY;
ALTER TABLE refunds FORCE ROW LEVEL SECURITY;

ALTER TABLE webhooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhooks FORCE ROW LEVEL SECURITY;

ALTER TABLE webhook_secrets ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhook_secrets FORCE ROW LEVEL SECURITY;

-- ==============================================================================
-- 6. Create tenant isolation policies
-- ==============================================================================

-- Policy for refunds
CREATE POLICY tenant_isolation_refunds ON refunds
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'));

-- Policy for webhooks
CREATE POLICY tenant_isolation_webhooks ON webhooks
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'));

-- Policy for webhook_secrets
CREATE POLICY tenant_isolation_secrets ON webhook_secrets
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'));

-- ==============================================================================
-- 7. Grant permissions to admin role
-- ==============================================================================

GRANT ALL ON refunds TO fluxpay_admin;
GRANT USAGE, SELECT ON SEQUENCE refunds_id_seq TO fluxpay_admin;

GRANT ALL ON webhooks TO fluxpay_admin;
GRANT USAGE, SELECT ON SEQUENCE webhooks_id_seq TO fluxpay_admin;

GRANT ALL ON webhook_secrets TO fluxpay_admin;
GRANT USAGE, SELECT ON SEQUENCE webhook_secrets_id_seq TO fluxpay_admin;

-- ==============================================================================
-- 8. Add comments for documentation
-- ==============================================================================

-- Refunds table comments
COMMENT ON TABLE refunds IS 'Stores refund requests and their processing status';
COMMENT ON COLUMN refunds.refund_id IS 'Unique refund identifier (ref_xxx format)';
COMMENT ON COLUMN refunds.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN refunds.payment_id IS 'Reference to the payment being refunded';
COMMENT ON COLUMN refunds.amount IS 'Refund amount';
COMMENT ON COLUMN refunds.currency IS 'Currency code (KRW, USD, etc.)';
COMMENT ON COLUMN refunds.reason IS 'Reason for the refund (optional)';
COMMENT ON COLUMN refunds.status IS 'Refund status: REQUESTED, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN refunds.pg_refund_id IS 'PG refund transaction ID';
COMMENT ON COLUMN refunds.requested_at IS 'When the refund was requested';
COMMENT ON COLUMN refunds.completed_at IS 'When the refund was completed';
COMMENT ON COLUMN refunds.error_message IS 'Error message if refund failed';

-- Webhooks table comments
COMMENT ON TABLE webhooks IS 'Stores outbound webhook delivery attempts';
COMMENT ON COLUMN webhooks.webhook_id IS 'Unique webhook delivery identifier';
COMMENT ON COLUMN webhooks.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN webhooks.event_type IS 'Event type (e.g., payment.confirmed, refund.completed)';
COMMENT ON COLUMN webhooks.payload IS 'CloudEvent JSON payload';
COMMENT ON COLUMN webhooks.target_url IS 'Target URL to deliver webhook to';
COMMENT ON COLUMN webhooks.status IS 'Delivery status: PENDING, DELIVERED, FAILED';
COMMENT ON COLUMN webhooks.retry_count IS 'Number of retry attempts';
COMMENT ON COLUMN webhooks.last_attempt_at IS 'Timestamp of last delivery attempt';
COMMENT ON COLUMN webhooks.delivered_at IS 'Timestamp when webhook was delivered';
COMMENT ON COLUMN webhooks.error_message IS 'Error message if delivery failed';
COMMENT ON COLUMN webhooks.created_at IS 'When the webhook was created';

-- Webhook secrets table comments
COMMENT ON TABLE webhook_secrets IS 'Stores webhook signing secrets per tenant';
COMMENT ON COLUMN webhook_secrets.tenant_id IS 'Tenant identifier (unique)';
COMMENT ON COLUMN webhook_secrets.secret_key IS 'HMAC-SHA256 signing secret';
COMMENT ON COLUMN webhook_secrets.created_at IS 'When the secret was created';
COMMENT ON COLUMN webhook_secrets.rotated_at IS 'When the secret was last rotated';
