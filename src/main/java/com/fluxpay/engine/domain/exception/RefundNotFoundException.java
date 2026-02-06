package com.fluxpay.engine.domain.exception;

import com.fluxpay.engine.domain.model.refund.RefundId;

/**
 * Exception thrown when a refund is not found.
 */
public class RefundNotFoundException extends RuntimeException {

    private final RefundId refundId;

    public RefundNotFoundException(RefundId refundId) {
        super("Refund not found: " + refundId);
        this.refundId = refundId;
    }

    public RefundNotFoundException(String message) {
        super(message);
        this.refundId = null;
    }

    public RefundId getRefundId() {
        return refundId;
    }
}
