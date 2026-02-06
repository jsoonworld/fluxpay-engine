package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import com.fluxpay.engine.domain.model.refund.RefundId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RefundRequestedEvent.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("RefundRequestedEvent")
class RefundRequestedEventTest {

    private static final String TEST_EVENT_ID = "evt_12345";
    private static final String TEST_REFUND_ID = "ref_12345";
    private static final String TEST_PAYMENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("10000");
    private static final String TEST_CURRENCY = "KRW";
    private static final String TEST_REASON = "Customer requested refund";
    private static final Instant TEST_OCCURRED_AT = Instant.parse("2026-02-06T10:00:00Z");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create event with all required fields")
        void shouldCreateEventWithAllRequiredFields() {
            RefundRequestedEvent event = new RefundRequestedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_REASON,
                TEST_OCCURRED_AT
            );

            assertThat(event.eventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(event.refundId()).isEqualTo(TEST_REFUND_ID);
            assertThat(event.paymentId()).isEqualTo(TEST_PAYMENT_ID);
            assertThat(event.amount()).isEqualTo(TEST_AMOUNT);
            assertThat(event.currency()).isEqualTo(TEST_CURRENCY);
            assertThat(event.reason()).isEqualTo(TEST_REASON);
            assertThat(event.occurredAt()).isEqualTo(TEST_OCCURRED_AT);
        }

        @Test
        @DisplayName("should allow null reason")
        void shouldAllowNullReason() {
            RefundRequestedEvent event = new RefundRequestedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                null,
                TEST_OCCURRED_AT
            );

            assertThat(event.reason()).isNull();
        }

        @Test
        @DisplayName("should throw exception for null eventId")
        void shouldThrowExceptionForNullEventId() {
            assertThatThrownBy(() -> new RefundRequestedEvent(
                null,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_REASON,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("should throw exception for null refundId")
        void shouldThrowExceptionForNullRefundId() {
            assertThatThrownBy(() -> new RefundRequestedEvent(
                TEST_EVENT_ID,
                null,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_REASON,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("refundId");
        }

        @Test
        @DisplayName("should throw exception for null paymentId")
        void shouldThrowExceptionForNullPaymentId() {
            assertThatThrownBy(() -> new RefundRequestedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                null,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_REASON,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("paymentId");
        }

        @Test
        @DisplayName("should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            assertThatThrownBy(() -> new RefundRequestedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                null,
                TEST_CURRENCY,
                TEST_REASON,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw exception for null currency")
        void shouldThrowExceptionForNullCurrency() {
            assertThatThrownBy(() -> new RefundRequestedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                null,
                TEST_REASON,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
        }

        @Test
        @DisplayName("should throw exception for null occurredAt")
        void shouldThrowExceptionForNullOccurredAt() {
            assertThatThrownBy(() -> new RefundRequestedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_REASON,
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
        @DisplayName("should create event from Refund domain object")
        void shouldCreateEventFromRefund() {
            PaymentId paymentId = PaymentId.generate();
            Money amount = Money.krw(5000);
            String reason = "Test refund reason";
            Refund refund = Refund.create(paymentId, amount, reason);

            Instant before = Instant.now();
            RefundRequestedEvent event = RefundRequestedEvent.from(refund);
            Instant after = Instant.now();

            assertThat(event.eventId()).isNotNull();
            assertThat(event.refundId()).isEqualTo(refund.getId().value());
            assertThat(event.paymentId()).isEqualTo(paymentId.value().toString());
            assertThat(event.amount()).isEqualTo(amount.amount());
            assertThat(event.currency()).isEqualTo(amount.currency().name());
            assertThat(event.reason()).isEqualTo(reason);
            assertThat(event.occurredAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should generate unique eventId for each event")
        void shouldGenerateUniqueEventId() {
            PaymentId paymentId = PaymentId.generate();
            Money amount = Money.krw(5000);
            Refund refund = Refund.create(paymentId, amount, "reason");

            RefundRequestedEvent event1 = RefundRequestedEvent.from(refund);
            RefundRequestedEvent event2 = RefundRequestedEvent.from(refund);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }

        @Test
        @DisplayName("should throw exception for null refund")
        void shouldThrowExceptionForNullRefund() {
            assertThatThrownBy(() -> RefundRequestedEvent.from(null))
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
            assertThat(RefundRequestedEvent.class.isRecord()).isTrue();
        }

        @Test
        @DisplayName("should implement RefundEvent sealed interface")
        void shouldImplementRefundEventInterface() {
            RefundRequestedEvent event = new RefundRequestedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                TEST_CURRENCY,
                TEST_REASON,
                TEST_OCCURRED_AT
            );

            assertThat(event).isInstanceOf(RefundEvent.class);
            assertThat(event).isInstanceOf(DomainEvent.class);
        }
    }
}
