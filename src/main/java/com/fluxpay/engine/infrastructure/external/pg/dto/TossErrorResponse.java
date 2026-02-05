package com.fluxpay.engine.infrastructure.external.pg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossErrorResponse(
    @JsonProperty("code") String code,
    @JsonProperty("message") String message
) {
    public String toErrorMessage() {
        return String.format("[%s] %s", code, message);
    }
}
