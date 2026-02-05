package com.fluxpay.engine.infrastructure.external.pg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TossPaymentsPropertiesTest {

    @Test
    @DisplayName("Should apply default values when nulls provided")
    void shouldApplyDefaultValues() {
        TossPaymentsProperties props = new TossPaymentsProperties(null, "secret_key", null, null);

        assertThat(props.apiUrl()).isEqualTo("https://api.tosspayments.com/v1");
        assertThat(props.secretKey()).isEqualTo("secret_key");
        assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("Should use custom values when provided")
    void shouldUseCustomValues() {
        Duration connectTimeout = Duration.ofSeconds(5);
        Duration readTimeout = Duration.ofSeconds(15);

        TossPaymentsProperties props = new TossPaymentsProperties(
            "https://custom.api.com",
            "custom_secret",
            connectTimeout,
            readTimeout
        );

        assertThat(props.apiUrl()).isEqualTo("https://custom.api.com");
        assertThat(props.secretKey()).isEqualTo("custom_secret");
        assertThat(props.connectTimeout()).isEqualTo(connectTimeout);
        assertThat(props.readTimeout()).isEqualTo(readTimeout);
    }

    @Test
    @DisplayName("Should apply default URL for blank string")
    void shouldApplyDefaultForBlankUrl() {
        TossPaymentsProperties props = new TossPaymentsProperties("  ", "secret", null, null);

        assertThat(props.apiUrl()).isEqualTo("https://api.tosspayments.com/v1");
    }
}
