package com.fluxpay.engine.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.RefundId;
import com.fluxpay.engine.domain.service.RefundService;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyService;
import com.fluxpay.engine.infrastructure.tenant.TenantContext;
import com.fluxpay.engine.presentation.dto.ApiResponse;
import com.fluxpay.engine.presentation.dto.request.CreateRefundRequest;
import com.fluxpay.engine.presentation.dto.response.RefundResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * REST controller for Refund operations.
 * Provides endpoints for creating and retrieving refunds.
 */
@RestController
@RequestMapping("/api/v1")
public class RefundController {

    private static final Logger log = LoggerFactory.getLogger(RefundController.class);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final RefundService refundService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public RefundController(RefundService refundService,
                            IdempotencyService idempotencyService,
                            ObjectMapper objectMapper) {
        this.refundService = refundService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/refunds")
    public Mono<ResponseEntity<byte[]>> createRefund(
        @RequestHeader("X-Idempotency-Key") String idempotencyKeyStr,
        @Valid @RequestBody CreateRefundRequest request
    ) {
        log.info("Creating refund for payment: {}", request.paymentId());

        return TenantContext.getTenantId()
            .flatMap(tenantId -> {
                IdempotencyKey key = new IdempotencyKey(tenantId, "POST /api/v1/refunds", idempotencyKeyStr);
                String payloadHash = computePayloadHash(request);

                return idempotencyService.acquireLock(key, payloadHash, IDEMPOTENCY_TTL)
                    .flatMap(result -> {
                        if (result.isHit()) {
                            return Mono.just(toJsonResponse(result.cachedHttpStatus(), result.cachedResponse()));
                        }

                        return processCreateRefund(request)
                            .flatMap(response -> {
                                String jsonResponse = serialize(response);
                                return idempotencyService.store(key, payloadHash, jsonResponse, 201, IDEMPOTENCY_TTL)
                                    .thenReturn(toJsonResponse(HttpStatus.CREATED.value(), jsonResponse));
                            })
                            .onErrorResume(ex ->
                                idempotencyService.releaseLock(key).then(Mono.error(ex)));
                    });
            });
    }

    @GetMapping("/refunds/{refundId}")
    public Mono<ApiResponse<RefundResponse>> getRefund(@PathVariable String refundId) {
        log.info("Getting refund: {}", refundId);

        return refundService.getRefund(RefundId.of(refundId))
            .map(refund -> ApiResponse.success(RefundResponse.from(refund)));
    }

    @GetMapping("/payments/{paymentId}/refunds")
    public Mono<ApiResponse<List<RefundResponse>>> getRefundsByPayment(@PathVariable String paymentId) {
        log.info("Getting refunds for payment: {}", paymentId);

        return refundService.getRefundsByPayment(PaymentId.of(paymentId))
            .map(RefundResponse::from)
            .collectList()
            .map(ApiResponse::success);
    }

    private Mono<ApiResponse<RefundResponse>> processCreateRefund(CreateRefundRequest request) {
        PaymentId paymentId = PaymentId.of(request.paymentId());
        Currency currency = Currency.valueOf(request.currency());
        Money amount = Money.of(request.amount(), currency);

        return refundService.createRefund(paymentId, amount, request.reason())
            .map(refund -> ApiResponse.success(RefundResponse.from(refund)));
    }

    private ResponseEntity<byte[]> toJsonResponse(int httpStatus, String jsonBody) {
        return ResponseEntity.status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonBody.getBytes(StandardCharsets.UTF_8));
    }

    private String computePayloadHash(CreateRefundRequest request) {
        try {
            return String.valueOf(objectMapper.writeValueAsString(request).hashCode());
        } catch (JsonProcessingException e) {
            return String.valueOf(request.hashCode());
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response", e);
        }
    }
}
