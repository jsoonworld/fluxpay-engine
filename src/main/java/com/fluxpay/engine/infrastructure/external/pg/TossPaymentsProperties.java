package com.fluxpay.engine.infrastructure.external.pg;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fluxpay.pg.toss")
public record TossPaymentsProperties(
    String apiUrl,
    String secretKey,
    Duration connectTimeout,
    Duration readTimeout
) {
    public TossPaymentsProperties {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://api.tosspayments.com/v1";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
    }
}
