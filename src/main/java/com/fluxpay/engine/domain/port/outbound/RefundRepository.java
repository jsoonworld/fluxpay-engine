package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import com.fluxpay.engine.domain.model.refund.RefundId;
import com.fluxpay.engine.domain.model.refund.RefundStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository port for refund persistence operations.
 */
public interface RefundRepository {

    /**
     * Saves a refund.
     *
     * @param refund the refund to save
     * @return the saved refund
     */
    Mono<Refund> save(Refund refund);

    /**
     * Finds a refund by its ID.
     *
     * @param id the refund ID
     * @return the refund if found, empty otherwise
     */
    Mono<Refund> findById(RefundId id);

    /**
     * Finds all refunds for a payment.
     *
     * @param paymentId the payment ID
     * @return a flux of refunds for the payment
     */
    Flux<Refund> findByPaymentId(PaymentId paymentId);

    /**
     * Finds all refunds with a given status.
     *
     * @param status the status to filter by
     * @return a flux of matching refunds
     */
    Flux<Refund> findByStatus(RefundStatus status);

    /**
     * Gets the total refunded amount for a payment.
     * Only counts COMPLETED refunds.
     *
     * @param paymentId the payment ID
     * @return the total refunded amount
     */
    Mono<Money> getTotalRefundedAmount(PaymentId paymentId);
}
