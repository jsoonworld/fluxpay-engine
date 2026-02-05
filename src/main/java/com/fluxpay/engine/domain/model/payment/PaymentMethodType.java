package com.fluxpay.engine.domain.model.payment;

/**
 * Enum representing supported payment method types.
 */
public enum PaymentMethodType {
    /**
     * Credit/debit card payment.
     */
    CARD,

    /**
     * Bank transfer payment.
     */
    BANK_TRANSFER,

    /**
     * Virtual account payment.
     */
    VIRTUAL_ACCOUNT,

    /**
     * Mobile carrier billing payment.
     */
    MOBILE,

    /**
     * Easy pay services (e.g., Kakao Pay, Naver Pay, Toss).
     */
    EASY_PAY
}
