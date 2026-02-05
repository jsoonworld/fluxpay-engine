package com.fluxpay.engine.infrastructure.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenantPropertiesTest {

    @Nested
    @DisplayName("TenantProperties")
    class TenantPropertiesTests {

        @Test
        @DisplayName("Should load default config")
        void shouldLoadDefaultConfig() {
            // Given
            TenantProperties.TenantConfig defaultConfig = new TenantProperties.TenantConfig(
                100,
                false,
                false,
                ""
            );
            Map<String, TenantProperties.TenantConfig> configs = new HashMap<>();
            configs.put("default", defaultConfig);

            // When
            TenantProperties properties = new TenantProperties(configs);

            // Then
            TenantProperties.TenantConfig result = properties.getConfig("default");
            assertThat(result).isNotNull();
            assertThat(result.rateLimit()).isEqualTo(100);
            assertThat(result.creditEnabled()).isFalse();
            assertThat(result.subscriptionEnabled()).isFalse();
            assertThat(result.webhookUrl()).isEmpty();
        }

        @Test
        @DisplayName("Should load tenant specific config")
        void shouldLoadTenantSpecificConfig() {
            // Given
            TenantProperties.TenantConfig defaultConfig = new TenantProperties.TenantConfig(
                100,
                false,
                false,
                ""
            );
            TenantProperties.TenantConfig serviceAConfig = new TenantProperties.TenantConfig(
                1000,
                true,
                false,
                "https://service-a.com/webhooks"
            );
            Map<String, TenantProperties.TenantConfig> configs = new HashMap<>();
            configs.put("default", defaultConfig);
            configs.put("service-a", serviceAConfig);

            // When
            TenantProperties properties = new TenantProperties(configs);

            // Then
            TenantProperties.TenantConfig result = properties.getConfig("service-a");
            assertThat(result).isNotNull();
            assertThat(result.rateLimit()).isEqualTo(1000);
            assertThat(result.creditEnabled()).isTrue();
            assertThat(result.subscriptionEnabled()).isFalse();
            assertThat(result.webhookUrl()).isEqualTo("https://service-a.com/webhooks");
        }

        @Test
        @DisplayName("Should fallback to default for unknown tenant")
        void shouldFallbackToDefaultForUnknownTenant() {
            // Given
            TenantProperties.TenantConfig defaultConfig = new TenantProperties.TenantConfig(
                100,
                false,
                false,
                ""
            );
            Map<String, TenantProperties.TenantConfig> configs = new HashMap<>();
            configs.put("default", defaultConfig);

            // When
            TenantProperties properties = new TenantProperties(configs);

            // Then
            TenantProperties.TenantConfig result = properties.getConfig("unknown-tenant");
            assertThat(result).isNotNull();
            assertThat(result.rateLimit()).isEqualTo(100);
            assertThat(result.creditEnabled()).isFalse();
            assertThat(result.subscriptionEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should initialize empty configs map when null provided")
        void shouldInitializeEmptyConfigsWhenNullProvided() {
            // When
            TenantProperties properties = new TenantProperties(null);

            // Then
            assertThat(properties.configs()).isNotNull();
            assertThat(properties.configs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("TenantConfig")
    class TenantConfigTests {

        @Test
        @DisplayName("Should use default rate limit when non-positive value provided")
        void shouldUseDefaultRateLimitWhenNonPositiveProvided() {
            // When
            TenantProperties.TenantConfig config = new TenantProperties.TenantConfig(
                0,
                true,
                true,
                "https://example.com"
            );

            // Then
            assertThat(config.rateLimit()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should use default rate limit when negative value provided")
        void shouldUseDefaultRateLimitWhenNegativeProvided() {
            // When
            TenantProperties.TenantConfig config = new TenantProperties.TenantConfig(
                -50,
                true,
                true,
                "https://example.com"
            );

            // Then
            assertThat(config.rateLimit()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should preserve valid rate limit")
        void shouldPreserveValidRateLimit() {
            // When
            TenantProperties.TenantConfig config = new TenantProperties.TenantConfig(
                500,
                true,
                false,
                "https://example.com"
            );

            // Then
            assertThat(config.rateLimit()).isEqualTo(500);
            assertThat(config.creditEnabled()).isTrue();
            assertThat(config.subscriptionEnabled()).isFalse();
            assertThat(config.webhookUrl()).isEqualTo("https://example.com");
        }
    }
}
