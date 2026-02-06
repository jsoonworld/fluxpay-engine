package com.fluxpay.engine.domain.event;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RefundFailedEvent.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("RefundFailedEvent")
class RefundFailedEventTest {

    private static final String TEST_EVENT_ID = "evt_12345";
    private static final String TEST_REFUND_ID = "ref_12345";
    private static final String TEST_PAYMENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_ERROR_MESSAGE = "PG rejected the refund request";
    private static final Instant TEST_OCCURRED_AT = Instant.parse("2026-02-06T10:00:00Z");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create event with all required fields")
        void shouldCreateEventWithAllRequiredFields() {
            RefundFailedEvent event = new RefundFailedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_ERROR_MESSAGE,
                TEST_OCCURRED_AT
            );

            assertThat(event.eventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(event.refundId()).isEqualTo(TEST_REFUND_ID);
            assertThat(event.paymentId()).isEqualTo(TEST_PAYMENT_ID);
            assertThat(event.errorMessage()).isEqualTo(TEST_ERROR_MESSAGE);
            assertThat(event.occurredAt()).isEqualTo(TEST_OCCURRED_AT);
        }

        @Test
        @DisplayName("should throw exception for null eventId")
        void shouldThrowExceptionForNullEventId() {
            assertThatThrownBy(() -> new RefundFailedEvent(
                null,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_ERROR_MESSAGE,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("should throw exception for null refundId")
        void shouldThrowExceptionForNullRefundId() {
            assertThatThrownBy(() -> new RefundFailedEvent(
                TEST_EVENT_ID,
                null,
                TEST_PAYMENT_ID,
                TEST_ERROR_MESSAGE,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("refundId");
        }

        @Test
        @DisplayName("should throw exception for null paymentId")
        void shouldThrowExceptionForNullPaymentId() {
            assertThatThrownBy(() -> new RefundFailedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                null,
                TEST_ERROR_MESSAGE,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("paymentId");
        }

        @Test
        @DisplayName("should throw exception for null errorMessage")
        void shouldThrowExceptionForNullErrorMessage() {
            assertThatThrownBy(() -> new RefundFailedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                null,
                TEST_OCCURRED_AT
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("errorMessage");
        }

        @Test
        @DisplayName("should throw exception for null occurredAt")
        void shouldThrowExceptionForNullOccurredAt() {
            assertThatThrownBy(() -> new RefundFailedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_ERROR_MESSAGE,
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
        @DisplayName("should create event from failed Refund domain object")
        void shouldCreateEventFromFailedRefund() {
            PaymentId paymentId = PaymentId.generate();
            Money amount = Money.krw(5000);
            Refund refund = Refund.create(paymentId, amount, "Test reason");
            refund.startProcessing();
            String errorMessage = "PG service unavailable";
            refund.fail(errorMessage);

            Instant before = Instant.now();
            RefundFailedEvent event = RefundFailedEvent.from(refund);
            Instant after = Instant.now();

            assertThat(event.eventId()).isNotNull();
            assertThat(event.refundId()).isEqualTo(refund.getId().value());
            assertThat(event.paymentId()).isEqualTo(paymentId.value().toString());
            assertThat(event.errorMessage()).isEqualTo(errorMessage);
            assertThat(event.occurredAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should generate unique eventId for each event")
        void shouldGenerateUniqueEventId() {
            PaymentId paymentId = PaymentId.generate();
            Money amount = Money.krw(5000);
            Refund refund = Refund.create(paymentId, amount, "reason");
            refund.startProcessing();
            refund.fail("Error occurred");

            RefundFailedEvent event1 = RefundFailedEvent.from(refund);
            RefundFailedEvent event2 = RefundFailedEvent.from(refund);

            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }

        @Test
        @DisplayName("should throw exception for null refund")
        void shouldThrowExceptionForNullRefund() {
            assertThatThrownBy(() -> RefundFailedEvent.from(null))
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
            assertThat(RefundFailedEvent.class.isRecord()).isTrue();
        }

        @Test
        @DisplayName("should implement RefundEvent sealed interface")
        void shouldImplementRefundEventInterface() {
            RefundFailedEvent event = new RefundFailedEvent(
                TEST_EVENT_ID,
                TEST_REFUND_ID,
                TEST_PAYMENT_ID,
                TEST_ERROR_MESSAGE,
                TEST_OCCURRED_AT
            );

            assertThat(event).isInstanceOf(RefundEvent.class);
            assertThat(event).isInstanceOf(DomainEvent.class);
        }
    }
}
