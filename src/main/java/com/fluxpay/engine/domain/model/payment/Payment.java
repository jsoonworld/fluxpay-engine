package com.fluxpay.engine.domain.model.payment;

import com.fluxpay.engine.domain.exception.InvalidPaymentStateException;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a Payment.
 * Tracks the payment lifecycle from creation through approval and confirmation.
 *
 * State transitions:
 * READY -> PROCESSING (startProcessing)
 * PROCESSING -> APPROVED (approve)
 * APPROVED -> CONFIRMED (confirm)
 * READY/PROCESSING/APPROVED -> FAILED (fail)
 * CONFIRMED -> REFUNDED (refund)
 */
public class Payment {

    private final PaymentId id;
    private final OrderId orderId;
    private final Money amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String pgTransactionId;
    private String pgPaymentKey;
    private String failureReason;
    private Instant approvedAt;
    private Instant confirmedAt;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant failedAt;

    private Payment(PaymentId id, OrderId orderId, Money amount, PaymentStatus status,
                    PaymentMethod paymentMethod, String pgTransactionId, String pgPaymentKey,
                    String failureReason, Instant createdAt, Instant updatedAt,
                    Instant approvedAt, Instant confirmedAt, Instant failedAt) {
        this.id = Objects.requireNonNull(id, "Payment ID is required");
        this.orderId = Objects.requireNonNull(orderId, "Order ID is required");
        this.amount = Objects.requireNonNull(amount, "Amount is required");
        this.status = Objects.requireNonNull(status, "Status is required");
        this.paymentMethod = paymentMethod;
        this.pgTransactionId = pgTransactionId;
        this.pgPaymentKey = pgPaymentKey;
        this.failureReason = failureReason;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt is required");
        this.approvedAt = approvedAt;
        this.confirmedAt = confirmedAt;
        this.failedAt = failedAt;
    }

    /**
     * Creates a new Payment in READY status.
     *
     * @param orderId the order ID this payment is for
     * @param amount the payment amount
     * @return a new Payment in READY status
     * @throws NullPointerException if orderId or amount is null
     * @throws IllegalArgumentException if amount is zero
     */
    public static Payment create(OrderId orderId, Money amount) {
        Objects.requireNonNull(orderId, "Order ID is required");
        Objects.requireNonNull(amount, "Amount is required");
        if (amount.isZero()) {
            throw new IllegalArgumentException("Payment amount cannot be zero");
        }
        Instant now = Instant.now();
        return new Payment(
                PaymentId.generate(),
                orderId,
                amount,
                PaymentStatus.READY,
                null,
                null,
                null,
                null,
                now,
                now,
                null,
                null,
                null
        );
    }

    /**
     * Restores a Payment from persistence with all fields.
     *
     * @param id the payment ID
     * @param orderId the order ID
     * @param amount the payment amount
     * @param status the current status
     * @param paymentMethod the payment method (may be null for READY status)
     * @param pgTransactionId the PG transaction ID (may be null)
     * @param pgPaymentKey the PG payment key (may be null)
     * @param failureReason the failure reason (may be null)
     * @param createdAt when the payment was created
     * @param updatedAt when the payment was last updated
     * @param approvedAt when the payment was approved (may be null)
     * @param confirmedAt when the payment was confirmed (may be null)
     * @param failedAt when the payment failed (may be null)
     * @return a restored Payment
     */
    public static Payment restore(PaymentId id, OrderId orderId, Money amount,
                                   PaymentStatus status, PaymentMethod paymentMethod,
                                   String pgTransactionId, String pgPaymentKey,
                                   String failureReason, Instant createdAt, Instant updatedAt,
                                   Instant approvedAt, Instant confirmedAt, Instant failedAt) {
        return new Payment(id, orderId, amount, status, paymentMethod,
                pgTransactionId, pgPaymentKey, failureReason,
                createdAt, updatedAt, approvedAt, confirmedAt, failedAt);
    }

