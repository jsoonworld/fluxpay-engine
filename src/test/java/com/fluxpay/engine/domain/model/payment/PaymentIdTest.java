package com.fluxpay.engine.domain.model.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentId")
class PaymentIdTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should generate unique PaymentId using generate factory method")
        void shouldGenerateUniquePaymentId() {
            PaymentId id1 = PaymentId.generate();
            PaymentId id2 = PaymentId.generate();

            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotNull();
            assertThat(id2.value()).isNotNull();
        }

        @Test
        @DisplayName("should create PaymentId from UUID")
        void shouldCreatePaymentIdFromUuid() {
            UUID uuid = UUID.randomUUID();

            PaymentId paymentId = PaymentId.of(uuid);

            assertThat(paymentId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("should create PaymentId from string")
        void shouldCreatePaymentIdFromString() {
            UUID uuid = UUID.randomUUID();
            String uuidString = uuid.toString();

            PaymentId paymentId = PaymentId.of(uuidString);

            assertThat(paymentId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("should throw exception when UUID is null")
        void shouldThrowExceptionWhenUuidIsNull() {
            assertThatThrownBy(() -> PaymentId.of((UUID) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
        }

        @Test
        @DisplayName("should throw exception when string is null")
        void shouldThrowExceptionWhenStringIsNull() {
            assertThatThrownBy(() -> PaymentId.of((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
        }

        @Test
        @DisplayName("should throw exception when string is invalid UUID format")
        void shouldThrowExceptionWhenStringIsInvalidUuidFormat() {
            assertThatThrownBy(() -> PaymentId.of("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID format");
        }

        @Test
        @DisplayName("should throw exception when constructor receives null UUID")
        void shouldThrowExceptionWhenConstructorReceivesNullUuid() {
            assertThatThrownBy(() -> new PaymentId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("should be equal when UUID values are same")
        void shouldBeEqualWhenUuidValuesAreSame() {
            UUID uuid = UUID.randomUUID();
            PaymentId id1 = PaymentId.of(uuid);
            PaymentId id2 = PaymentId.of(uuid);

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when UUID values differ")
        void shouldNotBeEqualWhenUuidValuesDiffer() {
            PaymentId id1 = PaymentId.generate();
            PaymentId id2 = PaymentId.generate();

            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("should return UUID string representation")
        void shouldReturnUuidStringRepresentation() {
            UUID uuid = UUID.randomUUID();
            PaymentId paymentId = PaymentId.of(uuid);

            String result = paymentId.toString();

            assertThat(result).isEqualTo(uuid.toString());
        }
    }
}
