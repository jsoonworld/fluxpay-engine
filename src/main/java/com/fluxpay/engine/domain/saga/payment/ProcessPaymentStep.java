package com.fluxpay.engine.domain.saga.payment;

import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaStep;
import com.fluxpay.engine.domain.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Saga step that initiates payment processing.
 *
 * <p>This step:
 * <ul>
 *   <li>Initiates a payment for the order</li>
 *   <li>Stores the paymentId in the context for subsequent steps</li>
 *   <li>Compensates by cancelling the payment</li>
 * </ul>
 */
public class ProcessPaymentStep implements SagaStep<Payment> {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentStep.class);

    public static final String STEP_NAME = "PROCESS_PAYMENT";
    public static final int STEP_ORDER = 2;

    private final PaymentService paymentService;

    public ProcessPaymentStep(PaymentService paymentService) {
        this.paymentService = paymentService;
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
    public Mono<Payment> execute(SagaContext context) {
        Order order = context.get("order", Order.class);

        log.debug("Creating payment: orderId={}, amount={}", order.getId(), order.getTotalAmount());

        return paymentService.createPayment(order.getId(), order.getTotalAmount())
            .doOnSuccess(payment -> {
                context.put("paymentId", payment.getId());
                context.put("payment", payment);
                log.info("Payment created: paymentId={}", payment.getId());
            });
    }

    @Override
    public Mono<Void> compensate(SagaContext context) {
        PaymentId paymentId = context.get("paymentId", PaymentId.class);

        if (paymentId == null) {
            log.debug("No paymentId in context, skipping compensation");
            return Mono.empty();
        }

        log.info("Compensating: failing payment {}", paymentId);

        return paymentService.failPayment(paymentId, "Saga compensation")
            .then()
            .doOnSuccess(v -> log.info("Payment failed for compensation: paymentId={}", paymentId));
    }
}
