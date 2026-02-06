package com.fluxpay.engine.domain.saga.payment;

import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.saga.Saga;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaStep;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Saga for processing a payment order.
 *
 * <p>This saga orchestrates the following steps:
 * <ol>
 *   <li>Create Order - Creates an order with line items</li>
 *   <li>Process Payment - Creates and initiates the payment</li>
 * </ol>
 *
 * <p>On failure, compensation is performed in reverse order:
 * <ol>
 *   <li>Fail the payment (if created)</li>
 *   <li>Cancel the order (if created)</li>
 * </ol>
 */
public class PaymentSaga implements Saga<PaymentSagaResult> {

    public static final String SAGA_TYPE = "PAYMENT_SAGA";

    private final CreateOrderStep createOrderStep;
    private final ProcessPaymentStep processPaymentStep;

    public PaymentSaga(CreateOrderStep createOrderStep, ProcessPaymentStep processPaymentStep) {
        this.createOrderStep = createOrderStep;
        this.processPaymentStep = processPaymentStep;
    }

    @Override
    public String getSagaType() {
        return SAGA_TYPE;
    }

    @Override
    public List<SagaStep<?>> getSteps() {
        return List.of(createOrderStep, processPaymentStep);
    }

    @Override
    public Mono<PaymentSagaResult> onComplete(SagaContext context) {
        Order order = context.get("order", Order.class);
        Payment payment = context.get("payment", Payment.class);

        return Mono.just(new PaymentSagaResult(order, payment));
    }
}
