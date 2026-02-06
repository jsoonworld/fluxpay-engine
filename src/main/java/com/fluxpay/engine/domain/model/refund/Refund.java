package com.fluxpay.engine.domain.model.refund;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a Refund.
 * Tracks the refund lifecycle from request through processing and completion.
 *
 * State transitions:
 * REQUESTED -> PROCESSING (startProcessing)
 * PROCESSING -> COMPLETED (complete)
 * PROCESSING -> FAILED (fail)
 */
public class Refund {

    private final RefundId id;
    private final PaymentId paymentId;
    private final Money amount;
    private RefundStatus status;
    private final String reason;
    private String pgRefundId;
    private String errorMessage;
    private final Instant requestedAt;
    private Instant completedAt;

    private Refund(RefundId id, PaymentId paymentId, Money amount, RefundStatus status,
                   String reason, String pgRefundId, String errorMessage,
                   Instant requestedAt, Instant completedAt) {
        this.id = Objects.requireNonNull(id, "Refund ID is required");
        this.paymentId = Objects.requireNonNull(paymentId, "Payment ID is required");
        this.amount = Objects.requireNonNull(amount, "Amount is required");
        this.status = Objects.requireNonNull(status, "Status is required");
        this.reason = reason;
        this.pgRefundId = pgRefundId;
        this.errorMessage = errorMessage;
        this.requestedAt = Objects.requireNonNull(requestedAt, "RequestedAt is required");
        this.completedAt = completedAt;
    }

    /**
     * Creates a new Refund in REQUESTED status.
     *
     * @param paymentId the payment ID to refund
     * @param amount the refund amount
     * @param reason the reason for the refund (optional)
     * @return a new Refund in REQUESTED status
     * @throws NullPointerException if paymentId or amount is null
     * @throws IllegalArgumentException if amount is zero
     */
    public static Refund create(PaymentId paymentId, Money amount, String reason) {
        Objects.requireNonNull(paymentId, "Payment ID is required");
        Objects.requireNonNull(amount, "Amount is required");
        if (amount.isZero()) {
            throw new IllegalArgumentException("Refund amount cannot be zero");
        }

        return new Refund(
            RefundId.generate(),
            paymentId,
            amount,
            RefundStatus.REQUESTED,
            reason,
            null,
            null,
            Instant.now(),
            null
        );
    }

    /**
     * Restores a Refund from persistence with all fields.
     */
    public static Refund restore(RefundId id, PaymentId paymentId, Money amount,
                                  RefundStatus status, String reason, String pgRefundId,
                                  String errorMessage, Instant requestedAt, Instant completedAt) {
        return new Refund(id, paymentId, amount, status, reason, pgRefundId,
            errorMessage, requestedAt, completedAt);
    }

    /**
     * Starts processing the refund with the PG.
     * Transitions from REQUESTED to PROCESSING.
     *
     * @throws InvalidRefundStateException if transition is not allowed
     */
    public void startProcessing() {
        validateStateTransition(RefundStatus.PROCESSING);
        this.status = RefundStatus.PROCESSING;
    }

    /**
     * Marks the refund as completed.
     * Transitions from PROCESSING to COMPLETED.
     *
     * @param pgRefundId the PG refund transaction ID
     * @throws InvalidRefundStateException if transition is not allowed
     */
    public void complete(String pgRefundId) {
        validateStateTransition(RefundStatus.COMPLETED);
        this.pgRefundId = pgRefundId;
        this.status = RefundStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Marks the refund as failed.
     * Transitions from PROCESSING to FAILED.
     *
     * @param errorMessage the failure reason
     * @throws InvalidRefundStateException if transition is not allowed
     */
    public void fail(String errorMessage) {
        validateStateTransition(RefundStatus.FAILED);
        this.errorMessage = errorMessage;
        this.status = RefundStatus.FAILED;
    }

    private void validateStateTransition(RefundStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new InvalidRefundStateException(status, targetStatus);
        }
    }

    // Getters

    public RefundId getId() {
        return id;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public Money getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getPgRefundId() {
        return pgRefundId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Refund refund = (Refund) o;
        return Objects.equals(id, refund.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Refund[id=" + id + ", paymentId=" + paymentId + ", amount=" + amount +
            ", status=" + status + "]";
    }
}
