package com.fluxpay.engine.domain.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SagaStep")
class SagaStepTest {

    @Test
    @DisplayName("should define step name")
    void shouldDefineStepName() {
        SagaStep<String> step = new TestStep("CREATE_ORDER", 1);

        assertThat(step.getStepName()).isEqualTo("CREATE_ORDER");
    }

    @Test
    @DisplayName("should define step order")
    void shouldDefineStepOrder() {
        SagaStep<String> step = new TestStep("CREATE_ORDER", 1);

        assertThat(step.getOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("should execute step with context")
    void shouldExecuteStepWithContext() {
        SagaStep<String> step = new TestStep("CREATE_ORDER", 1);
        SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");

        StepVerifier.create(step.execute(context))
            .expectNext("executed")
            .verifyComplete();
    }

    @Test
    @DisplayName("should compensate step with context")
    void shouldCompensateStepWithContext() {
        SagaStep<String> step = new TestStep("CREATE_ORDER", 1);
        SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");

        StepVerifier.create(step.compensate(context))
            .verifyComplete();
    }

    /**
     * Test implementation of SagaStep for testing purposes.
     */
    private static class TestStep implements SagaStep<String> {
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
        public Mono<String> execute(SagaContext context) {
            return Mono.just("executed");
        }

        @Override
        public Mono<Void> compensate(SagaContext context) {
            return Mono.empty();
        }
    }
}
