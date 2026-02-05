package com.fluxpay.engine.presentation.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Order Errors (ORD_xxx)
    ORDER_NOT_FOUND("ORD_001", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
    ORDER_ALREADY_PROCESSED("ORD_002", HttpStatus.CONFLICT, "이미 처리된 주문입니다"),
    INVALID_ORDER_STATE("ORD_003", HttpStatus.BAD_REQUEST, "잘못된 주문 상태 전이입니다"),

    // Payment Errors (PAY_xxx)
    PAYMENT_NOT_FOUND("PAY_001", HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다"),
    INVALID_PAYMENT_AMOUNT("PAY_002", HttpStatus.BAD_REQUEST, "잘못된 결제 금액입니다"),
    PAYMENT_ALREADY_PROCESSED("PAY_003", HttpStatus.CONFLICT, "이미 처리된 결제입니다"),
    UNSUPPORTED_PAYMENT_METHOD("PAY_004", HttpStatus.UNPROCESSABLE_ENTITY, "지원하지 않는 결제 수단입니다"),
    PG_CONNECTION_ERROR("PAY_005", HttpStatus.BAD_GATEWAY, "PG 연동 오류가 발생했습니다"),
    INVALID_PAYMENT_STATE("PAY_006", HttpStatus.BAD_REQUEST, "잘못된 결제 상태 전이입니다"),

    // Validation Errors (VAL_xxx)
    VALIDATION_FAILED("VAL_001", HttpStatus.BAD_REQUEST, "요청 검증에 실패했습니다"),
    IDEMPOTENCY_KEY_MISSING("VAL_002", HttpStatus.BAD_REQUEST, "X-Idempotency-Key 헤더가 필요합니다"),
    IDEMPOTENCY_KEY_INVALID("VAL_003", HttpStatus.BAD_REQUEST, "유효하지 않은 멱등 키 형식입니다 (UUID 필요)"),
    IDEMPOTENCY_CONFLICT("VAL_004", HttpStatus.UNPROCESSABLE_ENTITY, "동일한 멱등 키에 다른 페이로드가 사용되었습니다"),
    IDEMPOTENCY_PROCESSING("VAL_005", HttpStatus.CONFLICT, "동일한 멱등 키로 요청이 처리 중입니다. 잠시 후 다시 시도해주세요"),

    // Tenant Errors (TNT_xxx)
    TENANT_HEADER_MISSING("TNT_001", HttpStatus.BAD_REQUEST, "X-Tenant-Id 헤더가 필요합니다"),
    TENANT_NOT_FOUND("TNT_002", HttpStatus.NOT_FOUND, "알 수 없는 테넌트입니다"),

    // System Errors (SYS_xxx)
    INTERNAL_SERVER_ERROR("SYS_001", HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE("SYS_002", HttpStatus.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다"),
    GATEWAY_TIMEOUT("SYS_003", HttpStatus.GATEWAY_TIMEOUT, "외부 서비스 응답 시간 초과");

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
