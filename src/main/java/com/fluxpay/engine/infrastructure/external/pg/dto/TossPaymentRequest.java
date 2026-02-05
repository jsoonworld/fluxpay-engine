package com.fluxpay.engine.infrastructure.external.pg.dto;

import java.math.BigDecimal;

public record TossPaymentRequest(
    String paymentKey,
    String orderId,
    BigDecimal amount
) {
    public static TossPaymentRequest of(String paymentKey, String orderId, BigDecimal amount) {
        return new TossPaymentRequest(paymentKey, orderId, amount);
    }
}
