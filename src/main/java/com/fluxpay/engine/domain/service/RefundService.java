package com.fluxpay.engine.domain.service;

import com.fluxpay.engine.domain.exception.InvalidRefundException;
import com.fluxpay.engine.domain.exception.PaymentNotFoundException;
import com.fluxpay.engine.domain.exception.RefundNotFoundException;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.domain.model.refund.Refund;
import com.fluxpay.engine.domain.model.refund.RefundId;
import com.fluxpay.engine.domain.model.refund.RefundStatus;
import com.fluxpay.engine.domain.port.outbound.PaymentRepository;
import com.fluxpay.engine.domain.port.outbound.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Domain service for Refund operations.
 * Handles refund creation, validation, and retrieval.
 */
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final int refundPeriodDays;
    private final int maxPartialRefunds;

    public RefundService(RefundRepository refundRepository, PaymentRepository paymentRepository,
                         int refundPeriodDays, int maxPartialRefunds) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.refundPeriodDays = refundPeriodDays;
        this.maxPartialRefunds = maxPartialRefunds;
    }

    /**
     * Creates a new refund for a payment.
     *
     * @param paymentId the payment ID to refund
     * @param amount the refund amount
     * @param reason optional reason for the refund
     * @return a Mono containing the created refund
     * @throws PaymentNotFoundException if the payment is not found
     * @throws InvalidRefundException if the refund is invalid
     */
    public Mono<Refund> createRefund(PaymentId paymentId, Money amount, String reason) {
        return paymentRepository.findById(paymentId)
            .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
            .flatMap(payment -> validateRefund(payment, amount))
            .flatMap(payment -> createAndSaveRefund(paymentId, amount, reason));
    }

    private Mono<Payment> validateRefund(Payment payment, Money amount) {
        // Check payment status
        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            return Mono.error(new InvalidRefundException(payment.getId(),
                "Payment must be in CONFIRMED status to refund. Current status: " + payment.getStatus(),
                "PAY_010"));
        }

        // Check refund period
        if (payment.getConfirmedAt() != null) {
            Instant refundDeadline = payment.getConfirmedAt().plus(Duration.ofDays(refundPeriodDays));
            if (Instant.now().isAfter(refundDeadline)) {
                return Mono.error(new InvalidRefundException(payment.getId(),
                    "Refund period expired. Refunds are only allowed within " + refundPeriodDays + " days of payment",
                    "PAY_008"));
            }
        }

        // Check refundable amount
        return refundRepository.getTotalRefundedAmount(payment.getId())
            .flatMap(totalRefunded -> {
                Money refundable = payment.getAmount().subtract(totalRefunded);
                if (amount.isGreaterThan(refundable)) {
                    return Mono.error(new InvalidRefundException(payment.getId(),
                        "Refund amount " + amount + " exceeds refundable amount " + refundable,
                        "PAY_007"));
                }
                return Mono.just(payment);
            })
            .flatMap(p -> checkPartialRefundLimit(p));
    }

    private Mono<Payment> checkPartialRefundLimit(Payment payment) {
        return refundRepository.findByPaymentId(payment.getId())
            .filter(r -> r.getStatus() != RefundStatus.FAILED)
            .count()
            .flatMap(count -> {
                if (count >= maxPartialRefunds) {
                    return Mono.error(new InvalidRefundException(payment.getId(),
                        "Maximum number of partial refunds (" + maxPartialRefunds + ") reached",
                        "PAY_011"));
                }
                return Mono.just(payment);
            });
    }

    private Mono<Refund> createAndSaveRefund(PaymentId paymentId, Money amount, String reason) {
        Refund refund = Refund.create(paymentId, amount, reason);

        return refundRepository.save(refund)
            .doOnSuccess(saved -> log.info("Refund created: id={}, paymentId={}, amount={}",
                saved.getId(), saved.getPaymentId(), saved.getAmount()));
    }

    /**
     * Gets a refund by its ID.
     *
     * @param refundId the refund ID
     * @return a Mono containing the refund
     * @throws RefundNotFoundException if the refund is not found
     */
    public Mono<Refund> getRefund(RefundId refundId) {
        return refundRepository.findById(refundId)
            .switchIfEmpty(Mono.error(new RefundNotFoundException(refundId)));
    }

    /**
     * Gets all refunds for a payment.
     *
     * @param paymentId the payment ID
     * @return a Flux of refunds
     */
    public Flux<Refund> getRefundsByPayment(PaymentId paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }

    /**
     * Gets the total refunded amount for a payment.
     *
     * @param paymentId the payment ID
     * @return a Mono containing the total refunded amount
     */
    public Mono<Money> getTotalRefundedAmount(PaymentId paymentId) {
        return refundRepository.getTotalRefundedAmount(paymentId);
    }
}
