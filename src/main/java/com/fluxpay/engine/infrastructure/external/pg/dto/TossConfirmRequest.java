package com.fluxpay.engine.infrastructure.external.pg.dto;

import java.math.BigDecimal;

public record TossConfirmRequest(
    String paymentKey,
    String orderId,
    BigDecimal amount
) {
    public static TossConfirmRequest of(String paymentKey, String orderId, BigDecimal amount) {
        return new TossConfirmRequest(paymentKey, orderId, amount);
    }
}
