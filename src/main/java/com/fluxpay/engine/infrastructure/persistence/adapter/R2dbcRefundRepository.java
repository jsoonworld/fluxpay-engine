package com.fluxpay.engine.infrastructure.persistence.adapter;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import com.fluxpay.engine.domain.model.refund.RefundId;
import com.fluxpay.engine.domain.model.refund.RefundStatus;
import com.fluxpay.engine.domain.port.outbound.RefundRepository;
import com.fluxpay.engine.infrastructure.persistence.entity.RefundEntity;
import com.fluxpay.engine.infrastructure.persistence.mapper.RefundMapper;
import com.fluxpay.engine.infrastructure.persistence.repository.RefundR2dbcRepository;
import com.fluxpay.engine.infrastructure.tenant.TenantContext;
import com.fluxpay.engine.infrastructure.tenant.TenantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * R2DBC implementation of RefundRepository.
 * Adapts the domain repository interface to Spring Data R2DBC.
 */
@Repository
public class R2dbcRefundRepository implements RefundRepository {

    private static final Logger log = LoggerFactory.getLogger(R2dbcRefundRepository.class);

    private final RefundR2dbcRepository r2dbcRepository;
    private final RefundMapper mapper;

    public R2dbcRefundRepository(RefundR2dbcRepository r2dbcRepository, RefundMapper mapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Refund> save(Refund refund) {
        log.debug("Saving refund: id={}", refund.getId());

        return TenantContext.getTenantIdOrEmpty()
            .switchIfEmpty(Mono.error(new TenantNotFoundException("Tenant ID is required for saving refunds")))
            .flatMap(tenantId -> {
                RefundEntity entity = mapper.toEntity(refund, tenantId);
                return r2dbcRepository.save(entity);
            })
            .map(mapper::toDomain)
            .doOnSuccess(saved -> log.debug("Refund saved: id={}", saved.getId()))
            .doOnError(error -> log.error("Failed to save refund: id={}", refund.getId(), error));
    }

    @Override
    public Mono<Refund> findById(RefundId id) {
        log.debug("Finding refund by id: {}", id);

        return r2dbcRepository.findByRefundId(id.value())
            .map(mapper::toDomain)
            .doOnSuccess(refund -> {
                if (refund != null) {
                    log.debug("Refund found: id={}", id);
                } else {
                    log.debug("Refund not found: id={}", id);
                }
            })
            .doOnError(error -> log.error("Failed to find refund by id: {}", id, error));
    }

    @Override
    public Flux<Refund> findByPaymentId(PaymentId paymentId) {
        log.debug("Finding refunds by paymentId: {}", paymentId);

        return r2dbcRepository.findByPaymentId(paymentId.value().toString())
            .map(mapper::toDomain)
            .doOnComplete(() -> log.debug("Completed finding refunds for paymentId: {}", paymentId))
            .doOnError(error -> log.error("Failed to find refunds by paymentId: {}", paymentId, error));
    }

    @Override
    public Flux<Refund> findByStatus(RefundStatus status) {
        log.debug("Finding refunds by status: {}", status);

        return r2dbcRepository.findByStatus(status.name())
            .map(mapper::toDomain)
            .doOnComplete(() -> log.debug("Completed finding refunds for status: {}", status))
            .doOnError(error -> log.error("Failed to find refunds by status: {}", status, error));
    }

    @Override
    public Mono<Money> getTotalRefundedAmount(PaymentId paymentId) {
        log.debug("Getting total refunded amount for paymentId: {}", paymentId);

        return r2dbcRepository.getTotalRefundedAmount(paymentId.value().toString())
            .map(total -> Money.krw(total.longValue()))
            .defaultIfEmpty(Money.krw(0))
            .doOnSuccess(total -> log.debug("Total refunded amount for paymentId {}: {}",
                paymentId, total))
            .doOnError(error -> log.error("Failed to get total refunded amount for paymentId: {}",
                paymentId, error));
    }
}
