package com.fluxpay.engine.domain.model.order;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OrderLineItem value object.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("OrderLineItem")
class OrderLineItemTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create OrderLineItem with valid parameters")
        void shouldCreateOrderLineItemWithValidParameters() {
            // Given
            String productId = "PROD-001";
            String productName = "Test Product";
            int quantity = 2;
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When
            OrderLineItem lineItem = OrderLineItem.create(productId, productName, quantity, unitPrice);

            // Then
            assertThat(lineItem).isNotNull();
            assertThat(lineItem.getId()).isNotNull();
            assertThat(lineItem.getProductId()).isEqualTo(productId);
            assertThat(lineItem.getProductName()).isEqualTo(productName);
            assertThat(lineItem.getQuantity()).isEqualTo(quantity);
            assertThat(lineItem.getUnitPrice()).isEqualTo(unitPrice);
        }

        @Test
        @DisplayName("should automatically calculate totalPrice as unitPrice * quantity")
        void shouldAutomaticallyCalculateTotalPrice() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
            int quantity = 3;

            // When
            OrderLineItem lineItem = OrderLineItem.create("PROD-001", "Test Product", quantity, unitPrice);

            // Then
            Money expectedTotal = Money.of(BigDecimal.valueOf(30000), Currency.KRW);
            assertThat(lineItem.getTotalPrice()).isEqualTo(expectedTotal);
        }

        @Test
        @DisplayName("should calculate totalPrice correctly with quantity of 1")
        void shouldCalculateTotalPriceWithQuantityOne() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(15000), Currency.KRW);
            int quantity = 1;

            // When
            OrderLineItem lineItem = OrderLineItem.create("PROD-002", "Single Item", quantity, unitPrice);

            // Then
            assertThat(lineItem.getTotalPrice()).isEqualTo(unitPrice);
        }

        @Test
        @DisplayName("should restore OrderLineItem from database with all fields")
        void shouldRestoreOrderLineItemFromDatabase() {
            // Given
            UUID id = UUID.randomUUID();
            String productId = "PROD-003";
            String productName = "Restored Product";
            int quantity = 5;
            Money unitPrice = Money.of(BigDecimal.valueOf(5000), Currency.USD);
            Money totalPrice = Money.of(BigDecimal.valueOf(25000), Currency.USD);

            // When
            OrderLineItem lineItem = OrderLineItem.restore(id, productId, productName, quantity, unitPrice, totalPrice);

            // Then
            assertThat(lineItem.getId()).isEqualTo(id);
            assertThat(lineItem.getProductId()).isEqualTo(productId);
            assertThat(lineItem.getProductName()).isEqualTo(productName);
            assertThat(lineItem.getQuantity()).isEqualTo(quantity);
            assertThat(lineItem.getUnitPrice()).isEqualTo(unitPrice);
            assertThat(lineItem.getTotalPrice()).isEqualTo(totalPrice);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw exception when productId is null")
        void shouldThrowExceptionWhenProductIdIsNull() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When & Then
            assertThatThrownBy(() -> OrderLineItem.create(null, "Product", 1, unitPrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product ID");
        }

        @Test
        @DisplayName("should throw exception when productId is blank")
        void shouldThrowExceptionWhenProductIdIsBlank() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When & Then
            assertThatThrownBy(() -> OrderLineItem.create("   ", "Product", 1, unitPrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product ID");
        }

        @Test
        @DisplayName("should throw exception when productName is null")
        void shouldThrowExceptionWhenProductNameIsNull() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When & Then
            assertThatThrownBy(() -> OrderLineItem.create("PROD-001", null, 1, unitPrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name");
        }

        @Test
        @DisplayName("should throw exception when productName is blank")
        void shouldThrowExceptionWhenProductNameIsBlank() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When & Then
            assertThatThrownBy(() -> OrderLineItem.create("PROD-001", "", 1, unitPrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name");
        }

        @Test
        @DisplayName("should throw exception when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When & Then
            assertThatThrownBy(() -> OrderLineItem.create("PROD-001", "Product", 0, unitPrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When & Then
            assertThatThrownBy(() -> OrderLineItem.create("PROD-001", "Product", -1, unitPrice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("should throw exception when unitPrice is null")
        void shouldThrowExceptionWhenUnitPriceIsNull() {
            // When & Then
            assertThatThrownBy(() -> OrderLineItem.create("PROD-001", "Product", 1, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Unit price");
        }
    }

    @Nested
    @DisplayName("withQuantity")
    class WithQuantity {

        @Test
        @DisplayName("should create new OrderLineItem with updated quantity")
        void shouldCreateNewOrderLineItemWithUpdatedQuantity() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
            OrderLineItem original = OrderLineItem.create("PROD-001", "Product", 2, unitPrice);

            // When
            OrderLineItem updated = original.withQuantity(5);

            // Then
            assertThat(updated).isNotSameAs(original);
            assertThat(updated.getQuantity()).isEqualTo(5);
            assertThat(updated.getTotalPrice()).isEqualTo(Money.of(BigDecimal.valueOf(50000), Currency.KRW));
        }

        @Test
        @DisplayName("should preserve id when creating new instance with updated quantity")
        void shouldPreserveIdWhenCreatingNewInstanceWithUpdatedQuantity() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
            OrderLineItem original = OrderLineItem.create("PROD-001", "Product", 2, unitPrice);

            // When
            OrderLineItem updated = original.withQuantity(3);

            // Then
            assertThat(updated.getId()).isEqualTo(original.getId());
            assertThat(updated.getProductId()).isEqualTo(original.getProductId());
            assertThat(updated.getProductName()).isEqualTo(original.getProductName());
            assertThat(updated.getUnitPrice()).isEqualTo(original.getUnitPrice());
        }

        @Test
        @DisplayName("should throw exception when new quantity is zero")
        void shouldThrowExceptionWhenNewQuantityIsZero() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
            OrderLineItem lineItem = OrderLineItem.create("PROD-001", "Product", 2, unitPrice);

            // When & Then
            assertThatThrownBy(() -> lineItem.withQuantity(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("should throw exception when new quantity is negative")
        void shouldThrowExceptionWhenNewQuantityIsNegative() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
            OrderLineItem lineItem = OrderLineItem.create("PROD-001", "Product", 2, unitPrice);

            // When & Then
            assertThatThrownBy(() -> lineItem.withQuantity(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity");
        }

        @Test
        @DisplayName("should recalculate totalPrice correctly")
        void shouldRecalculateTotalPriceCorrectly() {
            // Given
            Money unitPrice = Money.of(new BigDecimal("99.99"), Currency.USD);
            OrderLineItem original = OrderLineItem.create("PROD-001", "Product", 1, unitPrice);

            // When
            OrderLineItem updated = original.withQuantity(3);

            // Then - 99.99 * 3 = 299.97
            assertThat(updated.getTotalPrice()).isEqualTo(Money.of(new BigDecimal("299.97"), Currency.USD));
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("two OrderLineItems with same id should be equal")
        void twoOrderLineItemsWithSameIdShouldBeEqual() {
            // Given
            UUID id = UUID.randomUUID();
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
            Money totalPrice = Money.of(BigDecimal.valueOf(20000), Currency.KRW);

            // When
            OrderLineItem item1 = OrderLineItem.restore(id, "PROD-001", "Product 1", 2, unitPrice, totalPrice);
            OrderLineItem item2 = OrderLineItem.restore(id, "PROD-001", "Product 1", 2, unitPrice, totalPrice);

            // Then
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }

        @Test
        @DisplayName("two OrderLineItems with different ids should not be equal")
        void twoOrderLineItemsWithDifferentIdsShouldNotBeEqual() {
            // Given
            Money unitPrice = Money.of(BigDecimal.valueOf(10000), Currency.KRW);

            // When
            OrderLineItem item1 = OrderLineItem.create("PROD-001", "Product", 2, unitPrice);
            OrderLineItem item2 = OrderLineItem.create("PROD-001", "Product", 2, unitPrice);

            // Then (different UUIDs generated)
            assertThat(item1).isNotEqualTo(item2);
        }
    }
}
