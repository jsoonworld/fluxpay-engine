package com.fluxpay.engine.infrastructure.persistence.adapter;

import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.domain.port.outbound.PaymentRepository;
import com.fluxpay.engine.infrastructure.persistence.entity.PaymentEntity;
import com.fluxpay.engine.infrastructure.persistence.mapper.PaymentMapper;
import com.fluxpay.engine.infrastructure.persistence.repository.PaymentR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC implementation of PaymentRepository.
 * Adapts the domain repository interface to Spring Data R2DBC.
 */
@Repository
public class R2dbcPaymentRepository implements PaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(R2dbcPaymentRepository.class);

    private final PaymentR2dbcRepository r2dbcRepository;
    private final PaymentMapper mapper;

    public R2dbcPaymentRepository(PaymentR2dbcRepository r2dbcRepository, PaymentMapper mapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Payment> save(Payment payment) {
        log.debug("Saving payment: id={}", payment.getId());

        return r2dbcRepository.existsById(payment.getId().value())
                .flatMap(exists -> {
                    PaymentEntity entity = mapper.toEntity(payment);
                    if (exists) {
                        entity.markAsExisting();
                    }
                    return r2dbcRepository.save(entity);
                })
                .map(mapper::toDomain)
                .doOnSuccess(savedPayment -> log.debug("Payment saved: id={}", savedPayment.getId()))
                .doOnError(error -> log.error("Failed to save payment: id={}", payment.getId(), error));
    }

    @Override
    public Mono<Payment> findById(PaymentId id) {
        log.debug("Finding payment by id: {}", id);

        return r2dbcRepository.findById(id.value())
                .map(mapper::toDomain)
                .doOnSuccess(payment -> {
                    if (payment != null) {
                        log.debug("Payment found: id={}", id);
                    } else {
                        log.debug("Payment not found: id={}", id);
                    }
                });
    }

    @Override
    public Mono<Payment> findByOrderId(OrderId orderId) {
        log.debug("Finding payment by orderId: {}", orderId);

        return r2dbcRepository.findByOrderId(orderId.value())
                .map(mapper::toDomain)
                .doOnSuccess(payment -> {
                    if (payment != null) {
                        log.debug("Payment found for orderId: {}", orderId);
                    } else {
                        log.debug("Payment not found for orderId: {}", orderId);
                    }
                });
    }

    @Override
    public Flux<Payment> findByStatus(PaymentStatus status) {
        log.debug("Finding payments by status: {}", status);

        return r2dbcRepository.findByStatus(status.name())
                .map(mapper::toDomain)
                .doOnComplete(() -> log.debug("Completed finding payments for status: {}", status));
    }

    @Override
    public Mono<Payment> findByPgPaymentKey(String pgPaymentKey) {
        log.debug("Finding payment by pgPaymentKey: {}", pgPaymentKey);

        return r2dbcRepository.findByPgPaymentKey(pgPaymentKey)
                .map(mapper::toDomain)
                .doOnSuccess(payment -> {
                    if (payment != null) {
                        log.debug("Payment found for pgPaymentKey: {}", pgPaymentKey);
                    } else {
                        log.debug("Payment not found for pgPaymentKey: {}", pgPaymentKey);
                    }
                });
    }

    @Override
    public Mono<Boolean> existsById(PaymentId id) {
        log.debug("Checking if payment exists: id={}", id);

        return r2dbcRepository.existsById(id.value())
                .doOnSuccess(exists -> log.debug("Payment exists check: id={}, exists={}", id, exists));
    }
}
