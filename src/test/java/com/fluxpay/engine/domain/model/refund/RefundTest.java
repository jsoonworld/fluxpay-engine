package com.fluxpay.engine.domain.model.refund;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Refund entity.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("Refund")
class RefundTest {

    private static final PaymentId TEST_PAYMENT_ID = PaymentId.generate();
    private static final Money TEST_AMOUNT = Money.krw(5000);
    private static final String TEST_REASON = "Customer requested refund";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create refund with REQUESTED status")
        void shouldCreateRefundWithRequestedStatus() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        }

        @Test
        @DisplayName("should generate RefundId on creation")
        void shouldGenerateRefundIdOnCreation() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);

            assertThat(refund.getId()).isNotNull();
            assertThat(refund.getId().value()).startsWith("ref_");
        }

        @Test
        @DisplayName("should set paymentId, amount, and reason correctly")
        void shouldSetFieldsCorrectly() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);

            assertThat(refund.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
            assertThat(refund.getAmount()).isEqualTo(TEST_AMOUNT);
            assertThat(refund.getReason()).isEqualTo(TEST_REASON);
        }

        @Test
        @DisplayName("should set requestedAt on creation")
        void shouldSetRequestedAtOnCreation() {
            Instant before = Instant.now();
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);
            Instant after = Instant.now();

            assertThat(refund.getRequestedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should allow null reason")
        void shouldAllowNullReason() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, null);

            assertThat(refund.getReason()).isNull();
        }

        @Test
        @DisplayName("should throw exception for null paymentId")
        void shouldThrowExceptionForNullPaymentId() {
            assertThatThrownBy(() -> Refund.create(null, TEST_AMOUNT, TEST_REASON))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Payment ID");
        }

        @Test
        @DisplayName("should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            assertThatThrownBy(() -> Refund.create(TEST_PAYMENT_ID, null, TEST_REASON))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Amount");
        }

        @Test
        @DisplayName("should throw exception for zero amount")
        void shouldThrowExceptionForZeroAmount() {
            Money zeroAmount = Money.zero(Currency.KRW);

            assertThatThrownBy(() -> Refund.create(TEST_PAYMENT_ID, zeroAmount, TEST_REASON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount")
                .hasMessageContaining("zero");
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition from REQUESTED to PROCESSING")
        void shouldTransitionFromRequestedToProcessing() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);

            refund.startProcessing();

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition from PROCESSING to COMPLETED")
        void shouldTransitionFromProcessingToCompleted() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);
            refund.startProcessing();
            String pgRefundId = "pg-refund-123";

            refund.complete(pgRefundId);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(refund.getPgRefundId()).isEqualTo(pgRefundId);
            assertThat(refund.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition from PROCESSING to FAILED")
        void shouldTransitionFromProcessingToFailed() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);
            refund.startProcessing();
            String errorMessage = "PG refund rejected";

            refund.fail(errorMessage);

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
            assertThat(refund.getErrorMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("should throw exception when starting processing from non-REQUESTED state")
        void shouldThrowExceptionWhenStartingProcessingFromNonRequestedState() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);
            refund.startProcessing();

            assertThatThrownBy(() -> refund.startProcessing())
                .isInstanceOf(InvalidRefundStateException.class)
                .hasMessageContaining("PROCESSING")
                .hasMessageContaining("PROCESSING");
        }

        @Test
        @DisplayName("should throw exception when completing from non-PROCESSING state")
        void shouldThrowExceptionWhenCompletingFromNonProcessingState() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);

            assertThatThrownBy(() -> refund.complete("pg-refund-123"))
                .isInstanceOf(InvalidRefundStateException.class)
                .hasMessageContaining("REQUESTED")
                .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("should throw exception when failing from non-PROCESSING state")
        void shouldThrowExceptionWhenFailingFromNonProcessingState() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);

            assertThatThrownBy(() -> refund.fail("Error"))
                .isInstanceOf(InvalidRefundStateException.class)
                .hasMessageContaining("REQUESTED")
                .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("should not allow transition from COMPLETED state")
        void shouldNotAllowTransitionFromCompletedState() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);
            refund.startProcessing();
            refund.complete("pg-refund-123");

            assertThatThrownBy(() -> refund.fail("Error"))
                .isInstanceOf(InvalidRefundStateException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("should not allow transition from FAILED state")
        void shouldNotAllowTransitionFromFailedState() {
            Refund refund = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);
            refund.startProcessing();
            refund.fail("First failure");

            assertThatThrownBy(() -> refund.complete("pg-refund-123"))
                .isInstanceOf(InvalidRefundStateException.class)
                .hasMessageContaining("FAILED")
                .hasMessageContaining("COMPLETED");
        }
    }

    @Nested
    @DisplayName("Restore from Database")
    class RestoreFromDatabase {

        @Test
        @DisplayName("should restore refund with all fields")
        void shouldRestoreRefundWithAllFields() {
            RefundId refundId = RefundId.generate();
            Instant requestedAt = Instant.parse("2024-01-01T10:00:00Z");
            Instant completedAt = Instant.parse("2024-01-01T10:05:00Z");
            String pgRefundId = "pg-refund-123";

            Refund refund = Refund.restore(
                refundId,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                RefundStatus.COMPLETED,
                TEST_REASON,
                pgRefundId,
                null,
                requestedAt,
                completedAt
            );

            assertThat(refund.getId()).isEqualTo(refundId);
            assertThat(refund.getPaymentId()).isEqualTo(TEST_PAYMENT_ID);
            assertThat(refund.getAmount()).isEqualTo(TEST_AMOUNT);
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(refund.getReason()).isEqualTo(TEST_REASON);
            assertThat(refund.getPgRefundId()).isEqualTo(pgRefundId);
            assertThat(refund.getRequestedAt()).isEqualTo(requestedAt);
            assertThat(refund.getCompletedAt()).isEqualTo(completedAt);
        }

        @Test
        @DisplayName("should restore failed refund with error message")
        void shouldRestoreFailedRefundWithErrorMessage() {
            RefundId refundId = RefundId.generate();
            String errorMessage = "PG rejected refund";

            Refund refund = Refund.restore(
                refundId,
                TEST_PAYMENT_ID,
                TEST_AMOUNT,
                RefundStatus.FAILED,
                TEST_REASON,
                null,
                errorMessage,
                Instant.now(),
                null
            );

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
            assertThat(refund.getErrorMessage()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("two refunds with same id should be equal")
        void twoRefundsWithSameIdShouldBeEqual() {
            RefundId refundId = RefundId.generate();
            Instant now = Instant.now();

            Refund refund1 = Refund.restore(
                refundId, TEST_PAYMENT_ID, TEST_AMOUNT,
                RefundStatus.REQUESTED, TEST_REASON,
                null, null, now, null
            );
            Refund refund2 = Refund.restore(
                refundId, PaymentId.generate(), Money.krw(10000),
                RefundStatus.COMPLETED, "Different reason",
                "pg-123", null, now, now
            );

            assertThat(refund1).isEqualTo(refund2);
            assertThat(refund1.hashCode()).isEqualTo(refund2.hashCode());
        }

        @Test
        @DisplayName("two refunds with different ids should not be equal")
        void twoRefundsWithDifferentIdsShouldNotBeEqual() {
            Refund refund1 = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);
            Refund refund2 = Refund.create(TEST_PAYMENT_ID, TEST_AMOUNT, TEST_REASON);

            assertThat(refund1).isNotEqualTo(refund2);
        }
    }
}
