package com.fluxpay.engine.presentation.dto.response;

import com.fluxpay.engine.domain.model.refund.Refund;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Refund entity.
 * Used to transfer refund data to the presentation layer.
 */
public record RefundResponse(
    String refundId,
    String paymentId,
    BigDecimal amount,
    String currency,
    String status,
    String reason,
    String pgRefundId,
    String errorMessage,
    Instant requestedAt,
    Instant completedAt
) {
    /**
     * Creates a RefundResponse from a Refund domain entity.
     *
     * @param refund the Refund domain entity
     * @return a RefundResponse DTO
     */
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
            refund.getId().value(),
            refund.getPaymentId().toString(),
            refund.getAmount().amount(),
            refund.getAmount().currency().name(),
            refund.getStatus().name(),
            refund.getReason(),
            refund.getPgRefundId(),
            refund.getErrorMessage(),
            refund.getRequestedAt(),
            refund.getCompletedAt()
        );
    }
}
