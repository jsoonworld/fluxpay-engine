package com.fluxpay.engine.domain.service;

import com.fluxpay.engine.domain.exception.PaymentNotFoundException;
import com.fluxpay.engine.domain.exception.PgClientException;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.domain.port.outbound.PaymentRepository;
import com.fluxpay.engine.domain.port.outbound.PgClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Domain service for Payment operations.
 * Handles payment creation, PG approval, confirmation, and failure handling.
 */
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PgClient pgClient;

    public PaymentService(PaymentRepository paymentRepository, PgClient pgClient) {
        this.paymentRepository = paymentRepository;
        this.pgClient = pgClient;
    }

    /**
     * Creates a new payment for an order.
     *
     * @param orderId the order ID to create payment for
     * @param amount the payment amount
     * @return a Mono containing the created payment in READY status
     */
    public Mono<Payment> createPayment(OrderId orderId, Money amount) {
        Payment payment = Payment.create(orderId, amount);

        return paymentRepository.save(payment)
            .doOnSuccess(savedPayment -> log.info("Payment created: id={}, orderId={}, amount={}",
                savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getAmount()));
    }

    /**
     * Gets a payment by its ID.
     *
     * @param paymentId the payment ID
     * @return a Mono containing the payment
     * @throws PaymentNotFoundException if the payment is not found
     */
    public Mono<Payment> getPayment(PaymentId paymentId) {
        return paymentRepository.findById(paymentId)
            .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
            .doOnSuccess(payment -> log.debug("Payment retrieved: id={}", payment.getId()));
    }

    /**
     * Gets a payment by its order ID.
     *
     * @param orderId the order ID
     * @return a Mono containing the payment
     * @throws PaymentNotFoundException if no payment is found for the order
     */
    public Mono<Payment> getPaymentByOrderId(OrderId orderId) {
        return paymentRepository.findByOrderId(orderId)
            .switchIfEmpty(Mono.error(new PaymentNotFoundException(
                "Payment not found for order: " + orderId)))
            .doOnSuccess(payment -> log.debug("Payment retrieved by orderId: orderId={}, paymentId={}",
                orderId, payment.getId()));
    }

    /**
     * Requests PG approval for a payment.
     * Transitions the payment from READY to PROCESSING, then to APPROVED or FAILED.
     *
     * @param paymentId the payment ID
     * @param method the payment method to use
     * @return a Mono containing the payment with updated status (APPROVED or FAILED)
     * @throws PaymentNotFoundException if the payment is not found
     */
    public Mono<Payment> requestApproval(PaymentId paymentId, PaymentMethod method) {
        return paymentRepository.findById(paymentId)
            .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
            .flatMap(payment -> {
                payment.startProcessing(method);
                return paymentRepository.save(payment);
            })
            .flatMap(payment -> requestPgApproval(payment, method))
            .doOnSuccess(payment -> {
                if (payment.getStatus() == PaymentStatus.APPROVED) {
                    log.info("Payment approved: id={}, pgTransactionId={}",
                        payment.getId(), payment.getPgTransactionId());
                } else {
                    log.warn("Payment failed: id={}, reason={}",
                        payment.getId(), payment.getFailureReason());
                }
            });
    }

    private Mono<Payment> requestPgApproval(Payment payment, PaymentMethod method) {
        return pgClient.requestApproval(
                payment.getOrderId().value().toString(),
                payment.getAmount(),
                method)
            .flatMap(result -> {
                if (result.success()) {
                    payment.approve(result.transactionId(), result.paymentKey());
                } else {
                    payment.fail(result.errorMessage());
                }
                return paymentRepository.save(payment);
            })
            .onErrorResume(PgClientException.class, ex -> {
                log.error("PG client error for payment {}: {}", payment.getId(), ex.getMessage());
                payment.fail("PG error: " + ex.getMessage());
                return paymentRepository.save(payment);
            })
            .onErrorResume(ex -> {
                log.error("Unexpected error during PG approval for payment {}: {}", payment.getId(), ex.getMessage(), ex);
                payment.fail("Unexpected error: " + ex.getMessage());
                return paymentRepository.save(payment);
            });
    }

    /**
     * Confirms a payment (captures the funds).
     * Calls the PG to confirm the payment, then transitions from APPROVED to CONFIRMED.
     *
     * @param paymentId the payment ID
     * @return a Mono containing the confirmed payment (or failed payment if PG confirm fails)
     * @throws PaymentNotFoundException if the payment is not found
     * @throws com.fluxpay.engine.domain.exception.InvalidPaymentStateException if payment is not in APPROVED status
     */
    public Mono<Payment> confirmPayment(PaymentId paymentId) {
        return paymentRepository.findById(paymentId)
            .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
            .flatMap(this::requestPgConfirmation)
            .doOnSuccess(payment -> {
                if (payment.getStatus() == PaymentStatus.CONFIRMED) {
                    log.info("Payment confirmed: id={}", payment.getId());
                } else {
                    log.warn("Payment confirmation failed: id={}, reason={}",
                        payment.getId(), payment.getFailureReason());
                }
            });
    }

    private Mono<Payment> requestPgConfirmation(Payment payment) {
        // Validate state before calling PG
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            return Mono.error(new com.fluxpay.engine.domain.exception.InvalidPaymentStateException(
                payment.getStatus(), PaymentStatus.CONFIRMED));
        }

        return pgClient.confirmPayment(
                payment.getPgPaymentKey(),
                payment.getOrderId().value().toString(),
                payment.getAmount())
            .flatMap(result -> {
                if (result.success()) {
                    payment.confirm();
                } else {
                    payment.fail(result.errorMessage());
                }
                return paymentRepository.save(payment);
            })
            .onErrorResume(PgClientException.class, ex -> {
                log.error("PG client error during confirmation for payment {}: {}",
                    payment.getId(), ex.getMessage());
                payment.fail("PG error: " + ex.getMessage());
                return paymentRepository.save(payment);
            })
            .onErrorResume(ex -> {
                log.error("Unexpected error during PG confirmation for payment {}: {}",
                    payment.getId(), ex.getMessage(), ex);
                payment.fail("Unexpected error: " + ex.getMessage());
                return paymentRepository.save(payment);
            });
    }

    /**
     * Fails a payment with a reason.
     *
     * @param paymentId the payment ID
     * @param reason the failure reason
     * @return a Mono containing the failed payment
     * @throws PaymentNotFoundException if the payment is not found
     * @throws com.fluxpay.engine.domain.exception.InvalidPaymentStateException if payment cannot be failed
     */
    public Mono<Payment> failPayment(PaymentId paymentId, String reason) {
        return paymentRepository.findById(paymentId)
            .switchIfEmpty(Mono.error(new PaymentNotFoundException(paymentId)))
            .flatMap(payment -> {
                payment.fail(reason);
                return paymentRepository.save(payment);
            })
            .doOnSuccess(payment -> log.warn("Payment failed: id={}, reason={}",
                payment.getId(), payment.getFailureReason()));
    }
}
