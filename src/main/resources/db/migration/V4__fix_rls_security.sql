-- V4: Fix RLS security vulnerabilities
-- This migration addresses critical security issues from V3:
-- 1. Removes dangerous bypass policies that allow ALL access when tenant is not set
-- 2. Adds FORCE ROW LEVEL SECURITY to prevent table owner bypass
-- 3. Creates a proper admin role for migrations and admin operations

-- ==============================================================================
-- 1. Drop dangerous bypass policies
-- ==============================================================================
-- These policies allowed ALL access when app.tenant_id was NULL or empty,
-- which defeats the purpose of RLS (tenant isolation)

DROP POLICY IF EXISTS bypass_rls_when_no_tenant_orders ON orders;
DROP POLICY IF EXISTS bypass_rls_when_no_tenant_order_line_items ON order_line_items;
DROP POLICY IF EXISTS bypass_rls_when_no_tenant_payments ON payments;

-- ==============================================================================
-- 2. Enable FORCE ROW LEVEL SECURITY
-- ==============================================================================
-- FORCE ensures RLS is applied even to table owners (except superusers)
-- This prevents accidental data leaks from admin connections

ALTER TABLE orders FORCE ROW LEVEL SECURITY;
ALTER TABLE order_line_items FORCE ROW LEVEL SECURITY;
ALTER TABLE payments FORCE ROW LEVEL SECURITY;

-- ==============================================================================
-- 3. Update tenant isolation policies to be stricter
-- ==============================================================================
-- The original policies used current_setting('app.tenant_id', true) which returns
-- NULL if not set. We need to ensure that:
-- - If tenant_id is NOT set, NO rows are visible (fail-safe)
-- - Only matching tenant_id rows are accessible

-- Drop existing policies
DROP POLICY IF EXISTS tenant_isolation_orders ON orders;
DROP POLICY IF EXISTS tenant_isolation_order_line_items ON order_line_items;
DROP POLICY IF EXISTS tenant_isolation_payments ON payments;

-- Recreate stricter policies:
-- COALESCE ensures NULL tenant_id setting returns empty string,
-- which won't match any real tenant_id (fail-safe)
CREATE POLICY tenant_isolation_orders ON orders
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'));

CREATE POLICY tenant_isolation_order_line_items ON order_line_items
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'));

CREATE POLICY tenant_isolation_payments ON payments
    FOR ALL
    USING (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'))
    WITH CHECK (tenant_id = COALESCE(NULLIF(current_setting('app.tenant_id', true), ''), '__no_tenant__'));

-- ==============================================================================
-- 4. Create admin role for migrations and administrative operations
-- ==============================================================================
-- Note: This role should be used ONLY for migrations and admin tasks.
-- Application connections should use a separate role without BYPASSRLS.

-- Create admin role if it doesn't exist (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fluxpay_admin') THEN
        CREATE ROLE fluxpay_admin WITH LOGIN BYPASSRLS;
        COMMENT ON ROLE fluxpay_admin IS 'Admin role for migrations and administrative operations - bypasses RLS';
    END IF;
END
$$;

-- Grant necessary permissions to admin role
GRANT ALL ON orders TO fluxpay_admin;
GRANT ALL ON order_line_items TO fluxpay_admin;
GRANT ALL ON payments TO fluxpay_admin;

-- ==============================================================================
-- Usage Instructions:
-- ==============================================================================
-- For normal application operations:
--   1. Set app.tenant_id before any queries: SET app.tenant_id = 'tenant-123';
--   2. RLS will automatically filter rows by tenant_id
--   3. If tenant_id is not set, NO data will be accessible (fail-safe)
--
-- For migrations and admin operations:
--   1. Connect as fluxpay_admin role
--   2. This role bypasses RLS and can see all data
--
-- Security considerations:
--   - Never use fluxpay_admin for application connections
--   - Store admin credentials separately from application credentials
--   - Monitor admin role usage for unauthorized access
-- ==============================================================================
