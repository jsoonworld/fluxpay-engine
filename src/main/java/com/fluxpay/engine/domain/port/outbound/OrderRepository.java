package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outbound port (repository interface) for Order persistence.
 * Implementations should be in the infrastructure layer.
 */
public interface OrderRepository {

    /**
     * Saves an order (creates or updates).
     *
     * @param order the order to save
     * @return a Mono emitting the saved order
     */
    Mono<Order> save(Order order);

    /**
     * Finds an order by its ID.
     *
     * @param id the order ID
     * @return a Mono emitting the order if found, empty otherwise
     */
    Mono<Order> findById(OrderId id);

    /**
     * Finds all orders for a specific user.
     *
     * @param userId the user ID
     * @return a Flux emitting orders for the user
     */
    Flux<Order> findByUserId(String userId);

    /**
     * Checks if an order exists by its ID.
     *
     * @param id the order ID
     * @return a Mono emitting true if exists, false otherwise
     */
    Mono<Boolean> existsById(OrderId id);

    /**
     * Deletes an order by its ID.
     *
     * @param id the order ID
     * @return a Mono completing when deletion is done
     */
    Mono<Void> deleteById(OrderId id);
}
