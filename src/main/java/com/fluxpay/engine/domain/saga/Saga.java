package com.fluxpay.engine.domain.saga;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Represents a Saga - a sequence of steps that form a distributed transaction.
 *
 * <p>A Saga is composed of multiple {@link SagaStep}s that are executed in order.
 * If any step fails, compensation is performed in reverse order for all
 * previously executed steps.
 *
 * <p>Implementations should define:
 * <ul>
 *   <li>{@link #getSagaType()} - A unique identifier for the saga type</li>
 *   <li>{@link #getSteps()} - The list of steps to execute</li>
 *   <li>{@link #onComplete(SagaContext)} - Called when all steps complete successfully</li>
 * </ul>
 *
 * @param <T> the type of result produced by this saga
 */
public interface Saga<T> {

    /**
     * Returns the type identifier for this saga.
     *
     * @return the saga type
     */
    String getSagaType();

    /**
     * Returns the list of steps in this saga.
     * Steps will be executed in order of their {@link SagaStep#getOrder()}.
     *
     * @return the list of steps
     */
    List<SagaStep<?>> getSteps();

    /**
     * Called when all steps complete successfully.
     * This method should produce the final result of the saga.
     *
     * @param context the saga context containing data from all steps
     * @return a Mono completing with the saga result
     */
    Mono<T> onComplete(SagaContext context);
}
