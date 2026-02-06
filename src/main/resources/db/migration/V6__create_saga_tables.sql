-- V6: Create saga tables for distributed transaction management
-- This migration creates tables to track saga instances and their steps

-- ==============================================================================
-- 1. Create saga_instances table
-- ==============================================================================

CREATE TABLE saga_instances (
    id              BIGSERIAL PRIMARY KEY,
    saga_id         VARCHAR(50) NOT NULL UNIQUE,
    saga_type       VARCHAR(50) NOT NULL,           -- e.g., PAYMENT_SAGA
    correlation_id  VARCHAR(100) NOT NULL,          -- External request ID (idempotency key)
    tenant_id       VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL,           -- STARTED, PROCESSING, COMPLETED, etc.
    current_step    INT NOT NULL DEFAULT 0,
    context_data    TEXT,                           -- SagaContext serialized as JSON
    error_message   TEXT,                           -- Error details if failed
    started_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Unique constraint: one saga per correlation ID per tenant
    CONSTRAINT uk_saga_correlation UNIQUE (tenant_id, correlation_id)
);

-- ==============================================================================
-- 2. Create saga_steps table
-- ==============================================================================

CREATE TABLE saga_steps (
    id              BIGSERIAL PRIMARY KEY,
    saga_id         VARCHAR(50) NOT NULL REFERENCES saga_instances(saga_id) ON DELETE CASCADE,
    step_order      INT NOT NULL,
    step_name       VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL,           -- PENDING, COMPLETED, FAILED, COMPENSATED
    executed_at     TIMESTAMP WITH TIME ZONE,
    compensated_at  TIMESTAMP WITH TIME ZONE,
    error_message   TEXT,
    step_data       TEXT,                           -- Step-specific data as JSON

    -- Unique constraint: one step per order per saga
    CONSTRAINT uk_saga_step UNIQUE (saga_id, step_order)
);

-- ==============================================================================
-- 3. Create indexes for efficient lookups
-- ==============================================================================

-- Index for finding sagas by status (for recovery and monitoring)
CREATE INDEX idx_saga_status ON saga_instances (status);

-- Index for tenant-based lookups with time range
CREATE INDEX idx_saga_tenant ON saga_instances (tenant_id, started_at);

-- Index for correlation ID lookups
CREATE INDEX idx_saga_correlation ON saga_instances (tenant_id, correlation_id);

-- Index for saga step lookups
CREATE INDEX idx_saga_steps_saga ON saga_steps (saga_id);

-- ==============================================================================
-- 4. Enable Row Level Security (RLS)
-- ==============================================================================

ALTER TABLE saga_instances ENABLE ROW LEVEL SECURITY;
ALTER TABLE saga_instances FORCE ROW LEVEL SECURITY;

ALTER TABLE saga_steps ENABLE ROW LEVEL SECURITY;
ALTER TABLE saga_steps FORCE ROW LEVEL SECURITY;

-- ==============================================================================
-- 5. Create tenant isolation policies
-- ==============================================================================

-- Policy for saga_instances
CREATE POLICY tenant_isolation_saga ON saga_instances
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__'));

-- Policy for saga_steps (based on parent saga's tenant)
CREATE POLICY tenant_isolation_steps ON saga_steps
    FOR ALL
    USING (saga_id IN (
        SELECT saga_id FROM saga_instances
        WHERE tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__')
    ))
    WITH CHECK (saga_id IN (
        SELECT saga_id FROM saga_instances
        WHERE tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__default__')
    ));

-- ==============================================================================
-- 6. Grant permissions to admin role
-- ==============================================================================

GRANT ALL ON saga_instances TO fluxpay_admin;
GRANT USAGE, SELECT ON SEQUENCE saga_instances_id_seq TO fluxpay_admin;

GRANT ALL ON saga_steps TO fluxpay_admin;
GRANT USAGE, SELECT ON SEQUENCE saga_steps_id_seq TO fluxpay_admin;

-- ==============================================================================
-- 7. Add comments for documentation
-- ==============================================================================

COMMENT ON TABLE saga_instances IS 'Tracks saga instances for distributed transaction management';
COMMENT ON COLUMN saga_instances.saga_id IS 'Unique identifier for the saga instance';
COMMENT ON COLUMN saga_instances.saga_type IS 'Type of saga (e.g., PAYMENT_SAGA)';
COMMENT ON COLUMN saga_instances.correlation_id IS 'External correlation ID for idempotency';
COMMENT ON COLUMN saga_instances.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN saga_instances.status IS 'Current status: STARTED, PROCESSING, COMPLETED, COMPENSATING, COMPENSATED, FAILED';
COMMENT ON COLUMN saga_instances.current_step IS 'Index of the current step being executed';
COMMENT ON COLUMN saga_instances.context_data IS 'Serialized SagaContext as JSON';
COMMENT ON COLUMN saga_instances.error_message IS 'Error details if saga failed';
COMMENT ON COLUMN saga_instances.started_at IS 'When the saga started';
COMMENT ON COLUMN saga_instances.completed_at IS 'When the saga completed (success or failure)';
COMMENT ON COLUMN saga_instances.updated_at IS 'Last update timestamp';

COMMENT ON TABLE saga_steps IS 'Tracks individual steps within a saga';
COMMENT ON COLUMN saga_steps.saga_id IS 'Reference to parent saga instance';
COMMENT ON COLUMN saga_steps.step_order IS 'Execution order of this step (0-based)';
COMMENT ON COLUMN saga_steps.step_name IS 'Name of the step (e.g., CREATE_ORDER)';
COMMENT ON COLUMN saga_steps.status IS 'Step status: PENDING, COMPLETED, FAILED, COMPENSATED';
COMMENT ON COLUMN saga_steps.executed_at IS 'When the step was executed';
COMMENT ON COLUMN saga_steps.compensated_at IS 'When the step was compensated (if applicable)';
COMMENT ON COLUMN saga_steps.error_message IS 'Error details if step failed';
COMMENT ON COLUMN saga_steps.step_data IS 'Step-specific result data as JSON';

-- ==============================================================================
-- Usage Instructions:
-- ==============================================================================
-- Saga lifecycle:
--   1. Create saga_instance with status=STARTED
--   2. Create saga_steps with status=PENDING for all steps
--   3. Update saga status to PROCESSING when first step begins
--   4. Update each step status to COMPLETED/FAILED as execution progresses
--   5. On success: Update saga status to COMPLETED
--   6. On failure: Update saga status to COMPENSATING, then run compensations
--   7. On compensation success: Update saga status to COMPENSATED
--   8. On compensation failure: Update saga status to FAILED (manual intervention required)
--
-- Recovery:
--   - Query for sagas with status=PROCESSING or status=COMPENSATING
--   - Resume or compensate based on step statuses
-- ==============================================================================
