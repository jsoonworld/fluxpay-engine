package com.fluxpay.engine.domain.exception;

/**
 * Exception thrown when a payment gateway (PG) operation fails.
 */
public class PgClientException extends RuntimeException {

    private final String errorCode;

    public PgClientException(String message) {
        super(message);
        this.errorCode = null;
    }

    public PgClientException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public PgClientException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PgClientException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
