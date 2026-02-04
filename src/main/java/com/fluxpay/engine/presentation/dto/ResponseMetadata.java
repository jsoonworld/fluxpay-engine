package com.fluxpay.engine.presentation.dto;

import org.slf4j.MDC;
import java.time.Instant;

public record ResponseMetadata(Instant timestamp, String traceId) {
    public static ResponseMetadata now() {
        return new ResponseMetadata(Instant.now(), MDC.get("traceId"));
    }
}
