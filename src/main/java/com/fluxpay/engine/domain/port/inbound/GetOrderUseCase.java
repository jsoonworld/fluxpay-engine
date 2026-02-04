package com.fluxpay.engine.domain.port.inbound;

import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Inbound port for retrieving orders.
 * This interface defines the use case contract for order retrieval.
 */
public interface GetOrderUseCase {

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId the order ID
     * @return a Mono containing the order
     * @throws com.fluxpay.engine.domain.exception.OrderNotFoundException if order not found
     */
    Mono<Order> getOrder(OrderId orderId);

    /**
     * Retrieves all orders for a specific user.
     *
     * @param userId the user ID
     * @return a Flux of orders belonging to the user
     */
    Flux<Order> getOrdersByUserId(String userId);
}
