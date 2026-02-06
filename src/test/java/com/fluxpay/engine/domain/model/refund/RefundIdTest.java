package com.fluxpay.engine.domain.model.refund;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RefundId value object.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("RefundId")
class RefundIdTest {

    @Nested
    @DisplayName("Generation")
    class Generation {

        @Test
        @DisplayName("should generate unique RefundId with ref_ prefix")
        void shouldGenerateUniqueRefundIdWithPrefix() {
            RefundId refundId = RefundId.generate();

            assertThat(refundId).isNotNull();
            assertThat(refundId.value()).startsWith("ref_");
        }

        @Test
        @DisplayName("should generate different IDs on each call")
        void shouldGenerateDifferentIds() {
            RefundId id1 = RefundId.generate();
            RefundId id2 = RefundId.generate();

            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }
    }

    @Nested
    @DisplayName("Creation from value")
    class CreationFromValue {

        @Test
        @DisplayName("should create RefundId from valid value")
        void shouldCreateRefundIdFromValidValue() {
            String value = "ref_abc123";
            RefundId refundId = RefundId.of(value);

            assertThat(refundId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("should throw exception for null value")
        void shouldThrowExceptionForNullValue() {
            assertThatThrownBy(() -> RefundId.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Refund ID");
        }

        @Test
        @DisplayName("should throw exception for blank value")
        void shouldThrowExceptionForBlankValue() {
            assertThatThrownBy(() -> RefundId.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should throw exception for empty value")
        void shouldThrowExceptionForEmptyValue() {
            assertThatThrownBy(() -> RefundId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("two RefundIds with same value should be equal")
        void twoRefundIdsWithSameValueShouldBeEqual() {
            RefundId id1 = RefundId.of("ref_test123");
            RefundId id2 = RefundId.of("ref_test123");

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("two RefundIds with different values should not be equal")
        void twoRefundIdsWithDifferentValuesShouldNotBeEqual() {
            RefundId id1 = RefundId.of("ref_test123");
            RefundId id2 = RefundId.of("ref_test456");

            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringMethod {

        @Test
        @DisplayName("toString should return the value")
        void toStringShouldReturnTheValue() {
            RefundId refundId = RefundId.of("ref_test123");

            assertThat(refundId.toString()).contains("ref_test123");
        }
    }
}
