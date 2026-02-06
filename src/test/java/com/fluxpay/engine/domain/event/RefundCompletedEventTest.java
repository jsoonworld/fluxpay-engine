package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RefundCompletedEvent.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("RefundCompletedEvent")
class RefundCompletedEventTest {

    private static final String TEST_EVENT_ID = "evt_12345";
    private static final String TEST_REFUND_ID = "ref_12345";
    private static final String TEST_PAYMENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("10000");
    private static final String TEST_CURRENCY = "KRW";
    private static final String TEST_PG_REFUND_ID = "pg_refund_abc123";
    private static final Instant TEST_OCCURRED_AT = Instant.parse("2026-02-06T10:00:00Z");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create event with all required fields")
        void shouldCreateEventWithAllRequiredFields() {
            RefundCompletedEvent event = new RefundCompletedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_PG_REFUND_ID,
                TEST_OCCURRED_AT
            );

            assertThat(event.eventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(event.refundId()).isEqualTo(TEST_REFUND_ID);
            assertThat(event.paymentId()).isEqualTo(TEST_PAYMENT_ID);
            assertThat(event.amount()).isEqualTo(TEST_AMOUNT);
            assertThat(event.currency()).isEqualTo(TEST_CURRENCY);
            assertThat(event.pgRefundId()).isEqualTo(TEST_PG_REFUND_ID);
            assertThat(event.occurredAt()).isEqualTo(TEST_OCCURRED_AT);
        }

        @Test
        @DisplayName("should throw exception for null eventId")
        void shouldThrowExceptionForNullEventId() {
            assertThatThrownBy(() -> new RefundCompletedEvent(
                null,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_PG_REFUND_ID,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("should throw exception for null refundId")
        void shouldThrowExceptionForNullRefundId() {
            assertThatThrownBy(() -> new RefundCompletedEvent(
                TEST_EVENT_ID,
                null,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_PG_REFUND_ID,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("refundId");
        }

        @Test
        @DisplayName("should throw exception for null paymentId")
        void shouldThrowExceptionForNullPaymentId() {
            assertThatThrownBy(() -> new RefundCompletedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                null,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_PG_REFUND_ID,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("paymentId");
        }

        @Test
        @DisplayName("should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            assertThatThrownBy(() -> new RefundCompletedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                null,
                TEST_CURRENCY,
                TEST_PG_REFUND_ID,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception for null currency")
        void shouldThrowExceptionForNullCurrency() {
            assertThatThrownBy(() -> new RefundCompletedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                null,
                TEST_PG_REFUND_ID,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should throw exception for null pgRefundId")
        void shouldThrowExceptionForNullPgRefundId() {
            assertThatThrownBy(() -> new RefundCompletedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                null,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pgRefundId");
        }

        @Test
        @DisplayName("should throw exception for null occurredAt")
        void shouldThrowExceptionForNullOccurredAt() {
            assertThatThrownBy(() -> new RefundCompletedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_PG_REFUND_ID,
                null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("occurredAt");
        }
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("should create event from completed Refund domain object")
        void shouldCreateEventFromCompletedRefund() {
            PaymentId paymentId = PaymentId.generate();
            Money amount = Money.krw(5000);
            Refund refund = Refund.create(paymentId, amount, "Test reason");
            refund.startProcessing();
            String pgRefundId = "pg_refund_test123";
            refund.complete(pgRefundId);

            Instant before = Instant.now();
            RefundCompletedEvent event = RefundCompletedEvent.from(refund);
            Instant after = Instant.now();

            assertThat(event.eventId()).isNotNull();
            assertThat(event.refundId()).isEqualTo(refund.getId().value());
            assertThat(event.paymentId()).isEqualTo(paymentId.value().toString());
            assertThat(event.amount()).isEqualTo(amount.amount());
            assertThat(event.currency()).isEqualTo(amount.currency().name());
            assertThat(event.pgRefundId()).isEqualTo(pgRefundId);
            assertThat(event.occurredAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should generate unique eventId for each event")
        void shouldGenerateUniqueEventId() {
            PaymentId paymentId = PaymentId.generate();
            Money amount = Money.krw(5000);
            Refund refund = Refund.create(paymentId, amount, "reason");
            refund.startProcessing();
            refund.complete("pg_refund_123");

            RefundCompletedEvent event1 = RefundCompletedEvent.from(refund);
            RefundCompletedEvent event2 = RefundCompletedEvent.from(refund);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }

        @Test
        @DisplayName("should throw exception for null refund")
        void shouldThrowExceptionForNullRefund() {
            assertThatThrownBy(() -> RefundCompletedEvent.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("refund");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should be a record type")
        void shouldBeRecordType() {
            assertThat(RefundCompletedEvent.class.isRecord()).isTrue();
        }

        @Test
        @DisplayName("should implement RefundEvent sealed interface")
        void shouldImplementRefundEventInterface() {
            RefundCompletedEvent event = new RefundCompletedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_PG_REFUND_ID,
                TEST_OCCURRED_AT
            );

            assertThat(event).isInstanceOf(RefundEvent.class);
            assertThat(event).isInstanceOf(DomainEvent.class);
        }
    }
}
