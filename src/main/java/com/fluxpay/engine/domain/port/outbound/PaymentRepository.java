package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentRepository {

    Mono<Payment> save(Payment payment);

    Mono<Payment> findById(PaymentId id);

    Mono<Payment> findByOrderId(OrderId orderId);

    Flux<Payment> findByStatus(PaymentStatus status);

    Mono<Payment> findByPgPaymentKey(String pgPaymentKey);

    Mono<Boolean> existsById(PaymentId id);
}
