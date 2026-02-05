-- Payment table for storing payment transactions
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
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
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    -- Foreign key constraint to orders table
    CONSTRAINT fk_payments_order_id
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT,

    -- Ensure valid status values
    CONSTRAINT chk_payment_status
        CHECK (status IN ('READY', 'PROCESSING', 'APPROVED', 'CONFIRMED', 'FAILED', 'REFUNDED')),

    -- Ensure valid currency values
    CONSTRAINT chk_payment_currency
        CHECK (currency IN ('KRW', 'USD', 'EUR', 'JPY')),

    -- Ensure positive amount
    CONSTRAINT chk_payment_amount_positive
        CHECK (amount > 0)
);

-- Indexes for common queries
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_pg_payment_key ON payments(pg_payment_key) WHERE pg_payment_key IS NOT NULL;
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- Unique constraint: one payment per order (can be modified if multiple payments per order needed)
CREATE UNIQUE INDEX idx_payments_order_id_unique ON payments(order_id);

-- Comment
COMMENT ON TABLE payments IS 'Payment transactions linked to orders';
