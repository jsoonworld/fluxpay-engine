package com.fluxpay.engine.domain.saga;

import java.util.*;

/**
 * Context object that holds state during saga execution.
 * Tracks executed steps, stores data, and captures failure information.
 */
public class SagaContext {

    private final String sagaId;
    private final String correlationId;
    private final String tenantId;
    private final Map<String, Object> data;
    private final List<String> executedSteps;
    private int currentStepIndex;
    private String failedStep;
    private String failureReason;

    private SagaContext(String sagaId, String correlationId, String tenantId) {
        this.sagaId = Objects.requireNonNull(sagaId, "sagaId is required");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId is required");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.data = new HashMap<>();
        this.executedSteps = new ArrayList<>();
        this.currentStepIndex = 0;
    }

    /**
     * Creates a new SagaContext.
     */
    public static SagaContext create(String sagaId, String correlationId, String tenantId) {
        return new SagaContext(sagaId, correlationId, tenantId);
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    /**
     * Stores a value in the context.
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Retrieves a value from the context.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /**
     * Checks if a key exists in the context.
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    /**
     * Returns all data as an unmodifiable map.
     */
    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Marks a step as executed.
     */
    public void markStepExecuted(String stepName) {
        executedSteps.add(stepName);
    }

    /**
     * Checks if a step has been executed.
     */
    public boolean isStepExecuted(String stepName) {
        return executedSteps.contains(stepName);
    }

    /**
     * Returns the list of executed steps in order.
     */
    public List<String> getExecutedSteps() {
        return Collections.unmodifiableList(executedSteps);
    }

    /**
     * Gets the current step index.
     */
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    /**
     * Increments the current step index.
     */
    public void incrementStepIndex() {
        currentStepIndex++;
    }

    /**
     * Sets the failed step name.
     */
    public void setFailedStep(String failedStep) {
        this.failedStep = failedStep;
    }

    /**
     * Gets the failed step name.
     */
    public String getFailedStep() {
        return failedStep;
    }

    /**
     * Sets the failure reason.
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Gets the failure reason.
     */
    public String getFailureReason() {
        return failureReason;
    }
}
