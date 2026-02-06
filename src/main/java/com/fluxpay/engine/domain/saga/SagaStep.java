package com.fluxpay.engine.domain.saga;

import reactor.core.publisher.Mono;

/**
 * Interface representing a step in a saga execution.
 *
 * @param <T> the result type of the step execution
 */
public interface SagaStep<T> {

    /**
     * Returns the name of this step.
     */
    String getStepName();

    /**
     * Returns the order in which this step should be executed.
     */
    int getOrder();

    /**
     * Executes this step.
     *
     * @param context the saga context containing shared data
     * @return a Mono containing the result of the execution
     */
    Mono<T> execute(SagaContext context);

    /**
     * Compensates (rolls back) this step.
     *
     * @param context the saga context containing shared data
     * @return a Mono that completes when compensation is done
     */
    Mono<Void> compensate(SagaContext context);
}
