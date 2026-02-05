package com.fluxpay.engine.domain.service;

import com.fluxpay.engine.domain.exception.OrderNotFoundException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.port.inbound.CreateOrderUseCase;
import com.fluxpay.engine.domain.port.inbound.GetOrderUseCase;
import com.fluxpay.engine.domain.port.outbound.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Domain service for Order operations.
 * Implements CreateOrderUseCase and GetOrderUseCase inbound ports.
 */
public class OrderService implements CreateOrderUseCase, GetOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Mono<Order> createOrder(String userId, List<OrderLineItem> lineItems,
                                   Currency currency, Map<String, Object> metadata) {
        Order order = Order.create(userId, lineItems, currency, metadata);

        return orderRepository.save(order)
            .doOnSuccess(savedOrder -> log.info("Order created: id={}, userId={}, totalAmount={}",
                savedOrder.getId(), savedOrder.getUserId(), savedOrder.getTotalAmount()));
    }

    @Override
    public Mono<Order> getOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .doOnSuccess(order -> log.debug("Order retrieved: id={}", order.getId()));
    }

    @Override
    public Flux<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId)
            .doOnComplete(() -> log.debug("Retrieved orders for userId={}", userId));
    }

    /**
     * Marks an order as paid.
     *
     * @param orderId the order ID
     * @return a Mono containing the updated order
     * @throws OrderNotFoundException if the order is not found
     */
    public Mono<Order> markOrderAsPaid(OrderId orderId) {
        return updateOrderState(orderId, Order::markAsPaid, "marked as paid");
    }

    /**
     * Completes an order.
     *
     * @param orderId the order ID
     * @return a Mono containing the updated order
     * @throws OrderNotFoundException if the order is not found
     */
    public Mono<Order> completeOrder(OrderId orderId) {
        return updateOrderState(orderId, Order::complete, "completed");
    }

    /**
     * Cancels an order.
     *
     * @param orderId the order ID
     * @return a Mono containing the updated order
     * @throws OrderNotFoundException if the order is not found
     */
    public Mono<Order> cancelOrder(OrderId orderId) {
        return updateOrderState(orderId, Order::cancel, "cancelled");
    }

    /**
     * Marks an order as failed.
     *
     * @param orderId the order ID
     * @return a Mono containing the updated order
     * @throws OrderNotFoundException if the order is not found
     */
    public Mono<Order> failOrder(OrderId orderId) {
        return updateOrderState(orderId, Order::fail, "marked as failed");
    }

    /**
     * Common helper method for order state transitions.
     * Follows the pattern: find -> validate -> update state -> save -> log
     */
    private Mono<Order> updateOrderState(OrderId orderId, Consumer<Order> stateTransition, String action) {
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> {
                stateTransition.accept(order);
                return orderRepository.save(order);
            })
            .doOnSuccess(order -> log.info("Order {}: id={}", action, order.getId()));
    }
}
