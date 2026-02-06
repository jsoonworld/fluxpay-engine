package com.fluxpay.engine.infrastructure.saga;

/**
 * Exception thrown when saga execution fails.
 */
public class SagaExecutionException extends RuntimeException {

    private final String sagaId;
    private final String sagaType;
    private final String failedStep;
    private final boolean compensationFailed;

    public SagaExecutionException(String message, String sagaId, String sagaType,
                                   String failedStep, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.failedStep = failedStep;
        this.compensationFailed = false;
    }

    public SagaExecutionException(String message, String sagaId, String sagaType,
                                   String failedStep, boolean compensationFailed, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.failedStep = failedStep;
        this.compensationFailed = compensationFailed;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public String getFailedStep() {
        return failedStep;
    }

    public boolean isCompensationFailed() {
        return compensationFailed;
    }
}
