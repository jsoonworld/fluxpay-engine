package com.fluxpay.engine.presentation.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Order Errors (ORD_xxx)
    ORDER_NOT_FOUND("ORD_001", HttpStatus.NOT_FOUND, "Order not found"),
    ORDER_ALREADY_PROCESSED("ORD_002", HttpStatus.CONFLICT, "Order has already been processed"),
    INVALID_ORDER_STATE("ORD_003", HttpStatus.BAD_REQUEST, "Invalid order state transition"),

    // Payment Errors (PAY_xxx)
    PAYMENT_NOT_FOUND("PAY_001", HttpStatus.NOT_FOUND, "Payment not found"),
    INVALID_PAYMENT_AMOUNT("PAY_002", HttpStatus.BAD_REQUEST, "Invalid payment amount"),
    PAYMENT_ALREADY_PROCESSED("PAY_003", HttpStatus.CONFLICT, "Payment has already been processed"),
    UNSUPPORTED_PAYMENT_METHOD("PAY_004", HttpStatus.UNPROCESSABLE_ENTITY, "Unsupported payment method"),
    PG_CONNECTION_ERROR("PAY_005", HttpStatus.BAD_GATEWAY, "Payment gateway connection error"),
    INVALID_PAYMENT_STATE("PAY_006", HttpStatus.BAD_REQUEST, "Invalid payment state transition"),
    REFUND_AMOUNT_EXCEEDED("PAY_007", HttpStatus.BAD_REQUEST, "Refund amount exceeds remaining refundable amount"),
    REFUND_PERIOD_EXPIRED("PAY_008", HttpStatus.BAD_REQUEST, "Refund period has expired"),
    REFUND_NOT_FOUND("PAY_009", HttpStatus.NOT_FOUND, "Refund not found"),
    INVALID_REFUND_STATE("PAY_010", HttpStatus.BAD_REQUEST, "Invalid refund state transition"),

    // Validation Errors (VAL_xxx)
    VALIDATION_FAILED("VAL_001", HttpStatus.BAD_REQUEST, "Request validation failed"),
    IDEMPOTENCY_KEY_MISSING("VAL_002", HttpStatus.BAD_REQUEST, "X-Idempotency-Key header is required"),
    IDEMPOTENCY_KEY_INVALID("VAL_003", HttpStatus.BAD_REQUEST, "Invalid idempotency key format (UUID required)"),
    IDEMPOTENCY_CONFLICT("VAL_004", HttpStatus.UNPROCESSABLE_ENTITY, "Different payload used with the same idempotency key"),
    IDEMPOTENCY_PROCESSING("VAL_005", HttpStatus.CONFLICT, "Request with the same idempotency key is being processed. Please retry later"),
    INVALID_WEBHOOK_SIGNATURE("VAL_006", HttpStatus.UNAUTHORIZED, "Invalid webhook signature"),

    // Tenant Errors (TNT_xxx)
    TENANT_HEADER_MISSING("TNT_001", HttpStatus.BAD_REQUEST, "X-Tenant-Id header is required"),
    TENANT_NOT_FOUND("TNT_002", HttpStatus.NOT_FOUND, "Unknown tenant"),

    // System Errors (SYS_xxx)
    INTERNAL_SERVER_ERROR("SYS_001", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    SERVICE_UNAVAILABLE("SYS_002", HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable"),
    GATEWAY_TIMEOUT("SYS_003", HttpStatus.GATEWAY_TIMEOUT, "External service response timeout");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
