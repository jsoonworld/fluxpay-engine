package com.fluxpay.engine.infrastructure.external.pg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
    @JsonProperty("paymentKey") String paymentKey,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("orderName") String orderName,
    @JsonProperty("status") String status,
    @JsonProperty("totalAmount") BigDecimal totalAmount,
    @JsonProperty("method") String method,
    @JsonProperty("approvedAt") OffsetDateTime approvedAt,
    @JsonProperty("transactionKey") String transactionKey,
    @JsonProperty("receipt") Receipt receipt
) {
    public boolean isApproved() {
        return "DONE".equals(status);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(
        @JsonProperty("url") String url
    ) {}
}
