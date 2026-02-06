package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaInstance;
import com.fluxpay.engine.domain.saga.SagaStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SagaRepository")
class SagaRepositoryTest {

    @Test
    @DisplayName("should define create method that returns SagaInstance")
    void shouldDefineCreateMethod() {
        SagaRepository repository = new MockSagaRepository();
        SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");

        StepVerifier.create(repository.create("PAYMENT_SAGA", context))
            .assertNext(instance -> {
                assertThat(instance).isNotNull();
                assertThat(instance.getSagaId()).isEqualTo("saga-1");
                assertThat(instance.getSagaType()).isEqualTo("PAYMENT_SAGA");
                assertThat(instance.getStatus()).isEqualTo(SagaStatus.STARTED);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findBySagaId method")
    void shouldDefineFindBySagaIdMethod() {
        SagaRepository repository = new MockSagaRepository();

        StepVerifier.create(repository.findBySagaId("saga-1"))
            .assertNext(instance -> assertThat(instance.getSagaId()).isEqualTo("saga-1"))
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findByCorrelationId method")
    void shouldDefineFindByCorrelationIdMethod() {
        SagaRepository repository = new MockSagaRepository();

        StepVerifier.create(repository.findByCorrelationId("tenant-1", "corr-1"))
            .assertNext(instance -> assertThat(instance.getCorrelationId()).isEqualTo("corr-1"))
            .verifyComplete();
    }

    @Test
    @DisplayName("should define updateStatus method")
    void shouldDefineUpdateStatusMethod() {
        SagaRepository repository = new MockSagaRepository();

        StepVerifier.create(repository.updateStatus("saga-1", SagaStatus.PROCESSING))
            .assertNext(instance -> assertThat(instance.getStatus()).isEqualTo(SagaStatus.PROCESSING))
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findByStatus method")
    void shouldDefineFindByStatusMethod() {
        SagaRepository repository = new MockSagaRepository();

        StepVerifier.create(repository.findByStatus(SagaStatus.PROCESSING).collectList())
            .assertNext(list -> assertThat(list).hasSize(1))
            .verifyComplete();
    }

    /**
     * Mock implementation for testing the interface contract.
     */
    private static class MockSagaRepository implements SagaRepository {

        @Override
        public Mono<SagaInstance> create(String sagaType, SagaContext context) {
            return Mono.just(SagaInstance.create(
                context.getSagaId(),
                sagaType,
                context.getCorrelationId(),
                context.getTenantId()
            ));
        }

        @Override
        public Mono<SagaInstance> findBySagaId(String sagaId) {
            return Mono.just(SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1"));
        }

        @Override
        public Mono<SagaInstance> findByCorrelationId(String tenantId, String correlationId) {
            return Mono.just(SagaInstance.create("saga-1", "PAYMENT_SAGA", correlationId, tenantId));
        }

        @Override
        public Mono<SagaInstance> updateStatus(String sagaId, SagaStatus status) {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");
            instance.updateStatus(status);
            return Mono.just(instance);
        }

        @Override
        public Mono<SagaInstance> save(SagaInstance instance) {
            return Mono.just(instance);
        }

        @Override
        public Flux<SagaInstance> findByStatus(SagaStatus status) {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");
            instance.updateStatus(status);
            return Flux.just(instance);
        }

        @Override
        public Mono<Void> saveStep(String sagaId, int stepOrder, String stepName, String status) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> updateStepStatus(String sagaId, int stepOrder, String status, String errorMessage) {
            return Mono.empty();
        }
    }
}
