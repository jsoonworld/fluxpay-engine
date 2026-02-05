package com.fluxpay.engine.infrastructure.persistence.repository;

import com.fluxpay.engine.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Spring Data R2DBC repository for OrderEntity.
 */
@Repository
public interface OrderR2dbcRepository extends ReactiveCrudRepository<OrderEntity, UUID> {

    /**
     * Find all orders by user ID.
     *
     * @param userId the user ID
     * @return a Flux of OrderEntity for the user
     */
    Flux<OrderEntity> findByUserId(String userId);
}
