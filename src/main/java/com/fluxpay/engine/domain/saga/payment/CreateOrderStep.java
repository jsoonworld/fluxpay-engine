package com.fluxpay.engine.domain.saga.payment;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaStep;
import com.fluxpay.engine.domain.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Saga step that creates an order.
 *
 * <p>This step:
 * <ul>
 *   <li>Creates an order with the given line items</li>
 *   <li>Stores the orderId in the context for subsequent steps</li>
 *   <li>Compensates by cancelling the order</li>
 * </ul>
 */
public class CreateOrderStep implements SagaStep<Order> {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderStep.class);

    public static final String STEP_NAME = "CREATE_ORDER";
    public static final int STEP_ORDER = 1;

    private final OrderService orderService;

    public CreateOrderStep(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public String getStepName() {
        return STEP_NAME;
    }

    @Override
    public int getOrder() {
        return STEP_ORDER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Order> execute(SagaContext context) {
        String userId = context.get("userId", String.class);
        List<OrderLineItem> lineItems = context.get("lineItems", List.class);
        Currency currency = context.get("currency", Currency.class);

        log.debug("Creating order: userId={}, lineItemCount={}", userId, lineItems.size());

        return orderService.createOrder(userId, lineItems, currency, Map.of())
            .doOnSuccess(order -> {
                context.put("orderId", order.getId());
                context.put("order", order);
                log.info("Order created: orderId={}", order.getId());
            });
    }

    @Override
    public Mono<Void> compensate(SagaContext context) {
        OrderId orderId = context.get("orderId", OrderId.class);

        if (orderId == null) {
            log.debug("No orderId in context, skipping compensation");
            return Mono.empty();
        }

        log.info("Compensating: cancelling order {}", orderId);

        return orderService.cancelOrder(orderId)
            .doOnSuccess(order -> log.info("Order cancelled: orderId={}", orderId))
            .then();
    }
}
