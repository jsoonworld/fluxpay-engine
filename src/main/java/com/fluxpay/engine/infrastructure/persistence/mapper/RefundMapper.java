package com.fluxpay.engine.infrastructure.persistence.mapper;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import com.fluxpay.engine.domain.model.refund.RefundId;
import com.fluxpay.engine.domain.model.refund.RefundStatus;
import com.fluxpay.engine.infrastructure.persistence.entity.RefundEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Refund domain objects and persistence entities.
 */
@Component
public class RefundMapper {

    /**
     * Converts a RefundEntity to a Refund domain object.
     *
     * @param entity the RefundEntity to convert
     * @return the Refund domain object, or null if entity is null
     */
    public Refund toDomain(RefundEntity entity) {
        if (entity == null) {
            return null;
        }

        return Refund.restore(
            RefundId.of(entity.getRefundId()),
            PaymentId.of(entity.getPaymentId()),
            Money.of(entity.getAmount(), Currency.valueOf(entity.getCurrency())),
            RefundStatus.valueOf(entity.getStatus()),
            entity.getReason(),
            entity.getPgRefundId(),
            entity.getErrorMessage(),
            entity.getRequestedAt(),
            entity.getCompletedAt()
        );
    }

    /**
     * Converts a Refund domain object to a RefundEntity.
     *
     * @param refund the Refund domain object to convert
     * @param tenantId the tenant ID to set on the entity
     * @return the RefundEntity with tenantId set
     * @throws IllegalArgumentException if refund is null
     */
    public RefundEntity toEntity(Refund refund, String tenantId) {
        if (refund == null) {
            throw new IllegalArgumentException("Refund cannot be null");
        }

        RefundEntity entity = new RefundEntity();
        entity.setRefundId(refund.getId().value());
        entity.setTenantId(tenantId);
        entity.setPaymentId(refund.getPaymentId().value().toString());
        entity.setAmount(refund.getAmount().amount());
        entity.setCurrency(refund.getAmount().currency().name());
        entity.setReason(refund.getReason());
        entity.setStatus(refund.getStatus().name());
        entity.setPgRefundId(refund.getPgRefundId());
        entity.setRequestedAt(refund.getRequestedAt());
        entity.setCompletedAt(refund.getCompletedAt());
        entity.setErrorMessage(refund.getErrorMessage());

        return entity;
    }
}