    /**
     * Starts processing the payment with the given payment method.
     * Transitions from READY to PROCESSING.
     *
     * @param paymentMethod the payment method to use
     * @throws InvalidPaymentStateException if transition is not allowed
     * @throws NullPointerException if paymentMethod is null
     */
    public void startProcessing(PaymentMethod paymentMethod) {
        validateStateTransition(PaymentStatus.PROCESSING);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method is required");
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the payment as approved by the PG.
     * Transitions from PROCESSING to APPROVED.
     *
     * @param pgTransactionId the PG transaction ID
     * @throws InvalidPaymentStateException if transition is not allowed
     * @throws NullPointerException if pgTransactionId is null
     */
    public void approve(String pgTransactionId) {
        approve(pgTransactionId, null);
    }

    /**
     * Marks the payment as approved by the PG.
     * Transitions from PROCESSING to APPROVED.
     *
     * @param pgTransactionId the PG transaction ID
     * @param pgPaymentKey the PG payment key
     * @throws InvalidPaymentStateException if transition is not allowed
     * @throws NullPointerException if pgTransactionId is null
     */
    public void approve(String pgTransactionId, String pgPaymentKey) {
        validateStateTransition(PaymentStatus.APPROVED);
        this.pgTransactionId = Objects.requireNonNull(pgTransactionId, "PG transaction ID is required");
        this.pgPaymentKey = pgPaymentKey;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Confirms the payment (captures the funds).
     * Transitions from APPROVED to CONFIRMED.
     *
     * @throws InvalidPaymentStateException if transition is not allowed
     */
    public void confirm() {
        validateStateTransition(PaymentStatus.CONFIRMED);
        this.status = PaymentStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the payment as failed with a reason.
     * Can be called from READY, PROCESSING, or APPROVED states.
     *
     * @param reason the failure reason
     * @throws InvalidPaymentStateException if called from a terminal state
     */
    public void fail(String reason) {
        if (!status.canTransitionTo(PaymentStatus.FAILED)) {
            throw new InvalidPaymentStateException(status, PaymentStatus.FAILED);
        }
        this.failureReason = reason;
        this.status = PaymentStatus.FAILED;
        this.failedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Refunds the payment.
     * Transitions from CONFIRMED to REFUNDED.
     *
     * @throws InvalidPaymentStateException if transition is not allowed
     */
    public void refund() {
        validateStateTransition(PaymentStatus.REFUNDED);
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the approval has expired based on the given expiration duration.
     *
     * @param now the current time
     * @param expirationDuration the duration after which approval expires
     * @return true if the payment is approved and the approval has expired
     */
    public boolean isApprovalExpired(Instant now, Duration expirationDuration) {
        if (status != PaymentStatus.APPROVED || approvedAt == null) {
            return false;
        }
        return now.isAfter(approvedAt.plus(expirationDuration));
    }

    private void validateStateTransition(PaymentStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new InvalidPaymentStateException(status, targetStatus);
        }
    }

    // Status check helpers

    /**
     * Returns true if the payment is in READY status.
     */
    public boolean isReady() {
        return status == PaymentStatus.READY;
    }

    /**
     * Returns true if the payment is in PROCESSING status.
     */
    public boolean isProcessing() {
        return status == PaymentStatus.PROCESSING;
    }

    /**
     * Returns true if the payment is in APPROVED status.
     */
    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    /**
     * Returns true if the payment is in CONFIRMED status.
     */
    public boolean isConfirmed() {
        return status == PaymentStatus.CONFIRMED;
    }

    /**
     * Returns true if the payment is in FAILED status.
     */
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    /**
     * Returns true if the payment is in REFUNDED status.
     */
    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    // Getters

    public PaymentId getId() {
        return id;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * Alias for getPaymentMethod() for backward compatibility.
     */
    public PaymentMethod getMethod() {
        return paymentMethod;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public String getPgPaymentKey() {
        return pgPaymentKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Payment[id=" + id + ", orderId=" + orderId + ", amount=" + amount +
                ", status=" + status + "]";
    }
}
