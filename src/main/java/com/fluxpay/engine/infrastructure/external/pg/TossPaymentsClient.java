package com.fluxpay.engine.infrastructure.external.pg;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.port.outbound.PgClient;
import com.fluxpay.engine.infrastructure.external.pg.dto.TossCancelRequest;
import com.fluxpay.engine.infrastructure.external.pg.dto.TossCancelResponse;
import com.fluxpay.engine.infrastructure.external.pg.dto.TossConfirmRequest;
import com.fluxpay.engine.infrastructure.external.pg.dto.TossErrorResponse;
import com.fluxpay.engine.infrastructure.external.pg.dto.TossPaymentResponse;
import io.netty.channel.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/**
 * TossPayments PG client implementation.
 * Implements the PgClient interface for integration with TossPayments API.
 */
public class TossPaymentsClient implements PgClient {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsClient.class);

    private final WebClient webClient;

    public TossPaymentsClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<PgApprovalResult> requestApproval(String orderId, Money amount, PaymentMethod method) {
        // For Phase 1, return mock result (real flow requires client-side payment widget)
        log.info("Requesting payment approval for order: {}, amount: {}", orderId, amount.amount());
        String paymentKey = "toss_pk_" + orderId;
        String transactionId = "toss_tx_" + System.currentTimeMillis();
        return Mono.just(new PgApprovalResult(transactionId, paymentKey, true, null));
    }

    @Override
    public Mono<PgConfirmResult> confirmPayment(String paymentKey, String orderId, Money amount) {
        log.info("Confirming payment - paymentKey: {}, orderId: {}, amount: {}", paymentKey, orderId, amount.amount());

        TossConfirmRequest request = TossConfirmRequest.of(paymentKey, orderId, amount.amount());

        return webClient.post()
            .uri("/payments/confirm")
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response ->
                response.bodyToMono(TossErrorResponse.class)
                    .flatMap(error -> Mono.error(
                        new TossPaymentsException(error.code(), error.toErrorMessage())
                    ))
            )
            .bodyToMono(TossPaymentResponse.class)
            .map(response -> {
                if (response.isApproved()) {
                    log.info("Payment confirmed successfully - paymentKey: {}", response.paymentKey());
                    return new PgConfirmResult(response.transactionKey(), true, null);
                } else {
                    log.warn("Payment not in DONE status: {}", response.status());
                    return new PgConfirmResult(null, false, "Payment status: " + response.status());
                }
            })
            .onErrorResume(TossPaymentsException.class, e -> {
                log.error("Toss Payments error: {}", e.getMessage());
                return Mono.just(new PgConfirmResult(null, false, e.getMessage()));
            })
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("HTTP error during payment confirmation: {} {}", e.getStatusCode(), e.getStatusText(), e);
                return Mono.just(new PgConfirmResult(null, false, "HTTP error: " + e.getStatusCode() + " " + e.getStatusText()));
            })
            .onErrorResume(ConnectTimeoutException.class, e -> {
                log.error("Connection timeout during payment confirmation", e);
                return Mono.just(new PgConfirmResult(null, false, "Connection timeout"));
            })
            .onErrorResume(TimeoutException.class, e -> {
                log.error("Read timeout during payment confirmation", e);
                return Mono.just(new PgConfirmResult(null, false, "Read timeout"));
            })
            .onErrorResume(WebClientRequestException.class, e -> {
                if (e.getCause() instanceof ConnectException) {
                    log.error("Connection error during payment confirmation", e);
                    return Mono.just(new PgConfirmResult(null, false, "Connection error: " + e.getCause().getMessage()));
                }
                log.error("Request error during payment confirmation", e);
                return Mono.just(new PgConfirmResult(null, false, "Request error: " + e.getMessage()));
            })
            .onErrorResume(e -> {
                log.error("Unexpected error during payment confirmation", e);
                return Mono.just(new PgConfirmResult(null, false, "Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
            });
    }

    @Override
    public Mono<PgCancelResult> cancelPayment(String paymentKey, String reason) {
        log.info("Cancelling payment - paymentKey: {}, reason: {}", paymentKey, reason);

        TossCancelRequest request = TossCancelRequest.of(reason);

        return webClient.post()
            .uri("/payments/{paymentKey}/cancel", paymentKey)
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response ->
                response.bodyToMono(TossErrorResponse.class)
                    .flatMap(error -> Mono.error(
                        new TossPaymentsException(error.code(), error.toErrorMessage())
                    ))
            )
            .bodyToMono(TossCancelResponse.class)
            .map(response -> {
                if (response.isCancelled()) {
                    log.info("Payment cancelled successfully - paymentKey: {}", paymentKey);
                    String txKey = response.cancels() != null && !response.cancels().isEmpty()
                        ? response.cancels().get(0).transactionKey()
                        : null;
                    return new PgCancelResult(txKey, true, null);
                } else {
                    log.warn("Payment not cancelled: {}", response.status());
                    return new PgCancelResult(null, false, "Payment status: " + response.status());
                }
            })
            .onErrorResume(TossPaymentsException.class, e -> {
                log.error("Toss Payments cancel error: {}", e.getMessage());
                return Mono.just(new PgCancelResult(null, false, e.getMessage()));
            })
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("HTTP error during payment cancellation: {} {}", e.getStatusCode(), e.getStatusText(), e);
                return Mono.just(new PgCancelResult(null, false, "HTTP error: " + e.getStatusCode() + " " + e.getStatusText()));
            })
            .onErrorResume(ConnectTimeoutException.class, e -> {
                log.error("Connection timeout during payment cancellation", e);
                return Mono.just(new PgCancelResult(null, false, "Connection timeout"));
            })
            .onErrorResume(TimeoutException.class, e -> {
                log.error("Read timeout during payment cancellation", e);
                return Mono.just(new PgCancelResult(null, false, "Read timeout"));
            })
            .onErrorResume(WebClientRequestException.class, e -> {
                if (e.getCause() instanceof ConnectException) {
                    log.error("Connection error during payment cancellation", e);
                    return Mono.just(new PgCancelResult(null, false, "Connection error: " + e.getCause().getMessage()));
                }
                log.error("Request error during payment cancellation", e);
                return Mono.just(new PgCancelResult(null, false, "Request error: " + e.getMessage()));
            })
            .onErrorResume(e -> {
                log.error("Unexpected error during payment cancellation", e);
                return Mono.just(new PgCancelResult(null, false, "Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
            });
    }
}
