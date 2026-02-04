package com.fluxpay.engine.infrastructure.persistence.repository;

import com.fluxpay.engine.infrastructure.persistence.entity.OrderLineItemEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Spring Data R2DBC repository for OrderLineItemEntity.
 */
@Repository
public interface OrderLineItemR2dbcRepository extends ReactiveCrudRepository<OrderLineItemEntity, UUID> {

    /**
     * Find all line items for an order.
     *
     * @param orderId the order ID
     * @return a Flux of OrderLineItemEntity for the order
     */
    Flux<OrderLineItemEntity> findByOrderId(UUID orderId);

    /**
     * Delete all line items for an order.
     *
     * @param orderId the order ID
     * @return a Mono completing when deletion is done
     */
    Mono<Void> deleteByOrderId(UUID orderId);

    /**
     * Find all line items for multiple orders (batch fetch).
     * Used to avoid N+1 query problem.
     *
     * @param orderIds the collection of order IDs
     * @return a Flux of OrderLineItemEntity for the orders
     */
    Flux<OrderLineItemEntity> findByOrderIdIn(java.util.Collection<UUID> orderIds);
}
