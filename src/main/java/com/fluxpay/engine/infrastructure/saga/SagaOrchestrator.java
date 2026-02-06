package com.fluxpay.engine.infrastructure.saga;

import com.fluxpay.engine.domain.port.outbound.SagaRepository;
import com.fluxpay.engine.domain.saga.Saga;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaInstance;
import com.fluxpay.engine.domain.saga.SagaStatus;
import com.fluxpay.engine.domain.saga.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates saga execution with compensation on failure.
 *
 * <p>The orchestrator:
 * <ul>
 *   <li>Executes saga steps in order</li>
 *   <li>Tracks step execution in the context</li>
 *   <li>On failure, compensates in reverse order</li>
 *   <li>Persists saga state for recovery</li>
 * </ul>
 */
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final SagaRepository sagaRepository;

    public SagaOrchestrator(SagaRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    /**
     * Executes a saga with the given context.
     *
     * @param saga the saga to execute
     * @param context the saga context
     * @param <T> the result type
     * @return a Mono completing with the saga result
     */
    public <T> Mono<T> execute(Saga<T> saga, SagaContext context) {
        log.info("Starting saga execution: type={}, sagaId={}, correlationId={}",
            saga.getSagaType(), context.getSagaId(), context.getCorrelationId());

        return sagaRepository.create(saga.getSagaType(), context)
            .flatMap(instance -> {
                // Sort steps by order
                List<SagaStep<?>> sortedSteps = saga.getSteps().stream()
                    .sorted(Comparator.comparingInt(SagaStep::getOrder))
                    .toList();

                // Initialize all steps in repository
                return initializeSteps(instance.getSagaId(), sortedSteps)
                    .then(executeSteps(saga, context, instance, sortedSteps));
            });
    }

    private Mono<Void> initializeSteps(String sagaId, List<SagaStep<?>> steps) {
        return Flux.fromIterable(steps)
            .flatMap(step -> sagaRepository.saveStep(
                sagaId,
                step.getOrder(),
                step.getStepName(),
                "PENDING"
            ))
            .then();
    }

    private <T> Mono<T> executeSteps(Saga<T> saga, SagaContext context,
                                      SagaInstance instance, List<SagaStep<?>> steps) {
        return executeStepsSequentially(saga, context, instance, steps, 0)
            .flatMap(result -> {
                // All steps completed successfully
                instance.updateStatus(SagaStatus.COMPLETED);
                return sagaRepository.save(instance)
                    .doOnSuccess(saved -> log.info("Saga completed successfully: sagaId={}",
                        context.getSagaId()))
                    .then(Mono.just(result));
            })
            .onErrorResume(error -> {
                log.error("Saga step failed: sagaId={}, step={}, error={}",
                    context.getSagaId(), context.getFailedStep(), error.getMessage());

                return compensate(saga, context, instance, steps, error);
            });
    }

    @SuppressWarnings("unchecked")
    private <T> Mono<T> executeStepsSequentially(Saga<T> saga, SagaContext context,
                                                   SagaInstance instance, List<SagaStep<?>> steps,
                                                   int stepIndex) {
        if (stepIndex >= steps.size()) {
            // All steps completed
            return saga.onComplete(context);
        }

        SagaStep<?> step = steps.get(stepIndex);

        // Update instance state
        instance.startProcessing(stepIndex);

        return sagaRepository.save(instance)
            .then(executeStep(step, context, instance.getSagaId()))
            .doOnSuccess(v -> context.markStepExecuted(step.getStepName()))
            .then(sagaRepository.updateStepStatus(instance.getSagaId(), step.getOrder(),
                "COMPLETED", null))
            .then(executeStepsSequentially(saga, context, instance, steps, stepIndex + 1));
    }

    private Mono<Void> executeStep(SagaStep<?> step, SagaContext context, String sagaId) {
        log.debug("Executing step: sagaId={}, step={}", sagaId, step.getStepName());

        return step.execute(context)
            .then()
            .doOnSuccess(v -> log.debug("Step completed: sagaId={}, step={}",
                sagaId, step.getStepName()))
            .doOnError(error -> {
                context.setFailedStep(step.getStepName());
                context.setFailureReason(error.getMessage());
                log.error("Step failed: sagaId={}, step={}, error={}",
                    sagaId, step.getStepName(), error.getMessage());
            });
    }

    @SuppressWarnings("unchecked")
    private <T> Mono<T> compensate(Saga<T> saga, SagaContext context, SagaInstance instance,
                                    List<SagaStep<?>> steps, Throwable originalError) {
        log.info("Starting compensation: sagaId={}, failedStep={}",
            context.getSagaId(), context.getFailedStep());

        // Update status to COMPENSATING
        instance.updateStatus(SagaStatus.COMPENSATING);
        instance.setError(originalError.getMessage());

        return sagaRepository.save(instance)
            .then(compensateSteps(saga, context, instance, steps))
            .then(Mono.defer(() -> {
                // Compensation completed successfully
                instance.updateStatus(SagaStatus.COMPENSATED);
                return sagaRepository.save(instance)
                    .doOnSuccess(saved -> log.info("Saga compensated: sagaId={}",
                        context.getSagaId()));
            }))
            .<T>then(Mono.error(new SagaExecutionException(
                "Saga execution failed: " + originalError.getMessage(),
                context.getSagaId(),
                saga.getSagaType(),
                context.getFailedStep(),
                originalError
            )))
            .onErrorResume(error -> {
                if (error instanceof SagaExecutionException) {
                    return Mono.error(error);
                }
                // Compensation also failed
                log.error("Compensation failed: sagaId={}, error={}",
                    context.getSagaId(), error.getMessage());

                instance.updateStatus(SagaStatus.FAILED);
                return sagaRepository.save(instance)
                    .<T>then(Mono.error(new SagaExecutionException(
                        "Saga compensation failed: " + error.getMessage(),
                        context.getSagaId(),
                        saga.getSagaType(),
                        context.getFailedStep(),
                        true,
                        error
                    )));
            });
    }

    private Mono<Void> compensateSteps(Saga<?> saga, SagaContext context,
                                        SagaInstance instance, List<SagaStep<?>> steps) {
        // Get executed steps in reverse order
        List<SagaStep<?>> stepsToCompensate = steps.stream()
            .filter(step -> context.isStepExecuted(step.getStepName()))
            .sorted(Comparator.comparingInt((SagaStep<?> s) -> s.getOrder()).reversed())
            .toList();

        return Flux.fromIterable(stepsToCompensate)
            .concatMap(step -> compensateStep(step, context, instance.getSagaId()))
            .then();
    }

    private Mono<Void> compensateStep(SagaStep<?> step, SagaContext context, String sagaId) {
        log.debug("Compensating step: sagaId={}, step={}", sagaId, step.getStepName());

        return step.compensate(context)
            .then(sagaRepository.updateStepStatus(sagaId, step.getOrder(),
                "COMPENSATED", null))
            .doOnSuccess(v -> log.debug("Step compensated: sagaId={}, step={}",
                sagaId, step.getStepName()))
            .doOnError(error -> log.error("Step compensation failed: sagaId={}, step={}, error={}",
                sagaId, step.getStepName(), error.getMessage()));
    }
}
