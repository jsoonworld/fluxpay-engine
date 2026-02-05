package com.fluxpay.engine.infrastructure.persistence.mapper;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentMethodType;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Payment domain objects and persistence entities.
 */
@Component
public class PaymentMapper {

    /**
     * Converts a PaymentEntity to a Payment domain object.
     *
     * @param entity the PaymentEntity to convert
     * @return the Payment domain object, or null if entity is null
     */
    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        PaymentMethod paymentMethod = null;
        if (entity.getPaymentMethodType() != null) {
            paymentMethod = new PaymentMethod(
                    PaymentMethodType.valueOf(entity.getPaymentMethodType()),
                    entity.getPaymentMethodDisplayName()
            );
        }

        return Payment.restore(
                PaymentId.of(entity.getId()),
                OrderId.of(entity.getOrderId()),
                Money.of(entity.getAmount(), Currency.valueOf(entity.getCurrency())),
                PaymentStatus.valueOf(entity.getStatus()),
                paymentMethod,
                entity.getPgTransactionId(),
                entity.getPgPaymentKey(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getApprovedAt(),
                entity.getConfirmedAt(),
                entity.getFailedAt()
        );
    }

    /**
     * Converts a Payment domain object to a PaymentEntity.
     *
     * @param payment the Payment domain object to convert
     * @return the PaymentEntity
     * @throws IllegalArgumentException if payment is null
     */
    public PaymentEntity toEntity(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId().value());
        entity.setOrderId(payment.getOrderId().value());
        entity.setAmount(payment.getAmount().amount());
        entity.setCurrency(payment.getAmount().currency().name());
        entity.setStatus(payment.getStatus().name());

        if (payment.getPaymentMethod() != null) {
            entity.setPaymentMethodType(payment.getPaymentMethod().type().name());
            entity.setPaymentMethodDisplayName(payment.getPaymentMethod().displayName());
        }

        entity.setPgTransactionId(payment.getPgTransactionId());
        entity.setPgPaymentKey(payment.getPgPaymentKey());
        entity.setFailureReason(payment.getFailureReason());
        entity.setApprovedAt(payment.getApprovedAt());
        entity.setConfirmedAt(payment.getConfirmedAt());
        entity.setFailedAt(payment.getFailedAt());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setUpdatedAt(payment.getUpdatedAt());

        return entity;
    }
}
