package com.fluxpay.engine.domain.saga;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing a saga instance.
 *
 * <p>Tracks the state and metadata of a saga execution.
 */
public class SagaInstance {

    private final String sagaId;
    private final String sagaType;
    private final String correlationId;
    private final String tenantId;
    private SagaStatus status;
    private int currentStep;
    private String contextData;
    private String errorMessage;
    private final Instant startedAt;
    private Instant completedAt;
    private Instant updatedAt;

    private SagaInstance(String sagaId, String sagaType, String correlationId, String tenantId,
                         SagaStatus status, int currentStep, String contextData, String errorMessage,
                         Instant startedAt, Instant completedAt, Instant updatedAt) {
        this.sagaId = Objects.requireNonNull(sagaId, "sagaId is required");
        this.sagaType = Objects.requireNonNull(sagaType, "sagaType is required");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId is required");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.currentStep = currentStep;
        this.contextData = contextData;
        this.errorMessage = errorMessage;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt is required");
        this.completedAt = completedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    /**
     * Creates a new saga instance with STARTED status.
     */
    public static SagaInstance create(String sagaId, String sagaType, String correlationId, String tenantId) {
        Instant now = Instant.now();
        return new SagaInstance(sagaId, sagaType, correlationId, tenantId,
            SagaStatus.STARTED, 0, null, null, now, null, now);
    }

    /**
     * Restores a saga instance from persistence.
     */
    public static SagaInstance restore(String sagaId, String sagaType, String correlationId, String tenantId,
                                       SagaStatus status, int currentStep, String contextData, String errorMessage,
                                       Instant startedAt, Instant completedAt, Instant updatedAt) {
        return new SagaInstance(sagaId, sagaType, correlationId, tenantId,
            status, currentStep, contextData, errorMessage, startedAt, completedAt, updatedAt);
    }

    /**
     * Updates the status of this saga instance.
     */
    public void updateStatus(SagaStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this.status, newStatus));
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
        if (newStatus.isTerminal()) {
            this.completedAt = Instant.now();
        }
    }

    /**
     * Marks the saga as processing and sets the current step.
     */
    public void startProcessing(int stepIndex) {
        if (this.status == SagaStatus.STARTED) {
            this.status = SagaStatus.PROCESSING;
        }
        this.currentStep = stepIndex;
        this.updatedAt = Instant.now();
    }

    /**
     * Sets the error message for a failed saga.
     */
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    /**
     * Sets the serialized context data.
     */
    public void setContextData(String contextData) {
        this.contextData = contextData;
        this.updatedAt = Instant.now();
    }

    // Getters

    public String getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public String getContextData() {
        return contextData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SagaInstance that = (SagaInstance) o;
        return Objects.equals(sagaId, that.sagaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sagaId);
    }

    @Override
    public String toString() {
        return "SagaInstance{" +
               "sagaId='" + sagaId + '\'' +
               ", sagaType='" + sagaType + '\'' +
               ", status=" + status +
               ", currentStep=" + currentStep +
               '}';
    }
}
