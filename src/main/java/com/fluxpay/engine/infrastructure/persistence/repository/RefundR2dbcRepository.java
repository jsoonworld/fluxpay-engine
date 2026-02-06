package com.fluxpay.engine.infrastructure.persistence.repository;

import com.fluxpay.engine.infrastructure.persistence.entity.RefundEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for RefundEntity.
 */
public interface RefundR2dbcRepository extends ReactiveCrudRepository<RefundEntity, Long> {

    /**
     * Finds a refund by its refund_id.
     */
    Mono<RefundEntity> findByRefundId(String refundId);

    /**
     * Finds all refunds for a payment.
     */
    Flux<RefundEntity> findByPaymentId(String paymentId);

    /**
     * Finds all refunds with a given status.
     */
    Flux<RefundEntity> findByStatus(String status);

    /**
     * Calculates the total refunded amount for a payment (only COMPLETED refunds).
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM refunds " +
           "WHERE payment_id = :paymentId AND status = 'COMPLETED'")
    Mono<java.math.BigDecimal> getTotalRefundedAmount(String paymentId);
}
