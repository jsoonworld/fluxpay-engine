package com.fluxpay.engine.domain.exception;

import com.fluxpay.engine.domain.model.payment.PaymentId;

/**
 * Exception thrown when a refund request is invalid.
 */
public class InvalidRefundException extends RuntimeException {

    private final PaymentId paymentId;
    private final String reason;
    private final String errorCode;

    public InvalidRefundException(PaymentId paymentId, String reason, String errorCode) {
        super(String.format("Invalid refund for payment %s: %s", paymentId, reason));
        this.paymentId = paymentId;
        this.reason = reason;
        this.errorCode = errorCode;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public String getReason() {
        return reason;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
