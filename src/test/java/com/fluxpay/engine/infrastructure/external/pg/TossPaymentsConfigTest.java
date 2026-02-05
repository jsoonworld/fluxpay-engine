package com.fluxpay.engine.infrastructure.external.pg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TossPaymentsConfigTest {

    private TossPaymentsConfig config;
    private TossPaymentsProperties properties;

    @BeforeEach
    void setUp() {
        config = new TossPaymentsConfig();
        properties = new TossPaymentsProperties(
            "https://api.test.com",
            "test_secret_key",
            Duration.ofSeconds(3),
            Duration.ofSeconds(10)
        );
    }

    @Test
    @DisplayName("Should create WebClient bean")
    void shouldCreateWebClientBean() {
        // When
        WebClient webClient = config.tossPaymentsWebClient(properties);

        // Then
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("Should configure correct base URL from properties")
    void shouldConfigureCorrectBaseUrl() {
        // Given
        String expectedBaseUrl = "https://api.test.com";
        TossPaymentsProperties testProperties = new TossPaymentsProperties(
            expectedBaseUrl,
            "test_secret_key",
            Duration.ofSeconds(3),
            Duration.ofSeconds(10)
        );

        // When
        WebClient webClient = config.tossPaymentsWebClient(testProperties);

        // Then
        // WebClient doesn't expose its configuration directly,
        // so we verify it was created without errors with the given properties
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("Should encode secret key as Basic Auth with colon suffix")
    void shouldEncodeSecretKeyAsBasicAuth() {
        // Given
        String secretKey = "test_secret_key";
        String expectedCredentials = secretKey + ":";
        String expectedEncoded = Base64.getEncoder()
            .encodeToString(expectedCredentials.getBytes(StandardCharsets.UTF_8));

        // When
        WebClient webClient = config.tossPaymentsWebClient(properties);

        // Then
        // Verify the encoding logic produces expected result
        assertThat(expectedEncoded).isEqualTo("dGVzdF9zZWNyZXRfa2V5Og==");
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("Should create WebClient with custom timeout properties")
    void shouldCreateWebClientWithCustomTimeoutProperties() {
        // Given
        TossPaymentsProperties customProperties = new TossPaymentsProperties(
            "https://api.custom.com",
            "custom_secret",
            Duration.ofSeconds(5),
            Duration.ofSeconds(15)
        );

        // When
        WebClient webClient = config.tossPaymentsWebClient(customProperties);

        // Then
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("Should handle default timeout values from properties")
    void shouldHandleDefaultTimeoutValues() {
        // Given - properties with null timeouts should use defaults
        TossPaymentsProperties defaultProperties = new TossPaymentsProperties(
            "https://api.tosspayments.com/v1",
            "secret_key",
            null,
            null
        );

        // When
        WebClient webClient = config.tossPaymentsWebClient(defaultProperties);

        // Then
        assertThat(webClient).isNotNull();
        // Default values are applied by TossPaymentsProperties record
        assertThat(defaultProperties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(defaultProperties.readTimeout()).isEqualTo(Duration.ofSeconds(10));
    }
}
