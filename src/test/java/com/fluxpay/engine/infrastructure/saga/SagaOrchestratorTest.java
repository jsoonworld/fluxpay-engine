package com.fluxpay.engine.infrastructure.saga;

import com.fluxpay.engine.domain.port.outbound.SagaRepository;
import com.fluxpay.engine.domain.saga.Saga;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaInstance;
import com.fluxpay.engine.domain.saga.SagaStatus;
import com.fluxpay.engine.domain.saga.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaOrchestrator")
class SagaOrchestratorTest {

    @Mock
    private SagaRepository sagaRepository;

    private SagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SagaOrchestrator(sagaRepository);
    }

    @Nested
    @DisplayName("Execute")
    class Execute {

        @Test
        @DisplayName("should execute all steps successfully")
        void shouldExecuteAllStepsSuccessfully() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            List<String> executedSteps = new ArrayList<>();

            Saga<String> saga = createTestSaga(
                List.of(
                    createStep("STEP_1", 1, () -> executedSteps.add("STEP_1")),
                    createStep("STEP_2", 2, () -> executedSteps.add("STEP_2"))
                ),
                ctx -> Mono.just("completed")
            );

            SagaInstance instance = SagaInstance.create("saga-1", "TEST_SAGA", "corr-1", "tenant-1");

            when(sagaRepository.create(anyString(), any(SagaContext.class)))
                .thenReturn(Mono.just(instance));
            when(sagaRepository.save(any(SagaInstance.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            when(sagaRepository.saveStep(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.empty());
            when(sagaRepository.updateStepStatus(anyString(), anyInt(), anyString(), any()))
                .thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(orchestrator.execute(saga, context))
                .expectNext("completed")
                .verifyComplete();

            assertThat(executedSteps).containsExactly("STEP_1", "STEP_2");
        }

        @Test
        @DisplayName("should update saga status to PROCESSING when execution starts")
        void shouldUpdateStatusToProcessing() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            AtomicBoolean processingStatusSet = new AtomicBoolean(false);
            Saga<String> saga = createTestSaga(
                List.of(createStep("STEP_1", 1, () -> {})),
                ctx -> Mono.just("done")
            );

            SagaInstance instance = SagaInstance.create("saga-1", "TEST_SAGA", "corr-1", "tenant-1");

            when(sagaRepository.create(anyString(), any(SagaContext.class)))
                .thenReturn(Mono.just(instance));
            when(sagaRepository.save(any(SagaInstance.class)))
                .thenAnswer(inv -> {
                    SagaInstance savedInstance = inv.getArgument(0);
                    if (savedInstance.getStatus() == SagaStatus.PROCESSING) {
                        processingStatusSet.set(true);
                    }
                    return Mono.just(savedInstance);
                });
            when(sagaRepository.saveStep(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.empty());
            when(sagaRepository.updateStepStatus(anyString(), anyInt(), anyString(), any()))
                .thenReturn(Mono.empty());

            // When
            orchestrator.execute(saga, context).block();

            // Then
            assertThat(processingStatusSet.get()).isTrue();
        }

        @Test
        @DisplayName("should update saga status to COMPLETED when all steps succeed")
        void shouldUpdateStatusToCompleted() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            AtomicBoolean completedStatusSet = new AtomicBoolean(false);

            Saga<String> saga = createTestSaga(
                List.of(createStep("STEP_1", 1, () -> {})),
                ctx -> Mono.just("done")
            );

            SagaInstance instance = SagaInstance.create("saga-1", "TEST_SAGA", "corr-1", "tenant-1");

            when(sagaRepository.create(anyString(), any(SagaContext.class)))
                .thenReturn(Mono.just(instance));
            when(sagaRepository.save(any(SagaInstance.class)))
                .thenAnswer(inv -> {
                    SagaInstance savedInstance = inv.getArgument(0);
                    if (savedInstance.getStatus() == SagaStatus.COMPLETED) {
                        completedStatusSet.set(true);
                    }
                    return Mono.just(savedInstance);
                });
            when(sagaRepository.saveStep(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.empty());
            when(sagaRepository.updateStepStatus(anyString(), anyInt(), anyString(), any()))
                .thenReturn(Mono.empty());

            // When
            orchestrator.execute(saga, context).block();

            // Then
            assertThat(completedStatusSet.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Compensation")
    class Compensation {

        @Test
        @DisplayName("should compensate in reverse order when a step fails")
        void shouldCompensateInReverseOrder() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            List<String> compensatedSteps = new ArrayList<>();

            SagaStep<Void> step1 = new TestSagaStep("STEP_1", 1,
                ctx -> Mono.empty(),
                ctx -> { compensatedSteps.add("STEP_1"); return Mono.empty(); }
            );
            SagaStep<Void> step2 = new TestSagaStep("STEP_2", 2,
                ctx -> Mono.empty(),
                ctx -> { compensatedSteps.add("STEP_2"); return Mono.empty(); }
            );
            SagaStep<Void> step3 = new TestSagaStep("STEP_3", 3,
                ctx -> Mono.error(new RuntimeException("Step 3 failed")),
                ctx -> { compensatedSteps.add("STEP_3"); return Mono.empty(); }
            );

            Saga<String> saga = createTestSaga(
                List.of(step1, step2, step3),
                ctx -> Mono.just("done")
            );

            SagaInstance instance = SagaInstance.create("saga-1", "TEST_SAGA", "corr-1", "tenant-1");

            when(sagaRepository.create(anyString(), any(SagaContext.class)))
                .thenReturn(Mono.just(instance));
            when(sagaRepository.save(any(SagaInstance.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            when(sagaRepository.saveStep(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.empty());
            when(sagaRepository.updateStepStatus(anyString(), anyInt(), anyString(), any()))
                .thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(orchestrator.execute(saga, context))
                .expectError(SagaExecutionException.class)
                .verify();

            // STEP_3 was not executed successfully, so only STEP_1 and STEP_2 need compensation
            // Compensation should happen in reverse order
            assertThat(compensatedSteps).containsExactly("STEP_2", "STEP_1");
        }

        @Test
        @DisplayName("should update saga status to COMPENSATING when compensation starts")
        void shouldUpdateStatusToCompensating() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            AtomicBoolean compensatingStatusSet = new AtomicBoolean(false);

            SagaStep<Void> failingStep = new TestSagaStep("FAILING_STEP", 1,
                ctx -> Mono.error(new RuntimeException("Failed")),
                ctx -> Mono.empty()
            );

            Saga<String> saga = createTestSaga(List.of(failingStep), ctx -> Mono.just("done"));

            SagaInstance instance = SagaInstance.create("saga-1", "TEST_SAGA", "corr-1", "tenant-1");

            when(sagaRepository.create(anyString(), any(SagaContext.class)))
                .thenReturn(Mono.just(instance));
            when(sagaRepository.save(any(SagaInstance.class)))
                .thenAnswer(inv -> {
                    SagaInstance savedInstance = inv.getArgument(0);
                    if (savedInstance.getStatus() == SagaStatus.COMPENSATING) {
                        compensatingStatusSet.set(true);
                    }
                    return Mono.just(savedInstance);
                });
            when(sagaRepository.saveStep(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.empty());
            when(sagaRepository.updateStepStatus(anyString(), anyInt(), anyString(), any()))
                .thenReturn(Mono.empty());

            // When
            StepVerifier.create(orchestrator.execute(saga, context))
                .expectError()
                .verify();

            // Then
            assertThat(compensatingStatusSet.get()).isTrue();
        }

        @Test
        @DisplayName("should mark saga as FAILED when compensation fails")
        void shouldMarkAsFailedWhenCompensationFails() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            AtomicBoolean failedStatusSet = new AtomicBoolean(false);

            SagaStep<Void> step1 = new TestSagaStep("STEP_1", 1,
                ctx -> Mono.empty(),
                ctx -> Mono.error(new RuntimeException("Compensation failed"))
            );
            SagaStep<Void> step2 = new TestSagaStep("STEP_2", 2,
                ctx -> Mono.error(new RuntimeException("Step failed")),
                ctx -> Mono.empty()
            );

            Saga<String> saga = createTestSaga(List.of(step1, step2), ctx -> Mono.just("done"));

            SagaInstance instance = SagaInstance.create("saga-1", "TEST_SAGA", "corr-1", "tenant-1");

            when(sagaRepository.create(anyString(), any(SagaContext.class)))
                .thenReturn(Mono.just(instance));
            when(sagaRepository.save(any(SagaInstance.class)))
                .thenAnswer(inv -> {
                    SagaInstance savedInstance = inv.getArgument(0);
                    if (savedInstance.getStatus() == SagaStatus.FAILED) {
                        failedStatusSet.set(true);
                    }
                    return Mono.just(savedInstance);
                });
            when(sagaRepository.saveStep(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.empty());
            when(sagaRepository.updateStepStatus(anyString(), anyInt(), anyString(), any()))
                .thenReturn(Mono.empty());

            // When
            StepVerifier.create(orchestrator.execute(saga, context))
                .expectError()
                .verify();

            // Then
            assertThat(failedStatusSet.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Context Data")
    class ContextData {

        @Test
        @DisplayName("should share context data between steps")
        void shouldShareContextDataBetweenSteps() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            AtomicBoolean dataShared = new AtomicBoolean(false);

            SagaStep<Void> step1 = new TestSagaStep("STEP_1", 1,
                ctx -> {
                    ctx.put("orderId", "order-123");
                    return Mono.empty();
                },
                ctx -> Mono.empty()
            );
            SagaStep<Void> step2 = new TestSagaStep("STEP_2", 2,
                ctx -> {
                    String orderId = ctx.get("orderId", String.class);
                    dataShared.set("order-123".equals(orderId));
                    return Mono.empty();
                },
                ctx -> Mono.empty()
            );

            Saga<String> saga = createTestSaga(List.of(step1, step2), ctx -> Mono.just("done"));

            SagaInstance instance = SagaInstance.create("saga-1", "TEST_SAGA", "corr-1", "tenant-1");

            when(sagaRepository.create(anyString(), any(SagaContext.class)))
                .thenReturn(Mono.just(instance));
            when(sagaRepository.save(any(SagaInstance.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            when(sagaRepository.saveStep(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.empty());
            when(sagaRepository.updateStepStatus(anyString(), anyInt(), anyString(), any()))
                .thenReturn(Mono.empty());

            // When
            orchestrator.execute(saga, context).block();

            // Then
            assertThat(dataShared.get()).isTrue();
        }
    }

    // Helper methods

    private SagaStep<Void> createStep(String name, int order, Runnable onExecute) {
        return new TestSagaStep(name, order,
            ctx -> {
                onExecute.run();
                return Mono.empty();
            },
            ctx -> Mono.empty()
        );
    }

    private Saga<String> createTestSaga(List<SagaStep<?>> steps, java.util.function.Function<SagaContext, Mono<String>> onComplete) {
        return new Saga<>() {
            @Override
            public String getSagaType() {
                return "TEST_SAGA";
            }

            @Override
            public List<SagaStep<?>> getSteps() {
                return steps;
            }

            @Override
            public Mono<String> onComplete(SagaContext context) {
                return onComplete.apply(context);
            }
        };
    }

    private static class TestSagaStep implements SagaStep<Void> {
        private final String stepName;
        private final int order;
        private final java.util.function.Function<SagaContext, Mono<Void>> executeFunc;
        private final java.util.function.Function<SagaContext, Mono<Void>> compensateFunc;

        TestSagaStep(String stepName, int order,
                     java.util.function.Function<SagaContext, Mono<Void>> executeFunc,
                     java.util.function.Function<SagaContext, Mono<Void>> compensateFunc) {
            this.stepName = stepName;
            this.order = order;
            this.executeFunc = executeFunc;
            this.compensateFunc = compensateFunc;
        }

        @Override
        public String getStepName() {
            return stepName;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public Mono<Void> execute(SagaContext context) {
            return executeFunc.apply(context);
        }

        @Override
        public Mono<Void> compensate(SagaContext context) {
            return compensateFunc.apply(context);
        }
    }
}
