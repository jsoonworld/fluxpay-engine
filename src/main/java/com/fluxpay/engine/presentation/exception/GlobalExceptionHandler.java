package com.fluxpay.engine.presentation.exception;

import com.fluxpay.engine.domain.exception.InvalidOrderStateException;
import com.fluxpay.engine.domain.exception.InvalidPaymentStateException;
import com.fluxpay.engine.domain.exception.OrderNotFoundException;
import com.fluxpay.engine.domain.exception.PaymentNotFoundException;
import com.fluxpay.engine.domain.exception.PaymentProcessingException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyConflictException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyKeyInvalidException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyKeyMissingException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyProcessingException;
import com.fluxpay.engine.infrastructure.tenant.TenantNotFoundException;
import com.fluxpay.engine.presentation.dto.ApiResponse;
import com.fluxpay.engine.presentation.dto.FieldError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidationError(WebExchangeBindException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
            .toList();

        log.warn("Validation error: {}", fieldErrors);

        return Mono.just(ResponseEntity
            .badRequest()
            .body(ApiResponse.validationError(
                ErrorCode.VALIDATION_FAILED.getCode(),
                ErrorCode.VALIDATION_FAILED.getMessage(),
                fieldErrors)));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleOrderNotFoundException(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.ORDER_NOT_FOUND);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        log.warn("Payment not found: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleInvalidOrderStateException(InvalidOrderStateException ex) {
        log.warn("Invalid order state transition: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.INVALID_ORDER_STATE);
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleInvalidPaymentStateException(InvalidPaymentStateException ex) {
        log.warn("Invalid payment state transition: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.INVALID_PAYMENT_STATE);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handlePaymentProcessingException(PaymentProcessingException ex) {
        log.error("Payment processing error: {}", ex.getMessage(), ex);
        return createErrorResponse(ErrorCode.PG_CONNECTION_ERROR);
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleTenantNotFoundException(TenantNotFoundException ex) {
        log.warn("Tenant not found: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.TENANT_HEADER_MISSING);
    }

    @ExceptionHandler(IdempotencyKeyMissingException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIdempotencyKeyMissing(IdempotencyKeyMissingException ex) {
        log.warn("Idempotency key missing: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.IDEMPOTENCY_KEY_MISSING);
    }

    @ExceptionHandler(IdempotencyKeyInvalidException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIdempotencyKeyInvalid(IdempotencyKeyInvalidException ex) {
        log.warn("Idempotency key invalid: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.IDEMPOTENCY_KEY_INVALID);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict for key {}: {}", ex.getIdempotencyKey(), ex.getMessage());
        return createErrorResponse(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @ExceptionHandler(IdempotencyProcessingException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIdempotencyProcessing(IdempotencyProcessingException ex) {
        log.warn("Idempotency processing for key {}: {}", ex.getIdempotencyKey(), ex.getMessage());
        return createErrorResponse(ErrorCode.IDEMPOTENCY_PROCESSING);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private Mono<ResponseEntity<ApiResponse<Void>>> createErrorResponse(ErrorCode errorCode) {
        return Mono.just(ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage())));
    }
}
