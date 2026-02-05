package com.fluxpay.engine.domain.model.order;

import com.fluxpay.engine.domain.exception.InvalidOrderStateException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Order aggregate root.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("Order")
class OrderTest {

    private OrderLineItem lineItem1;
    private OrderLineItem lineItem2;

    @BeforeEach
    void setUp() {
        lineItem1 = OrderLineItem.create(
                "PROD-001", "Test Product 1", 2,
                Money.of(BigDecimal.valueOf(10000), Currency.KRW)
        );
        lineItem2 = OrderLineItem.create(
                "PROD-002", "Test Product 2", 1,
                Money.of(BigDecimal.valueOf(5000), Currency.KRW)
        );
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create Order with PENDING status")
        void shouldCreateOrderWithPendingStatus() {
            // When
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.isPending()).isTrue();
        }

        @Test
        @DisplayName("should generate OrderId on creation")
        void shouldGenerateOrderIdOnCreation() {
            // When
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // Then
            assertThat(order.getId()).isNotNull();
            assertThat(order.getId().value()).isNotNull();
        }

        @Test
        @DisplayName("should set userId correctly")
        void shouldSetUserIdCorrectly() {
            // Given
            String userId = "user-123";

            // When
            Order order = Order.create(userId, List.of(lineItem1), Currency.KRW, null);

            // Then
            assertThat(order.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("should automatically calculate totalAmount from line items")
        void shouldAutomaticallyCalculateTotalAmount() {
            // Given - lineItem1: 10000 * 2 = 20000, lineItem2: 5000 * 1 = 5000
            // Total: 25000

            // When
            Order order = Order.create("user-123", List.of(lineItem1, lineItem2), Currency.KRW, null);

            // Then
            assertThat(order.getTotalAmount()).isEqualTo(Money.of(BigDecimal.valueOf(25000), Currency.KRW));
        }

        @Test
        @DisplayName("should set createdAt and updatedAt on creation")
        void shouldSetTimestampsOnCreation() {
            // Given
            Instant before = Instant.now();

            // When
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // Then
            Instant after = Instant.now();
            assertThat(order.getCreatedAt()).isBetween(before, after);
            assertThat(order.getUpdatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should store metadata")
        void shouldStoreMetadata() {
            // Given
            Map<String, Object> metadata = Map.of("channel", "WEB", "referenceId", "REF-123");

            // When
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, metadata);

            // Then
            assertThat(order.getMetadata()).containsEntry("channel", "WEB");
            assertThat(order.getMetadata()).containsEntry("referenceId", "REF-123");
        }

        @Test
        @DisplayName("should return unmodifiable line items list")
        void shouldReturnUnmodifiableLineItems() {
            // When
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // Then
            assertThatThrownBy(() -> order.getLineItems().add(lineItem2))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return unmodifiable metadata map")
        void shouldReturnUnmodifiableMetadata() {
            // Given
            Map<String, Object> metadata = Map.of("key", "value");

            // When
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, metadata);

            // Then
            assertThatThrownBy(() -> order.getMetadata().put("newKey", "newValue"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> Order.create(null, List.of(lineItem1), Currency.KRW, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("User ID");
        }

        @Test
        @DisplayName("should throw exception when userId is blank")
        void shouldThrowExceptionWhenUserIdIsBlank() {
            // When & Then
            assertThatThrownBy(() -> Order.create("   ", List.of(lineItem1), Currency.KRW, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User ID");
        }

        @Test
        @DisplayName("should throw exception when userId is empty")
        void shouldThrowExceptionWhenUserIdIsEmpty() {
            // When & Then
            assertThatThrownBy(() -> Order.create("", List.of(lineItem1), Currency.KRW, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User ID");
        }

        @Test
        @DisplayName("should throw exception when lineItems is null")
        void shouldThrowExceptionWhenLineItemsIsNull() {
            // When & Then
            assertThatThrownBy(() -> Order.create("user-123", null, Currency.KRW, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("line item");
        }

        @Test
        @DisplayName("should throw exception when lineItems is empty")
        void shouldThrowExceptionWhenLineItemsIsEmpty() {
            // When & Then
            assertThatThrownBy(() -> Order.create("user-123", List.of(), Currency.KRW, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("line item");
        }

        @Test
        @DisplayName("should throw exception when currency is null")
        void shouldThrowExceptionWhenCurrencyIsNull() {
            // When & Then
            assertThatThrownBy(() -> Order.create("user-123", List.of(lineItem1), null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Currency");
        }
    }

    @Nested
    @DisplayName("State Transitions - Valid")
    class ValidStateTransitions {

        @Test
        @DisplayName("should transition from PENDING to PAID")
        void shouldTransitionFromPendingToPaid() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // When
            order.markAsPaid();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.isPaid()).isTrue();
            assertThat(order.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition from PENDING to CANCELLED")
        void shouldTransitionFromPendingToCancelled() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // When
            order.cancel();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("should transition from PENDING to FAILED")
        void shouldTransitionFromPendingToFailed() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // When
            order.fail();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(order.isFailed()).isTrue();
        }

        @Test
        @DisplayName("should transition from PAID to COMPLETED")
        void shouldTransitionFromPaidToCompleted() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            order.markAsPaid();

            // When
            order.complete();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.isCompleted()).isTrue();
            assertThat(order.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition from PAID to CANCELLED")
        void shouldTransitionFromPaidToCancelled() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            order.markAsPaid();

            // When
            order.cancel();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("should transition from PAID to FAILED")
        void shouldTransitionFromPaidToFailed() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            order.markAsPaid();

            // When
            order.fail();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(order.isFailed()).isTrue();
        }

        @Test
        @DisplayName("should update updatedAt on state transition")
        void shouldUpdateTimestampOnStateTransition() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            Instant originalUpdatedAt = order.getUpdatedAt();

            // Small delay to ensure timestamp difference
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            // When
            order.markAsPaid();

            // Then
            assertThat(order.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("State Transitions - Invalid")
    class InvalidStateTransitions {

        @Test
        @DisplayName("should throw exception when transitioning from PENDING to COMPLETED")
        void shouldThrowExceptionWhenTransitioningFromPendingToCompleted() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // When & Then
            assertThatThrownBy(() -> order.complete())
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("should throw exception when transitioning from COMPLETED to any state")
        void shouldThrowExceptionWhenTransitioningFromCompletedToAnyState() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            order.markAsPaid();
            order.complete();

            // When & Then
            assertThatThrownBy(() -> order.cancel())
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("COMPLETED")
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("should throw exception when transitioning from CANCELLED to any state")
        void shouldThrowExceptionWhenTransitioningFromCancelledToAnyState() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            order.cancel();

            // When & Then
            assertThatThrownBy(() -> order.markAsPaid())
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("CANCELLED")
                    .hasMessageContaining("PAID");
        }

        @Test
        @DisplayName("should throw exception when transitioning from FAILED to any state")
        void shouldThrowExceptionWhenTransitioningFromFailedToAnyState() {
            // Given
            Order order = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            order.fail();

            // When & Then
            assertThatThrownBy(() -> order.complete())
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("FAILED")
                    .hasMessageContaining("COMPLETED");
        }
    }

    @Nested
    @DisplayName("Restore from Database")
    class RestoreFromDatabase {

        @Test
        @DisplayName("should restore Order with all fields")
        void shouldRestoreOrderWithAllFields() {
            // Given
            UUID orderId = UUID.randomUUID();
            UUID lineItemId = UUID.randomUUID();
            Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");
            Instant updatedAt = Instant.parse("2024-01-01T11:00:00Z");
            Instant paidAt = Instant.parse("2024-01-01T10:30:00Z");
            Instant completedAt = Instant.parse("2024-01-01T10:45:00Z");

            OrderLineItem restoredLineItem = OrderLineItem.restore(
                    lineItemId, "PROD-001", "Restored Product", 2,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    Money.of(BigDecimal.valueOf(20000), Currency.KRW)
            );

            // When
            Order order = Order.restore(
                    OrderId.of(orderId),
                    "user-123",
                    List.of(restoredLineItem),
                    Currency.KRW,
                    Map.of("key", "value"),
                    OrderStatus.COMPLETED,
                    Money.of(BigDecimal.valueOf(20000), Currency.KRW),
                    createdAt,
                    updatedAt,
                    paidAt,
                    completedAt
            );

            // Then
            assertThat(order.getId().value()).isEqualTo(orderId);
            assertThat(order.getUserId()).isEqualTo("user-123");
            assertThat(order.getLineItems()).hasSize(1);
            assertThat(order.getCurrency()).isEqualTo(Currency.KRW);
            assertThat(order.getMetadata()).containsEntry("key", "value");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.getTotalAmount()).isEqualTo(Money.of(BigDecimal.valueOf(20000), Currency.KRW));
            assertThat(order.getCreatedAt()).isEqualTo(createdAt);
            assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(order.getPaidAt()).isEqualTo(paidAt);
            assertThat(order.getCompletedAt()).isEqualTo(completedAt);
        }

        @Test
        @DisplayName("should throw exception when PAID status without paidAt timestamp")
        void shouldThrowExceptionWhenPaidStatusWithoutPaidAtTimestamp() {
            // Given
            UUID orderId = UUID.randomUUID();
            UUID lineItemId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderLineItem lineItem = OrderLineItem.restore(
                    lineItemId, "PROD-001", "Product", 1,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );

            // When & Then - PAID status without paidAt should fail
            assertThatThrownBy(() -> Order.restore(
                    OrderId.of(orderId),
                    "user-123",
                    List.of(lineItem),
                    Currency.KRW,
                    null,
                    OrderStatus.PAID,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    now,
                    now,
                    null,  // paidAt is null but status is PAID
                    null
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("paidAt");
        }

        @Test
        @DisplayName("should throw exception when COMPLETED status without paidAt timestamp")
        void shouldThrowExceptionWhenCompletedStatusWithoutPaidAtTimestamp() {
            // Given
            UUID orderId = UUID.randomUUID();
            UUID lineItemId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderLineItem lineItem = OrderLineItem.restore(
                    lineItemId, "PROD-001", "Product", 1,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );

            // When & Then - COMPLETED status without paidAt should fail
            assertThatThrownBy(() -> Order.restore(
                    OrderId.of(orderId),
                    "user-123",
                    List.of(lineItem),
                    Currency.KRW,
                    null,
                    OrderStatus.COMPLETED,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    now,
                    now,
                    null,  // paidAt is null
                    now    // completedAt is set
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("paidAt");
        }

        @Test
        @DisplayName("should throw exception when COMPLETED status without completedAt timestamp")
        void shouldThrowExceptionWhenCompletedStatusWithoutCompletedAtTimestamp() {
            // Given
            UUID orderId = UUID.randomUUID();
            UUID lineItemId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderLineItem lineItem = OrderLineItem.restore(
                    lineItemId, "PROD-001", "Product", 1,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );

            // When & Then - COMPLETED status without completedAt should fail
            assertThatThrownBy(() -> Order.restore(
                    OrderId.of(orderId),
                    "user-123",
                    List.of(lineItem),
                    Currency.KRW,
                    null,
                    OrderStatus.COMPLETED,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    now,
                    now,
                    now,   // paidAt is set
                    null   // completedAt is null but status is COMPLETED
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("completedAt");
        }

        @Test
        @DisplayName("should throw exception when restoring with blank userId")
        void shouldThrowExceptionWhenRestoringWithBlankUserId() {
            // Given
            UUID orderId = UUID.randomUUID();
            UUID lineItemId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderLineItem lineItem = OrderLineItem.restore(
                    lineItemId, "PROD-001", "Product", 1,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );

            // When & Then
            assertThatThrownBy(() -> Order.restore(
                    OrderId.of(orderId),
                    "   ",  // blank userId
                    List.of(lineItem),
                    Currency.KRW,
                    null,
                    OrderStatus.PENDING,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    now,
                    now,
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User ID");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("two Orders with same id should be equal")
        void twoOrdersWithSameIdShouldBeEqual() {
            // Given
            UUID orderId = UUID.randomUUID();
            UUID lineItemId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderLineItem lineItem = OrderLineItem.restore(
                    lineItemId, "PROD-001", "Product", 1,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );

            // When - Two orders with same ID but different attributes
            Order order1 = Order.restore(
                    OrderId.of(orderId), "user-1", List.of(lineItem),
                    Currency.KRW, null, OrderStatus.PENDING,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                    now, now, null, null
            );
            // Use COMPLETED status with required timestamps (paidAt and completedAt)
            Order order2 = Order.restore(
                    OrderId.of(orderId), "user-2", List.of(lineItem),
                    Currency.USD, null, OrderStatus.COMPLETED,
                    Money.of(BigDecimal.valueOf(5000), Currency.USD),
                    now, now, now, now  // paidAt and completedAt are now set
            );

            // Then - Orders with same ID are equal regardless of other attributes
            assertThat(order1).isEqualTo(order2);
            assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
        }

        @Test
        @DisplayName("two Orders with different ids should not be equal")
        void twoOrdersWithDifferentIdsShouldNotBeEqual() {
            // When
            Order order1 = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);
            Order order2 = Order.create("user-123", List.of(lineItem1), Currency.KRW, null);

            // Then
            assertThat(order1).isNotEqualTo(order2);
        }
    }
}
