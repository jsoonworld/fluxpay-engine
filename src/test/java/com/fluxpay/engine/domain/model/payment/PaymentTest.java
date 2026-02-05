package com.fluxpay.engine.domain.model.payment;

import com.fluxpay.engine.domain.exception.InvalidPaymentStateException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Payment entity.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("Payment")
class PaymentTest {

    private static final OrderId TEST_ORDER_ID = OrderId.generate();
    private static final Money TEST_AMOUNT = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
    private static final PaymentMethod TEST_PAYMENT_METHOD = PaymentMethod.card("card-token-123");
    private static final String TEST_PG_TRANSACTION_ID = "pg-txn-123";
    private static final String TEST_PG_PAYMENT_KEY = "pg-key-456";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create payment with READY status")
        void shouldCreatePaymentWithReadyStatus() {
            // When
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(payment.isReady()).isTrue();
        }

        @Test
        @DisplayName("should generate PaymentId on creation")
        void shouldGeneratePaymentIdOnCreation() {
            // When
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // Then
            assertThat(payment.getId()).isNotNull();
            assertThat(payment.getId().value()).isNotNull();
        }

        @Test
        @DisplayName("should set orderId and amount correctly")
        void shouldSetOrderIdAndAmountCorrectly() {
            // When
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // Then
            assertThat(payment.getOrderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(payment.getAmount()).isEqualTo(TEST_AMOUNT);
        }

        @Test
        @DisplayName("should set createdAt and updatedAt on creation")
        void shouldSetTimestampsOnCreation() {
            // Given
            Instant before = Instant.now();

            // When
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // Then
            Instant after = Instant.now();
            assertThat(payment.getCreatedAt()).isBetween(before, after);
            assertThat(payment.getUpdatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should throw exception for zero amount")
        void shouldThrowExceptionForZeroAmount() {
            // Given
            Money zeroAmount = Money.zero(Currency.KRW);

            // When & Then
            assertThatThrownBy(() -> Payment.create(TEST_ORDER_ID, zeroAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount")
                    .hasMessageContaining("zero");
        }

        @Test
        @DisplayName("should throw exception when orderId is null")
        void shouldThrowExceptionWhenOrderIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> Payment.create(null, TEST_AMOUNT))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Order ID");
        }

        @Test
        @DisplayName("should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            // When & Then
            assertThatThrownBy(() -> Payment.create(TEST_ORDER_ID, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Amount");
        }
    }

    @Nested
    @DisplayName("State Transitions - Valid")
    class ValidStateTransitions {

        @Test
        @DisplayName("should transition from READY to PROCESSING")
        void shouldTransitionFromReadyToProcessing() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // When
            payment.startProcessing(TEST_PAYMENT_METHOD);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(payment.isProcessing()).isTrue();
            assertThat(payment.getPaymentMethod()).isEqualTo(TEST_PAYMENT_METHOD);
        }

        @Test
        @DisplayName("should transition from PROCESSING to APPROVED")
        void shouldTransitionFromProcessingToApproved() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);

            // When
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.isApproved()).isTrue();
            assertThat(payment.getPgTransactionId()).isEqualTo(TEST_PG_TRANSACTION_ID);
            assertThat(payment.getPgPaymentKey()).isEqualTo(TEST_PG_PAYMENT_KEY);
            assertThat(payment.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition from APPROVED to CONFIRMED")
        void shouldTransitionFromApprovedToConfirmed() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);

            // When
            payment.confirm();

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(payment.isConfirmed()).isTrue();
            assertThat(payment.getConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("should update updatedAt on state transition")
        void shouldUpdateTimestampOnStateTransition() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            Instant originalUpdatedAt = payment.getUpdatedAt();

            // Small delay to ensure timestamp difference
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            // When
            payment.startProcessing(TEST_PAYMENT_METHOD);

            // Then
            assertThat(payment.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("State Transitions - Invalid")
    class InvalidStateTransitions {

        @Test
        @DisplayName("should throw exception when transitioning from READY to APPROVED")
        void shouldThrowExceptionWhenTransitioningFromReadyToApproved() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // When & Then
            assertThatThrownBy(() -> payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("READY")
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("should throw exception when transitioning from READY to CONFIRMED")
        void shouldThrowExceptionWhenTransitioningFromReadyToConfirmed() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // When & Then
            assertThatThrownBy(() -> payment.confirm())
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("READY")
                    .hasMessageContaining("CONFIRMED");
        }

        @Test
        @DisplayName("should throw exception when transitioning from PROCESSING to CONFIRMED")
        void shouldThrowExceptionWhenTransitioningFromProcessingToConfirmed() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);

            // When & Then
            assertThatThrownBy(() -> payment.confirm())
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("PROCESSING")
                    .hasMessageContaining("CONFIRMED");
        }

        @Test
        @DisplayName("should throw exception when transitioning from CONFIRMED to any state")
        void shouldThrowExceptionWhenTransitioningFromConfirmedToAnyState() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);
            payment.confirm();

            // When & Then
            assertThatThrownBy(() -> payment.startProcessing(TEST_PAYMENT_METHOD))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("CONFIRMED")
                    .hasMessageContaining("PROCESSING");
        }

        @Test
        @DisplayName("should throw exception when transitioning from FAILED to any state")
        void shouldThrowExceptionWhenTransitioningFromFailedToAnyState() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.fail("Payment declined");

            // When & Then
            assertThatThrownBy(() -> payment.startProcessing(TEST_PAYMENT_METHOD))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("FAILED")
                    .hasMessageContaining("PROCESSING");
        }
    }

    @Nested
    @DisplayName("Failure Handling")
    class FailureHandling {

        @Test
        @DisplayName("should fail payment from READY state")
        void shouldFailPaymentFromReadyState() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            String failureReason = "User cancelled";

            // When
            payment.fail(failureReason);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.isFailed()).isTrue();
            assertThat(payment.getFailureReason()).isEqualTo(failureReason);
        }

        @Test
        @DisplayName("should fail payment from PROCESSING state")
        void shouldFailPaymentFromProcessingState() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            String failureReason = "Card declined";

            // When
            payment.fail(failureReason);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.isFailed()).isTrue();
            assertThat(payment.getFailureReason()).isEqualTo(failureReason);
        }

        @Test
        @DisplayName("should fail payment from APPROVED state")
        void shouldFailPaymentFromApprovedState() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);
            String failureReason = "Confirmation timeout";

            // When
            payment.fail(failureReason);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.isFailed()).isTrue();
            assertThat(payment.getFailureReason()).isEqualTo(failureReason);
        }

        @Test
        @DisplayName("should not fail payment from CONFIRMED terminal state")
        void shouldNotFailPaymentFromConfirmedTerminalState() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);
            payment.confirm();

            // When & Then
            assertThatThrownBy(() -> payment.fail("Cannot fail confirmed payment"))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("CONFIRMED")
                    .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("should not fail payment from FAILED terminal state")
        void shouldNotFailPaymentFromFailedTerminalState() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.fail("First failure");

            // When & Then
            assertThatThrownBy(() -> payment.fail("Second failure"))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("should not fail payment from REFUNDED terminal state")
        void shouldNotFailPaymentFromRefundedTerminalState() {
            // Given - restore a refunded payment since we don't have refund() method yet
            PaymentId paymentId = PaymentId.generate();
            Instant now = Instant.now();
            Payment payment = Payment.restore(
                    paymentId, TEST_ORDER_ID, TEST_AMOUNT,
                    PaymentStatus.REFUNDED, TEST_PAYMENT_METHOD,
                    TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY,
                    null, now, now, now, now, null
            );

            // When & Then
            assertThatThrownBy(() -> payment.fail("Cannot fail refunded payment"))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("REFUNDED")
                    .hasMessageContaining("FAILED");
        }
    }

    @Nested
    @DisplayName("Approval Expiration")
    class ApprovalExpiration {

        @Test
        @DisplayName("should detect expired approval")
        void shouldDetectExpiredApproval() {
            // Given - restore a payment approved 2 hours ago
            PaymentId paymentId = PaymentId.generate();
            Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
            Payment payment = Payment.restore(
                    paymentId, TEST_ORDER_ID, TEST_AMOUNT,
                    PaymentStatus.APPROVED, TEST_PAYMENT_METHOD,
                    TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY,
                    null, twoHoursAgo, twoHoursAgo, twoHoursAgo, null, null
            );

            // When - check with 1 hour expiration duration
            boolean isExpired = payment.isApprovalExpired(Instant.now(), Duration.ofHours(1));

            // Then
            assertThat(isExpired).isTrue();
        }

        @Test
        @DisplayName("should not detect non-expired approval")
        void shouldNotDetectNonExpiredApproval() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);

            // When - check with 1 hour expiration duration
            boolean isExpired = payment.isApprovalExpired(Instant.now(), Duration.ofHours(1));

            // Then
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("should return false for non-approved payment")
        void shouldReturnFalseForNonApprovedPayment() {
            // Given
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // When
            boolean isExpired = payment.isApprovalExpired(Instant.now(), Duration.ofHours(1));

            // Then
            assertThat(isExpired).isFalse();
        }
    }

    @Nested
    @DisplayName("Restore from Database")
    class RestoreFromDatabase {

        @Test
        @DisplayName("should restore payment with all fields")
        void shouldRestorePaymentWithAllFields() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");
            Instant updatedAt = Instant.parse("2024-01-01T11:00:00Z");
            Instant approvedAt = Instant.parse("2024-01-01T10:30:00Z");
            Instant confirmedAt = Instant.parse("2024-01-01T10:45:00Z");

            // When
            Payment payment = Payment.restore(
                    paymentId,
                    TEST_ORDER_ID,
                    TEST_AMOUNT,
                    PaymentStatus.CONFIRMED,
                    TEST_PAYMENT_METHOD,
                    TEST_PG_TRANSACTION_ID,
                    TEST_PG_PAYMENT_KEY,
                    null,
                    createdAt,
                    updatedAt,
                    approvedAt,
                    confirmedAt,
                    null
            );

            // Then
            assertThat(payment.getId()).isEqualTo(paymentId);
            assertThat(payment.getOrderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(payment.getAmount()).isEqualTo(TEST_AMOUNT);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(payment.getPaymentMethod()).isEqualTo(TEST_PAYMENT_METHOD);
            assertThat(payment.getPgTransactionId()).isEqualTo(TEST_PG_TRANSACTION_ID);
            assertThat(payment.getPgPaymentKey()).isEqualTo(TEST_PG_PAYMENT_KEY);
            assertThat(payment.getCreatedAt()).isEqualTo(createdAt);
            assertThat(payment.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(payment.getApprovedAt()).isEqualTo(approvedAt);
            assertThat(payment.getConfirmedAt()).isEqualTo(confirmedAt);
        }

        @Test
        @DisplayName("should restore failed payment with failure reason")
        void shouldRestoreFailedPaymentWithFailureReason() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            String failureReason = "Insufficient funds";
            Instant now = Instant.now();

            // When
            Payment payment = Payment.restore(
                    paymentId,
                    TEST_ORDER_ID,
                    TEST_AMOUNT,
                    PaymentStatus.FAILED,
                    TEST_PAYMENT_METHOD,
                    null,
                    null,
                    failureReason,
                    now,
                    now,
                    null,
                    null,
                    now
            );

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo(failureReason);
        }

        @Test
        @DisplayName("should throw exception when restoring with null paymentId")
        void shouldThrowExceptionWhenRestoringWithNullPaymentId() {
            // When & Then
            assertThatThrownBy(() -> Payment.restore(
                    null,
                    TEST_ORDER_ID,
                    TEST_AMOUNT,
                    PaymentStatus.READY,
                    null,
                    null,
                    null,
                    null,
                    Instant.now(),
                    Instant.now(),
                    null,
                    null,
                    null
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Payment ID");
        }
    }

    @Nested
    @DisplayName("Status Check Helpers")
    class StatusCheckHelpers {

        @Test
        @DisplayName("isReady should return true only for READY status")
        void isReadyShouldReturnTrueOnlyForReadyStatus() {
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            assertThat(payment.isReady()).isTrue();
            assertThat(payment.isProcessing()).isFalse();
            assertThat(payment.isApproved()).isFalse();
            assertThat(payment.isConfirmed()).isFalse();
            assertThat(payment.isFailed()).isFalse();
        }

        @Test
        @DisplayName("isProcessing should return true only for PROCESSING status")
        void isProcessingShouldReturnTrueOnlyForProcessingStatus() {
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            assertThat(payment.isReady()).isFalse();
            assertThat(payment.isProcessing()).isTrue();
            assertThat(payment.isApproved()).isFalse();
            assertThat(payment.isConfirmed()).isFalse();
            assertThat(payment.isFailed()).isFalse();
        }

        @Test
        @DisplayName("isApproved should return true only for APPROVED status")
        void isApprovedShouldReturnTrueOnlyForApprovedStatus() {
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);
            assertThat(payment.isReady()).isFalse();
            assertThat(payment.isProcessing()).isFalse();
            assertThat(payment.isApproved()).isTrue();
            assertThat(payment.isConfirmed()).isFalse();
            assertThat(payment.isFailed()).isFalse();
        }

        @Test
        @DisplayName("isConfirmed should return true only for CONFIRMED status")
        void isConfirmedShouldReturnTrueOnlyForConfirmedStatus() {
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.startProcessing(TEST_PAYMENT_METHOD);
            payment.approve(TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY);
            payment.confirm();
            assertThat(payment.isReady()).isFalse();
            assertThat(payment.isProcessing()).isFalse();
            assertThat(payment.isApproved()).isFalse();
            assertThat(payment.isConfirmed()).isTrue();
            assertThat(payment.isFailed()).isFalse();
        }

        @Test
        @DisplayName("isFailed should return true only for FAILED status")
        void isFailedShouldReturnTrueOnlyForFailedStatus() {
            Payment payment = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            payment.fail("Test failure");
            assertThat(payment.isReady()).isFalse();
            assertThat(payment.isProcessing()).isFalse();
            assertThat(payment.isApproved()).isFalse();
            assertThat(payment.isConfirmed()).isFalse();
            assertThat(payment.isFailed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("two Payments with same id should be equal")
        void twoPaymentsWithSameIdShouldBeEqual() {
            // Given
            PaymentId paymentId = PaymentId.generate();
            Instant now = Instant.now();

            // When - Two payments with same ID but different attributes
            Payment payment1 = Payment.restore(
                    paymentId, TEST_ORDER_ID, TEST_AMOUNT,
                    PaymentStatus.READY, null,
                    null, null, null,
                    now, now, null, null, null
            );
            Payment payment2 = Payment.restore(
                    paymentId, OrderId.generate(), Money.krw(50000),
                    PaymentStatus.CONFIRMED, TEST_PAYMENT_METHOD,
                    TEST_PG_TRANSACTION_ID, TEST_PG_PAYMENT_KEY, null,
                    now, now, now, now, null
            );

            // Then - Payments with same ID are equal regardless of other attributes
            assertThat(payment1).isEqualTo(payment2);
            assertThat(payment1.hashCode()).isEqualTo(payment2.hashCode());
        }

        @Test
        @DisplayName("two Payments with different ids should not be equal")
        void twoPaymentsWithDifferentIdsShouldNotBeEqual() {
            // When
            Payment payment1 = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);
            Payment payment2 = Payment.create(TEST_ORDER_ID, TEST_AMOUNT);

            // Then
            assertThat(payment1).isNotEqualTo(payment2);
        }
    }
}
