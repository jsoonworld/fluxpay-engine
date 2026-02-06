package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaInstance;
import com.fluxpay.engine.domain.saga.SagaStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository port for saga persistence operations.
 */
public interface SagaRepository {

    /**
     * Creates a new saga instance.
     *
     * @param sagaType the type of saga
     * @param context the saga context
     * @return the created saga instance
     */
    Mono<SagaInstance> create(String sagaType, SagaContext context);

    /**
     * Finds a saga instance by its ID.
     *
     * @param sagaId the saga ID
     * @return the saga instance if found
     */
    Mono<SagaInstance> findBySagaId(String sagaId);

    /**
     * Finds a saga instance by correlation ID.
     *
     * @param tenantId the tenant ID
     * @param correlationId the correlation ID
     * @return the saga instance if found
     */
    Mono<SagaInstance> findByCorrelationId(String tenantId, String correlationId);

    /**
     * Updates the status of a saga instance.
     *
     * @param sagaId the saga ID
     * @param status the new status
     * @return the updated saga instance
     */
    Mono<SagaInstance> updateStatus(String sagaId, SagaStatus status);

    /**
     * Saves a saga instance.
     *
     * @param instance the saga instance to save
     * @return the saved saga instance
     */
    Mono<SagaInstance> save(SagaInstance instance);

    /**
     * Finds all saga instances with a given status.
     *
     * @param status the status to filter by
     * @return a flux of matching saga instances
     */
    Flux<SagaInstance> findByStatus(SagaStatus status);

    /**
     * Saves a saga step.
     *
     * @param sagaId the parent saga ID
     * @param stepOrder the step order
     * @param stepName the step name
     * @param status the step status
     * @return void when complete
     */
    Mono<Void> saveStep(String sagaId, int stepOrder, String stepName, String status);

    /**
     * Updates the status of a saga step.
     *
     * @param sagaId the parent saga ID
     * @param stepOrder the step order
     * @param status the new status
     * @param errorMessage optional error message
     * @return void when complete
     */
    Mono<Void> updateStepStatus(String sagaId, int stepOrder, String status, String errorMessage);
}
