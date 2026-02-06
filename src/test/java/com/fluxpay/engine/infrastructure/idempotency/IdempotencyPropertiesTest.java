package com.fluxpay.engine.infrastructure.idempotency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IdempotencyProperties.
 */
class IdempotencyPropertiesTest {

    @Nested
    @DisplayName("Default values tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should use default TTL of 24 hours when ttl is null")
        void shouldUseDefaultTtlWhenNull() {
            // Given & When
            IdempotencyProperties properties = new IdempotencyProperties(true, null, null);

            // Then
            assertThat(properties.ttl()).isEqualTo(Duration.ofHours(24));
        }

        @Test
        @DisplayName("Should use default Redis config when redis is null")
        void shouldUseDefaultRedisConfigWhenNull() {
            // Given & When
            IdempotencyProperties properties = new IdempotencyProperties(true, Duration.ofHours(1), null);

            // Then
            assertThat(properties.redis()).isNotNull();
            assertThat(properties.redis().keyPrefix()).isEqualTo("idempotency");
            assertThat(properties.redis().timeout()).isEqualTo(Duration.ofSeconds(3));
        }

        @Test
        @DisplayName("Should use provided values when specified")
        void shouldUseProvidedValues() {
            // Given
            Duration customTtl = Duration.ofHours(48);
            IdempotencyProperties.Redis customRedis = new IdempotencyProperties.Redis(
                "custom-prefix",
                Duration.ofSeconds(5)
            );

            // When
            IdempotencyProperties properties = new IdempotencyProperties(false, customTtl, customRedis);

            // Then
            assertThat(properties.enabled()).isFalse();
            assertThat(properties.ttl()).isEqualTo(customTtl);
            assertThat(properties.redis().keyPrefix()).isEqualTo("custom-prefix");
            assertThat(properties.redis().timeout()).isEqualTo(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Should create default configuration via factory method")
        void shouldCreateDefaultConfiguration() {
            // Given & When
            IdempotencyProperties properties = IdempotencyProperties.defaults();

            // Then
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.ttl()).isEqualTo(Duration.ofHours(24));
            assertThat(properties.redis().keyPrefix()).isEqualTo("idempotency");
            assertThat(properties.redis().timeout()).isEqualTo(Duration.ofSeconds(3));
        }
    }

    @Nested
    @DisplayName("Redis config default values tests")
    class RedisConfigDefaultValuesTests {

        @Test
        @DisplayName("Should use default keyPrefix when null or blank")
        void shouldUseDefaultKeyPrefixWhenNull() {
            // Given & When
            IdempotencyProperties.Redis redis1 = new IdempotencyProperties.Redis(null, Duration.ofSeconds(5));
            IdempotencyProperties.Redis redis2 = new IdempotencyProperties.Redis("", Duration.ofSeconds(5));
            IdempotencyProperties.Redis redis3 = new IdempotencyProperties.Redis("   ", Duration.ofSeconds(5));

            // Then
            assertThat(redis1.keyPrefix()).isEqualTo("idempotency");
            assertThat(redis2.keyPrefix()).isEqualTo("idempotency");
            assertThat(redis3.keyPrefix()).isEqualTo("idempotency");
        }

        @Test
        @DisplayName("Should use default timeout when null")
        void shouldUseDefaultTimeoutWhenNull() {
            // Given & When
            IdempotencyProperties.Redis redis = new IdempotencyProperties.Redis("prefix", null);

            // Then
            assertThat(redis.timeout()).isEqualTo(Duration.ofSeconds(3));
        }

        @Test
        @DisplayName("Should use provided values when specified")
        void shouldUseProvidedRedisValues() {
            // Given & When
            IdempotencyProperties.Redis redis = new IdempotencyProperties.Redis("my-prefix", Duration.ofSeconds(10));

            // Then
            assertThat(redis.keyPrefix()).isEqualTo("my-prefix");
            assertThat(redis.timeout()).isEqualTo(Duration.ofSeconds(10));
        }
    }
}
