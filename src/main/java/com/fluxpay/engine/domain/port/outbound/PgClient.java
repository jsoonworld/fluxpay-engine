package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import reactor.core.publisher.Mono;

public interface PgClient {

    Mono<PgApprovalResult> requestApproval(String orderId, Money amount, PaymentMethod method);

    Mono<PgConfirmResult> confirmPayment(String paymentKey, String orderId, Money amount);

    Mono<PgCancelResult> cancelPayment(String paymentKey, String reason);

    record PgApprovalResult(String transactionId, String paymentKey, boolean success, String errorMessage) {}
    record PgConfirmResult(String transactionId, boolean success, String errorMessage) {}
    record PgCancelResult(String transactionId, boolean success, String errorMessage) {}
}
