package com.fluxpay.engine.infrastructure.persistence.repository;

import com.fluxpay.engine.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Spring Data R2DBC repository interface for PaymentEntity.
 * Provides reactive CRUD operations for payments.
 */
public interface PaymentR2dbcRepository extends ReactiveCrudRepository<PaymentEntity, UUID> {

    /**
     * Find a payment by its associated order ID.
     *
     * @param orderId the order ID
     * @return the payment associated with the order, or empty if not found
     */
    Mono<PaymentEntity> findByOrderId(UUID orderId);

    /**
     * Find all payments with a specific status.
     *
     * @param status the payment status name
     * @return a Flux of payments with the given status
     */
    Flux<PaymentEntity> findByStatus(String status);

    /**
     * Find a payment by its PG payment key.
     *
     * @param pgPaymentKey the PG payment key
     * @return the payment with the given PG payment key, or empty if not found
     */
    Mono<PaymentEntity> findByPgPaymentKey(String pgPaymentKey);
}
