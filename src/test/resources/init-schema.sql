-- FluxPay Engine - Test Schema for Testcontainers
-- This schema mirrors production constraints for accurate testing

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT '__default__',
    paid_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Order Line Items Table
CREATE TABLE IF NOT EXISTS order_line_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    total_price DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT '__default__'
);

-- Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method_type VARCHAR(30),
    payment_method_display_name VARCHAR(100),
    pg_transaction_id VARCHAR(100),
    pg_payment_key VARCHAR(100),
    failure_reason TEXT,
    approved_at TIMESTAMP WITH TIME ZONE,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id VARCHAR(50) NOT NULL DEFAULT '__default__',
    version BIGINT DEFAULT 0,

    CONSTRAINT chk_payment_status CHECK (status IN ('READY', 'PROCESSING', 'APPROVED', 'CONFIRMED', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payment_currency CHECK (currency IN ('KRW', 'USD', 'EUR', 'JPY')),
    CONSTRAINT chk_payment_amount_positive CHECK (amount > 0)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_line_items_order_id ON order_line_items(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_pg_payment_key ON payments(pg_payment_key) WHERE pg_payment_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);

-- Unique constraint: one payment per order (policy: single payment per order)
CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_order_id_unique ON payments(order_id);

-- Idempotency Keys Table
-- Note: Using TEXT for response to match production schema (V5).
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL,
    endpoint        VARCHAR(200) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload_hash    VARCHAR(64) NOT NULL,
    response        TEXT NOT NULL,
    http_status     INT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_idempotency UNIQUE (tenant_id, endpoint, idempotency_key)
);

-- Idempotency Indexes
CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_keys (expires_at);
CREATE INDEX IF NOT EXISTS idx_idempotency_tenant_id ON idempotency_keys (tenant_id);

-- Saga Instances Table
CREATE TABLE IF NOT EXISTS saga_instances (
    id              BIGSERIAL PRIMARY KEY,
    saga_id         VARCHAR(50) NOT NULL UNIQUE,
    saga_type       VARCHAR(50) NOT NULL,
    correlation_id  VARCHAR(100) NOT NULL,
    tenant_id       VARCHAR(50) NOT NULL DEFAULT '__default__',
    status          VARCHAR(20) NOT NULL,
    current_step    INT NOT NULL DEFAULT 0,
    context_data    TEXT,
    error_message   TEXT,
    started_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT uk_saga_correlation UNIQUE (tenant_id, correlation_id)
);

-- Saga Steps Table
CREATE TABLE IF NOT EXISTS saga_steps (
    id              BIGSERIAL PRIMARY KEY,
    saga_id         VARCHAR(50) NOT NULL REFERENCES saga_instances(saga_id) ON DELETE CASCADE,
    step_order      INT NOT NULL,
    step_name       VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    executed_at     TIMESTAMP WITH TIME ZONE,
    compensated_at  TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,
    step_data       TEXT,

    CONSTRAINT uk_saga_step UNIQUE (saga_id, step_order)
);

-- Saga Indexes
CREATE INDEX IF NOT EXISTS idx_saga_status ON saga_instances (status);
CREATE INDEX IF NOT EXISTS idx_saga_tenant ON saga_instances (tenant_id, started_at);
CREATE INDEX IF NOT EXISTS idx_saga_correlation ON saga_instances (tenant_id, correlation_id);
CREATE INDEX IF NOT EXISTS idx_saga_steps_saga ON saga_steps (saga_id);

-- Refunds Table
CREATE TABLE IF NOT EXISTS refunds (
    id              BIGSERIAL PRIMARY KEY,
    refund_id       VARCHAR(50) NOT NULL UNIQUE,
    tenant_id       VARCHAR(50) NOT NULL DEFAULT '__default__',
    payment_id      VARCHAR(50) NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    reason          VARCHAR(500),
    status          VARCHAR(20) NOT NULL,
    pg_refund_id    VARCHAR(100),
    requested_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,

    CONSTRAINT ck_refund_status CHECK (status IN ('REQUESTED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_refund_amount CHECK (amount > 0)
);

-- Refunds Indexes
CREATE INDEX IF NOT EXISTS idx_refunds_payment ON refunds (payment_id);
CREATE INDEX IF NOT EXISTS idx_refunds_tenant ON refunds (tenant_id, requested_at);
CREATE INDEX IF NOT EXISTS idx_refunds_status ON refunds (status);

-- Webhooks Table
CREATE TABLE IF NOT EXISTS webhooks (
    id              BIGSERIAL PRIMARY KEY,
    webhook_id      VARCHAR(50) NOT NULL UNIQUE,
    tenant_id       VARCHAR(50) NOT NULL DEFAULT '__default__',
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    target_url      VARCHAR(500) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    retry_count     INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    delivered_at    TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT ck_webhook_status CHECK (status IN ('PENDING', 'SENDING', 'DELIVERED', 'FAILED', 'RETRYING'))
);

-- Webhooks Indexes
CREATE INDEX IF NOT EXISTS idx_webhooks_status ON webhooks (status, created_at);
CREATE INDEX IF NOT EXISTS idx_webhooks_tenant ON webhooks (tenant_id, created_at);

-- Webhook Secrets Table
CREATE TABLE IF NOT EXISTS webhook_secrets (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   VARCHAR(50) NOT NULL UNIQUE,
    secret_key  VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    rotated_at  TIMESTAMP WITH TIME ZONE
);

-- Outbox Events Table
CREATE TABLE IF NOT EXISTS outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(50) NOT NULL UNIQUE,
    tenant_id       VARCHAR(50) NOT NULL DEFAULT '__default__',
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    VARCHAR(50) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    published_at    TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,

    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_tenant ON outbox_events (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);

-- Processed Events Table (Consumer idempotency)
CREATE TABLE IF NOT EXISTS processed_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(50) NOT NULL UNIQUE,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_events_time ON processed_events (processed_at);
