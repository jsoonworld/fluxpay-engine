package com.fluxpay.engine.domain.model.refund;

/**
 * Exception thrown when an invalid refund state transition is attempted.
 */
public class InvalidRefundStateException extends RuntimeException {

    private final RefundStatus currentStatus;
    private final RefundStatus targetStatus;

    public InvalidRefundStateException(RefundStatus currentStatus, RefundStatus targetStatus) {
        super(String.format("Invalid refund state transition from %s to %s",
            currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidRefundStateException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public RefundStatus getCurrentStatus() {
        return currentStatus;
    }

    public RefundStatus getTargetStatus() {
        return targetStatus;
    }
}
