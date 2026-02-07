package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderCreatedEvent")
class OrderCreatedEventTest {

    private static final String TEST_EVENT_ID = "evt_order_12345";
    private static final String TEST_ORDER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_USER_ID = "user_12345";
    private static final BigDecimal TEST_TOTAL_AMOUNT = new BigDecimal("50000");
    private static final String TEST_CURRENCY = "KRW";
    private static final Instant TEST_OCCURRED_AT = Instant.parse("2026-02-06T10:00:00Z");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create event with all required fields")
        void shouldCreateEventWithAllRequiredFields() {
            OrderCreatedEvent event = new OrderCreatedEvent(
                TEST_EVENT_ID, TEST_ORDER_ID, TEST_USER_ID,
                TEST_TOTAL_AMOUNT, TEST_CURRENCY, TEST_OCCURRED_AT);

            assertThat(event.eventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(event.orderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(event.userId()).isEqualTo(TEST_USER_ID);
            assertThat(event.totalAmount()).isEqualTo(TEST_TOTAL_AMOUNT);
            assertThat(event.currency()).isEqualTo(TEST_CURRENCY);
            assertThat(event.occurredAt()).isEqualTo(TEST_OCCURRED_AT);
        }

        @Test
        @DisplayName("should throw exception for null eventId")
        void shouldThrowForNullEventId() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                null, TEST_ORDER_ID, TEST_USER_ID,
                TEST_TOTAL_AMOUNT, TEST_CURRENCY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("should throw exception for null orderId")
        void shouldThrowForNullOrderId() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                TEST_EVENT_ID, null, TEST_USER_ID,
                TEST_TOTAL_AMOUNT, TEST_CURRENCY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("orderId");
        }

        @Test
        @DisplayName("should throw exception for null userId")
        void shouldThrowForNullUserId() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                TEST_EVENT_ID, TEST_ORDER_ID, null,
                TEST_TOTAL_AMOUNT, TEST_CURRENCY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("should throw exception for null totalAmount")
        void shouldThrowForNullTotalAmount() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                TEST_EVENT_ID, TEST_ORDER_ID, TEST_USER_ID,
                null, TEST_CURRENCY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("totalAmount");
        }

        @Test
        @DisplayName("should throw exception for null currency")
        void shouldThrowForNullCurrency() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                TEST_EVENT_ID, TEST_ORDER_ID, TEST_USER_ID,
                TEST_TOTAL_AMOUNT, null, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should throw exception for null occurredAt")
        void shouldThrowForNullOccurredAt() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                TEST_EVENT_ID, TEST_ORDER_ID, TEST_USER_ID,
                TEST_TOTAL_AMOUNT, TEST_CURRENCY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("occurredAt");
        }
    }

    @Nested
    @DisplayName("DomainEvent contract")
    class DomainEventContract {

        @Test
        @DisplayName("should return ORDER as aggregate type")
        void shouldReturnOrderAsAggregateType() {
            OrderCreatedEvent event = createEvent();
            assertThat(event.aggregateType()).isEqualTo("ORDER");
        }

        @Test
        @DisplayName("should return orderId as aggregate id")
        void shouldReturnOrderIdAsAggregateId() {
            OrderCreatedEvent event = createEvent();
            assertThat(event.aggregateId()).isEqualTo(TEST_ORDER_ID);
        }

        @Test
        @DisplayName("should return order.created as event type")
        void shouldReturnOrderCreatedAsEventType() {
            OrderCreatedEvent event = createEvent();
            assertThat(event.eventType()).isEqualTo("order.created");
        }
    }

    @Nested
    @DisplayName("Factory method")
    class FactoryMethod {

        @Test
        @DisplayName("should create event from Order domain object")
        void shouldCreateFromOrder() {
            OrderLineItem lineItem = OrderLineItem.create(
                "product_1", "Test Product", 5, Money.krw(10000));
            Order order = Order.create(
                "user_abc", List.of(lineItem), Currency.KRW, Map.of());

            Instant before = Instant.now();
            OrderCreatedEvent event = OrderCreatedEvent.from(order);
            Instant after = Instant.now();

            assertThat(event.eventId()).isNotNull();
            assertThat(event.orderId()).isEqualTo(order.getId().value().toString());
            assertThat(event.userId()).isEqualTo(order.getUserId());
            assertThat(event.totalAmount()).isEqualByComparingTo(order.getTotalAmount().amount());
            assertThat(event.currency()).isEqualTo(order.getCurrency().name());
            assertThat(event.occurredAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should generate unique eventId for each event")
        void shouldGenerateUniqueEventId() {
            OrderLineItem lineItem = OrderLineItem.create(
                "product_1", "Test Product", 1, Money.krw(10000));
            Order order = Order.create(
                "user_abc", List.of(lineItem), Currency.KRW, Map.of());

            OrderCreatedEvent event1 = OrderCreatedEvent.from(order);
            OrderCreatedEvent event2 = OrderCreatedEvent.from(order);
            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }

        @Test
        @DisplayName("should throw exception for null order")
        void shouldThrowForNullOrder() {
            assertThatThrownBy(() -> OrderCreatedEvent.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("order");
        }
    }

    private OrderCreatedEvent createEvent() {
        return new OrderCreatedEvent(
            TEST_EVENT_ID, TEST_ORDER_ID, TEST_USER_ID,
            TEST_TOTAL_AMOUNT, TEST_CURRENCY, TEST_OCCURRED_AT);
    }
}
