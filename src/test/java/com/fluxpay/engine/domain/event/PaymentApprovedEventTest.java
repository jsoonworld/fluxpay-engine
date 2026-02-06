package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentMethodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentApprovedEvent")
class PaymentApprovedEventTest {

    private static final String TEST_EVENT_ID = "evt_pay_12345";
    private static final String TEST_PAYMENT_ID = "pay_12345";
    private static final String TEST_ORDER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("10000");
    private static final String TEST_CURRENCY = "KRW";
    private static final String TEST_PG_PAYMENT_KEY = "pg_key_abc123";
    private static final Instant TEST_OCCURRED_AT = Instant.parse("2026-02-06T10:00:00Z");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create event with all required fields")
        void shouldCreateEventWithAllRequiredFields() {
            PaymentApprovedEvent event = new PaymentApprovedEvent(
                TEST_EVENT_ID, TEST_PAYMENT_ID, TEST_ORDER_ID,
                TEST_AMOUNT, TEST_CURRENCY, TEST_PG_PAYMENT_KEY, TEST_OCCURRED_AT);

            assertThat(event.eventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(event.paymentId()).isEqualTo(TEST_PAYMENT_ID);
            assertThat(event.orderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(event.amount()).isEqualTo(TEST_AMOUNT);
            assertThat(event.currency()).isEqualTo(TEST_CURRENCY);
            assertThat(event.pgPaymentKey()).isEqualTo(TEST_PG_PAYMENT_KEY);
            assertThat(event.occurredAt()).isEqualTo(TEST_OCCURRED_AT);
        }

        @Test
        @DisplayName("should throw exception for null eventId")
        void shouldThrowForNullEventId() {
            assertThatThrownBy(() -> new PaymentApprovedEvent(
                null, TEST_PAYMENT_ID, TEST_ORDER_ID,
                TEST_AMOUNT, TEST_CURRENCY, TEST_PG_PAYMENT_KEY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("should throw exception for null paymentId")
        void shouldThrowForNullPaymentId() {
            assertThatThrownBy(() -> new PaymentApprovedEvent(
                TEST_EVENT_ID, null, TEST_ORDER_ID,
                TEST_AMOUNT, TEST_CURRENCY, TEST_PG_PAYMENT_KEY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("paymentId");
        }

        @Test
        @DisplayName("should throw exception for null orderId")
        void shouldThrowForNullOrderId() {
            assertThatThrownBy(() -> new PaymentApprovedEvent(
                TEST_EVENT_ID, TEST_PAYMENT_ID, null,
                TEST_AMOUNT, TEST_CURRENCY, TEST_PG_PAYMENT_KEY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("orderId");
        }

        @Test
        @DisplayName("should throw exception for null amount")
        void shouldThrowForNullAmount() {
            assertThatThrownBy(() -> new PaymentApprovedEvent(
                TEST_EVENT_ID, TEST_PAYMENT_ID, TEST_ORDER_ID,
                null, TEST_CURRENCY, TEST_PG_PAYMENT_KEY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception for null currency")
        void shouldThrowForNullCurrency() {
            assertThatThrownBy(() -> new PaymentApprovedEvent(
                TEST_EVENT_ID, TEST_PAYMENT_ID, TEST_ORDER_ID,
                TEST_AMOUNT, null, TEST_PG_PAYMENT_KEY, TEST_OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should throw exception for null occurredAt")
        void shouldThrowForNullOccurredAt() {
            assertThatThrownBy(() -> new PaymentApprovedEvent(
                TEST_EVENT_ID, TEST_PAYMENT_ID, TEST_ORDER_ID,
                TEST_AMOUNT, TEST_CURRENCY, TEST_PG_PAYMENT_KEY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("occurredAt");
        }
    }

    @Nested
    @DisplayName("DomainEvent contract")
    class DomainEventContract {

        @Test
        @DisplayName("should return PAYMENT as aggregate type")
        void shouldReturnPaymentAsAggregateType() {
            PaymentApprovedEvent event = createEvent();
            assertThat(event.aggregateType()).isEqualTo("PAYMENT");
        }

        @Test
        @DisplayName("should return paymentId as aggregate id")
        void shouldReturnPaymentIdAsAggregateId() {
            PaymentApprovedEvent event = createEvent();
            assertThat(event.aggregateId()).isEqualTo(TEST_PAYMENT_ID);
        }

        @Test
        @DisplayName("should return payment.approved as event type")
        void shouldReturnPaymentApprovedAsEventType() {
            PaymentApprovedEvent event = createEvent();
            assertThat(event.eventType()).isEqualTo("payment.approved");
        }
    }

    @Nested
    @DisplayName("Factory method")
    class FactoryMethod {

        @Test
        @DisplayName("should create event from Payment domain object")
        void shouldCreateFromPayment() {
            OrderId orderId = OrderId.generate();
            Payment payment = Payment.create(orderId, Money.krw(10000));
            payment.startProcessing(new PaymentMethod(PaymentMethodType.CARD, "card_token"));
            payment.approve("pg_tx_123", "pg_key_456");

            Instant before = Instant.now();
            PaymentApprovedEvent event = PaymentApprovedEvent.from(payment);
            Instant after = Instant.now();

            assertThat(event.eventId()).isNotNull();
            assertThat(event.paymentId()).isEqualTo(payment.getId().value().toString());
            assertThat(event.orderId()).isEqualTo(payment.getOrderId().value().toString());
            assertThat(event.amount()).isEqualByComparingTo(payment.getAmount().amount());
            assertThat(event.currency()).isEqualTo(payment.getAmount().currency().name());
            assertThat(event.pgPaymentKey()).isEqualTo(payment.getPgPaymentKey());
            assertThat(event.occurredAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should throw exception for null payment")
        void shouldThrowForNullPayment() {
            assertThatThrownBy(() -> PaymentApprovedEvent.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("payment");
        }
    }

    private PaymentApprovedEvent createEvent() {
        return new PaymentApprovedEvent(
            TEST_EVENT_ID, TEST_PAYMENT_ID, TEST_ORDER_ID,
            TEST_AMOUNT, TEST_CURRENCY, TEST_PG_PAYMENT_KEY, TEST_OCCURRED_AT);
    }
}
