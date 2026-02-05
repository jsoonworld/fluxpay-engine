package com.fluxpay.engine.domain.exception;

import com.fluxpay.engine.domain.model.payment.PaymentStatus;

/**
 * Exception thrown when an invalid payment state transition is attempted.
 */
public class InvalidPaymentStateException extends RuntimeException {

    private final PaymentStatus currentStatus;
    private final PaymentStatus targetStatus;

    public InvalidPaymentStateException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public InvalidPaymentStateException(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        super(String.format("Cannot transition payment from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public PaymentStatus getCurrentStatus() {
        return currentStatus;
    }

    public PaymentStatus getTargetStatus() {
        return targetStatus;
    }
}
