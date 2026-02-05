package com.fluxpay.engine.domain.exception;

import com.fluxpay.engine.domain.model.payment.PaymentId;

public class PaymentNotFoundException extends RuntimeException {

    private final String paymentId;

    public PaymentNotFoundException(PaymentId paymentId) {
        super("Payment not found: " + paymentId);
        this.paymentId = paymentId.toString();
    }

    public PaymentNotFoundException(String message) {
        super(message);
        this.paymentId = null;
    }

    public String getPaymentId() {
        return paymentId;
    }
}
