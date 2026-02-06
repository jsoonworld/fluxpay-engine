package com.fluxpay.engine.domain.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SagaInstance")
class SagaInstanceTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create instance with STARTED status")
        void shouldCreateInstanceWithStartedStatus() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");

            assertThat(instance.getSagaId()).isEqualTo("saga-1");
            assertThat(instance.getSagaType()).isEqualTo("PAYMENT_SAGA");
            assertThat(instance.getCorrelationId()).isEqualTo("corr-1");
            assertThat(instance.getTenantId()).isEqualTo("tenant-1");
            assertThat(instance.getStatus()).isEqualTo(SagaStatus.STARTED);
            assertThat(instance.getCurrentStep()).isEqualTo(0);
            assertThat(instance.getStartedAt()).isNotNull();
            assertThat(instance.getUpdatedAt()).isNotNull();
            assertThat(instance.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("should throw when sagaId is null")
        void shouldThrowWhenSagaIdIsNull() {
            assertThatThrownBy(() -> SagaInstance.create(null, "PAYMENT_SAGA", "corr-1", "tenant-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sagaId");
        }

        @Test
        @DisplayName("should throw when sagaType is null")
        void shouldThrowWhenSagaTypeIsNull() {
            assertThatThrownBy(() -> SagaInstance.create("saga-1", null, "corr-1", "tenant-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sagaType");
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition from STARTED to PROCESSING")
        void shouldTransitionFromStartedToProcessing() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");

            instance.updateStatus(SagaStatus.PROCESSING);

            assertThat(instance.getStatus()).isEqualTo(SagaStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition from PROCESSING to COMPLETED")
        void shouldTransitionFromProcessingToCompleted() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");
            instance.updateStatus(SagaStatus.PROCESSING);

            instance.updateStatus(SagaStatus.COMPLETED);

            assertThat(instance.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(instance.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should transition from PROCESSING to COMPENSATING")
        void shouldTransitionFromProcessingToCompensating() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");
            instance.updateStatus(SagaStatus.PROCESSING);

            instance.updateStatus(SagaStatus.COMPENSATING);

            assertThat(instance.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        }

        @Test
        @DisplayName("should throw when invalid transition")
        void shouldThrowWhenInvalidTransition() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");

            assertThatThrownBy(() -> instance.updateStatus(SagaStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
        }
    }

    @Nested
    @DisplayName("Processing")
    class Processing {

        @Test
        @DisplayName("should start processing and set step index")
        void shouldStartProcessingAndSetStepIndex() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");

            instance.startProcessing(1);

            assertThat(instance.getStatus()).isEqualTo(SagaStatus.PROCESSING);
            assertThat(instance.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should update step index without changing status when already processing")
        void shouldUpdateStepIndexWithoutChangingStatus() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");
            instance.updateStatus(SagaStatus.PROCESSING);

            instance.startProcessing(2);

            assertThat(instance.getStatus()).isEqualTo(SagaStatus.PROCESSING);
            assertThat(instance.getCurrentStep()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should set error message")
        void shouldSetErrorMessage() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");

            instance.setError("Payment failed");

            assertThat(instance.getErrorMessage()).isEqualTo("Payment failed");
        }

        @Test
        @DisplayName("should set context data")
        void shouldSetContextData() {
            SagaInstance instance = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");

            instance.setContextData("{\"orderId\": \"order-123\"}");

            assertThat(instance.getContextData()).isEqualTo("{\"orderId\": \"order-123\"}");
        }
    }

    @Nested
    @DisplayName("Restore")
    class Restore {

        @Test
        @DisplayName("should restore instance from persistence")
        void shouldRestoreInstanceFromPersistence() {
            Instant now = Instant.now();

            SagaInstance instance = SagaInstance.restore(
                "saga-1",
                "PAYMENT_SAGA",
                "corr-1",
                "tenant-1",
                SagaStatus.PROCESSING,
                2,
                "{\"orderId\": \"order-123\"}",
                null,
                now.minusSeconds(60),
                null,
                now
            );

            assertThat(instance.getSagaId()).isEqualTo("saga-1");
            assertThat(instance.getStatus()).isEqualTo(SagaStatus.PROCESSING);
            assertThat(instance.getCurrentStep()).isEqualTo(2);
            assertThat(instance.getContextData()).isEqualTo("{\"orderId\": \"order-123\"}");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when sagaId is same")
        void shouldBeEqualWhenSagaIdIsSame() {
            Instant now = Instant.now();
            SagaInstance instance1 = SagaInstance.restore(
                "saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1",
                SagaStatus.STARTED, 0, null, null, now, null, now
            );
            SagaInstance instance2 = SagaInstance.restore(
                "saga-1", "PAYMENT_SAGA", "corr-2", "tenant-2",
                SagaStatus.COMPLETED, 5, null, null, now, now, now
            );

            assertThat(instance1).isEqualTo(instance2);
            assertThat(instance1.hashCode()).isEqualTo(instance2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when sagaId is different")
        void shouldNotBeEqualWhenSagaIdIsDifferent() {
            SagaInstance instance1 = SagaInstance.create("saga-1", "PAYMENT_SAGA", "corr-1", "tenant-1");
            SagaInstance instance2 = SagaInstance.create("saga-2", "PAYMENT_SAGA", "corr-1", "tenant-1");

            assertThat(instance1).isNotEqualTo(instance2);
        }
    }
}
