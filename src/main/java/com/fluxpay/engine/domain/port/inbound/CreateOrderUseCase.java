package com.fluxpay.engine.domain.port.inbound;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Inbound port for creating orders.
 * This interface defines the use case contract for order creation.
 */
public interface CreateOrderUseCase {

    /**
     * Creates a new order.
     *
     * @param userId    the user ID placing the order
     * @param lineItems the list of order line items
     * @param currency  the currency for the order
     * @param metadata  optional metadata map
     * @return a Mono containing the created order
     */
    Mono<Order> createOrder(String userId, List<OrderLineItem> lineItems,
                            Currency currency, Map<String, Object> metadata);
}
