-- V3: Add multi-tenancy support with Row Level Security (RLS)
-- This migration adds tenant_id columns to all tables and enables RLS for tenant isolation

-- ==============================================================================
-- 1. Add tenant_id columns to all tables
-- ==============================================================================

-- Add tenant_id to orders table
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';

-- Add tenant_id to order_line_items table
ALTER TABLE order_line_items
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';

-- Add tenant_id to payments table
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50) NOT NULL DEFAULT 'default';

-- ==============================================================================
-- 2. Create indexes for tenant_id columns
-- ==============================================================================

-- Index for orders table
CREATE INDEX IF NOT EXISTS idx_orders_tenant_id ON orders (tenant_id);

-- Composite index for common tenant-specific queries on orders
CREATE INDEX IF NOT EXISTS idx_orders_tenant_status ON orders (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_created_at ON orders (tenant_id, created_at);

-- Index for order_line_items table
CREATE INDEX IF NOT EXISTS idx_order_line_items_tenant_id ON order_line_items (tenant_id);

-- Index for payments table
CREATE INDEX IF NOT EXISTS idx_payments_tenant_id ON payments (tenant_id);

-- Composite index for common tenant-specific queries on payments
CREATE INDEX IF NOT EXISTS idx_payments_tenant_status ON payments (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_created_at ON payments (tenant_id, created_at);

-- ==============================================================================
-- 3. Enable Row Level Security (RLS) on all tables
-- ==============================================================================

-- Enable RLS on orders table
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- Enable RLS on order_line_items table
ALTER TABLE order_line_items ENABLE ROW LEVEL SECURITY;

-- Enable RLS on payments table
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

-- ==============================================================================
-- 4. Create tenant isolation policies
-- ==============================================================================

-- Policy for orders table
-- Uses the session variable 'app.tenant_id' for tenant filtering
-- The 'true' parameter in current_setting makes it return NULL instead of error if not set
DROP POLICY IF EXISTS tenant_isolation_orders ON orders;
CREATE POLICY tenant_isolation_orders ON orders
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true));

-- Policy for order_line_items table
DROP POLICY IF EXISTS tenant_isolation_order_line_items ON order_line_items;
CREATE POLICY tenant_isolation_order_line_items ON order_line_items
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true));

-- Policy for payments table
DROP POLICY IF EXISTS tenant_isolation_payments ON payments;
CREATE POLICY tenant_isolation_payments ON payments
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true));

-- ==============================================================================
-- 5. Create bypass policy for superusers and migration tasks
-- ==============================================================================

-- Allow superusers to bypass RLS (useful for admin operations and migrations)
-- Note: This is automatically handled by PostgreSQL for superusers, but we add
-- explicit policies for roles that need full access

-- Create a policy that allows access when no tenant_id is set (e.g., for migrations)
DROP POLICY IF EXISTS bypass_rls_when_no_tenant_orders ON orders;
CREATE POLICY bypass_rls_when_no_tenant_orders ON orders
    FOR ALL
    USING (current_setting('app.tenant_id', true) IS NULL OR current_setting('app.tenant_id', true) = '')
    WITH CHECK (current_setting('app.tenant_id', true) IS NULL OR current_setting('app.tenant_id', true) = '');

DROP POLICY IF EXISTS bypass_rls_when_no_tenant_order_line_items ON order_line_items;
CREATE POLICY bypass_rls_when_no_tenant_order_line_items ON order_line_items
    FOR ALL
    USING (current_setting('app.tenant_id', true) IS NULL OR current_setting('app.tenant_id', true) = '')
    WITH CHECK (current_setting('app.tenant_id', true) IS NULL OR current_setting('app.tenant_id', true) = '');

DROP POLICY IF EXISTS bypass_rls_when_no_tenant_payments ON payments;
CREATE POLICY bypass_rls_when_no_tenant_payments ON payments
    FOR ALL
    USING (current_setting('app.tenant_id', true) IS NULL OR current_setting('app.tenant_id', true) = '')
    WITH CHECK (current_setting('app.tenant_id', true) IS NULL OR current_setting('app.tenant_id', true) = '');

-- ==============================================================================
-- 6. Add comments for documentation
-- ==============================================================================

COMMENT ON COLUMN orders.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN order_line_items.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN payments.tenant_id IS 'Tenant identifier for multi-tenancy isolation';

-- ==============================================================================
-- Usage Instructions:
-- ==============================================================================
-- To use RLS, set the tenant_id before executing queries:
--   SET app.tenant_id = 'tenant-123';
--
-- In Spring Boot with R2DBC, use connection initialization:
--   ConnectionFactoryOptions.builder()
--       .option(Option.valueOf("options"), "app.tenant_id=tenant-123")
--       ...
--
-- Or use a TenantConnectionFactory that sets the tenant context per request.
-- ==============================================================================
