package com.fluxpay.engine.infrastructure.external.pg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelResponse(
    @JsonProperty("paymentKey") String paymentKey,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("status") String status,
    @JsonProperty("cancels") List<CancelInfo> cancels
) {
    public boolean isCancelled() {
        return "CANCELED".equals(status);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CancelInfo(
        @JsonProperty("cancelAmount") BigDecimal cancelAmount,
        @JsonProperty("cancelReason") String cancelReason,
        @JsonProperty("canceledAt") OffsetDateTime canceledAt,
        @JsonProperty("transactionKey") String transactionKey
    ) {}
}
