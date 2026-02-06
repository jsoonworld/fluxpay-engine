package com.fluxpay.engine.domain.exception;

import com.fluxpay.engine.domain.model.payment.PaymentId;

/**
 * Exception thrown when a refund request is invalid.
 */
public class InvalidRefundException extends RuntimeException {

    private final PaymentId paymentId;
    private final String reason;

    public InvalidRefundException(PaymentId paymentId, String reason) {
        super(String.format("Invalid refund for payment %s: %s", paymentId, reason));
        this.paymentId = paymentId;
        this.reason = reason;
    }

    public InvalidRefundException(String message) {
        super(message);
        this.paymentId = null;
        this.reason = message;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public String getReason() {
        return reason;
    }
}
