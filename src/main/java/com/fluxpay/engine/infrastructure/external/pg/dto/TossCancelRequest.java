package com.fluxpay.engine.infrastructure.external.pg.dto;

public record TossCancelRequest(
    String cancelReason
) {
    public static TossCancelRequest of(String reason) {
        return new TossCancelRequest(reason);
    }
}
