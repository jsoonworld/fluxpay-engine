package com.fluxpay.engine.domain.model.webhook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WebhookId value object.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("WebhookId")
class WebhookIdTest {

    @Nested
    @DisplayName("Generation")
    class Generation {

        @Test
        @DisplayName("should generate unique WebhookId with whk_ prefix")
        void shouldGenerateUniqueWebhookIdWithPrefix() {
            WebhookId webhookId = WebhookId.generate();

            assertThat(webhookId).isNotNull();
            assertThat(webhookId.value()).startsWith("whk_");
        }

        @Test
        @DisplayName("should generate different IDs on each call")
        void shouldGenerateDifferentIds() {
            WebhookId id1 = WebhookId.generate();
            WebhookId id2 = WebhookId.generate();

            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }
    }

    @Nested
    @DisplayName("Creation from value")
    class CreationFromValue {

        @Test
        @DisplayName("should create WebhookId from valid value")
        void shouldCreateWebhookIdFromValidValue() {
            String value = "whk_abc123";
            WebhookId webhookId = WebhookId.of(value);

            assertThat(webhookId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("should throw exception for null value")
        void shouldThrowExceptionForNullValue() {
            assertThatThrownBy(() -> WebhookId.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Webhook ID");
        }

        @Test
        @DisplayName("should throw exception for blank value")
        void shouldThrowExceptionForBlankValue() {
            assertThatThrownBy(() -> WebhookId.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should throw exception for empty value")
        void shouldThrowExceptionForEmptyValue() {
            assertThatThrownBy(() -> WebhookId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("two WebhookIds with same value should be equal")
        void twoWebhookIdsWithSameValueShouldBeEqual() {
            WebhookId id1 = WebhookId.of("whk_test123");
            WebhookId id2 = WebhookId.of("whk_test123");

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("two WebhookIds with different values should not be equal")
        void twoWebhookIdsWithDifferentValuesShouldNotBeEqual() {
            WebhookId id1 = WebhookId.of("whk_test123");
            WebhookId id2 = WebhookId.of("whk_test456");

            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringMethod {

        @Test
        @DisplayName("toString should return the value")
        void toStringShouldReturnTheValue() {
            WebhookId webhookId = WebhookId.of("whk_test123");

            assertThat(webhookId.toString()).contains("whk_test123");
        }
    }
}
