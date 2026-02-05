package com.fluxpay.engine.infrastructure.external.pg;

public class TossPaymentsException extends RuntimeException {

    private final String errorCode;

    public TossPaymentsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TossPaymentsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
