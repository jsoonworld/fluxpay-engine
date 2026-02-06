package com.fluxpay.engine.domain.saga.payment;

import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.payment.Payment;

/**
 * Result of a successful PaymentSaga execution.
 *
 * @param order the created order
 * @param payment the created payment
 */
public record PaymentSagaResult(Order order, Payment payment) {
}
