package com.fluxpay.engine.domain.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Saga")
class SagaTest {

    @Test
    @DisplayName("should define saga type")
    void shouldDefineSagaType() {
        Saga<String> saga = new TestSaga();

        assertThat(saga.getSagaType()).isEqualTo("TEST_SAGA");
    }

    @Test
    @DisplayName("should return list of steps")
    void shouldReturnListOfSteps() {
        Saga<String> saga = new TestSaga();

        List<SagaStep<?>> steps = saga.getSteps();

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).getStepName()).isEqualTo("STEP_1");
        assertThat(steps.get(1).getStepName()).isEqualTo("STEP_2");
    }

    @Test
    @DisplayName("should execute onComplete with context")
    void shouldExecuteOnCompleteWithContext() {
        Saga<String> saga = new TestSaga();
        SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
        context.put("result", "completed");

        StepVerifier.create(saga.onComplete(context))
            .expectNext("completed")
            .verifyComplete();
    }

    /**
     * Test implementation of Saga for testing purposes.
     */
    private static class TestSaga implements Saga<String> {

        @Override
        public String getSagaType() {
            return "TEST_SAGA";
        }

        @Override
        public List<SagaStep<?>> getSteps() {
            return List.of(
                new TestStep("STEP_1", 1),
                new TestStep("STEP_2", 2)
            );
        }

        @Override
        public Mono<String> onComplete(SagaContext context) {
            String result = context.get("result", String.class);
            return Mono.justOrEmpty(result);
        }
    }

    private static class TestStep implements SagaStep<Void> {
        private final String stepName;
        private final int order;

        TestStep(String stepName, int order) {
            this.stepName = stepName;
            this.order = order;
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
            return Mono.empty();
        }

        @Override
        public Mono<Void> compensate(SagaContext context) {
            return Mono.empty();
        }
    }
}
