package com.fluxpay.engine.domain.model.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderId")
class OrderIdTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should generate unique OrderId using generate factory method")
        void shouldGenerateUniqueOrderId() {
            OrderId id1 = OrderId.generate();
            OrderId id2 = OrderId.generate();

            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotNull();
            assertThat(id2.value()).isNotNull();
        }

        @Test
        @DisplayName("should create OrderId from UUID")
        void shouldCreateOrderIdFromUuid() {
            UUID uuid = UUID.randomUUID();

            OrderId orderId = OrderId.of(uuid);

            assertThat(orderId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("should create OrderId from string")
        void shouldCreateOrderIdFromString() {
            UUID uuid = UUID.randomUUID();
            String uuidString = uuid.toString();

            OrderId orderId = OrderId.of(uuidString);

            assertThat(orderId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("should throw exception when UUID is null")
        void shouldThrowExceptionWhenUuidIsNull() {
            assertThatThrownBy(() -> OrderId.of((UUID) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
        }

        @Test
        @DisplayName("should throw exception when string is null")
        void shouldThrowExceptionWhenStringIsNull() {
            assertThatThrownBy(() -> OrderId.of((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
        }

        @Test
        @DisplayName("should throw exception when string is invalid UUID format")
        void shouldThrowExceptionWhenStringIsInvalidUuidFormat() {
            assertThatThrownBy(() -> OrderId.of("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("should be equal when UUID values are same")
        void shouldBeEqualWhenUuidValuesAreSame() {
            UUID uuid = UUID.randomUUID();
            OrderId id1 = OrderId.of(uuid);
            OrderId id2 = OrderId.of(uuid);

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when UUID values differ")
        void shouldNotBeEqualWhenUuidValuesDiffer() {
            OrderId id1 = OrderId.generate();
            OrderId id2 = OrderId.generate();

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
            OrderId orderId = OrderId.of(uuid);

            assertThat(orderId.toString()).contains(uuid.toString());
        }
    }
}
