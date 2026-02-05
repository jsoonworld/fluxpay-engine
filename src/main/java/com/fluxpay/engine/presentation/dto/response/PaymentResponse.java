package com.fluxpay.engine.presentation.dto.response;

import com.fluxpay.engine.domain.model.payment.Payment;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Payment entity.
 * Used to transfer payment data to the presentation layer.
 */
public record PaymentResponse(
    String paymentId,
    String orderId,
    String status,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String pgTransactionId,
    String failureReason,
    Instant approvedAt,
    Instant confirmedAt,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Creates a PaymentResponse from a Payment domain entity.
     *
     * @param payment the Payment domain entity
     * @return a PaymentResponse DTO
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId().toString(),
            payment.getOrderId().value().toString(),
            payment.getStatus().name(),
            payment.getAmount().amount(),
            payment.getAmount().currency().name(),
            payment.getPaymentMethod() != null
                ? payment.getPaymentMethod().type().name()
                : null,
            payment.getPgTransactionId(),
            payment.getFailureReason(),
            payment.getApprovedAt(),
            payment.getConfirmedAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
