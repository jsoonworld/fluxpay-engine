package com.fluxpay.engine.domain.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SagaContext")
class SagaContextTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create context with sagaId and correlationId")
        void shouldCreateContextWithIds() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            assertThat(context.getSagaId()).isEqualTo("saga-123");
            assertThat(context.getCorrelationId()).isEqualTo("correlation-456");
            assertThat(context.getTenantId()).isEqualTo("tenant-1");
        }

        @Test
        @DisplayName("should throw when sagaId is null")
        void shouldThrowWhenSagaIdIsNull() {
            assertThatThrownBy(() -> SagaContext.create(null, "correlation-456", "tenant-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sagaId");
        }

        @Test
        @DisplayName("should throw when correlationId is null")
        void shouldThrowWhenCorrelationIdIsNull() {
            assertThatThrownBy(() -> SagaContext.create("saga-123", null, "tenant-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("correlationId");
        }

        @Test
        @DisplayName("should throw when tenantId is null")
        void shouldThrowWhenTenantIdIsNull() {
            assertThatThrownBy(() -> SagaContext.create("saga-123", "correlation-456", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
        }
    }

    @Nested
    @DisplayName("Data Operations")
    class DataOperations {

        @Test
        @DisplayName("should store and retrieve data by key")
        void shouldStoreAndRetrieveData() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.put("orderId", "order-789");

            assertThat(context.get("orderId", String.class)).isEqualTo("order-789");
        }

        @Test
        @DisplayName("should return null for non-existent key")
        void shouldReturnNullForNonExistentKey() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            assertThat(context.get("nonExistent", String.class)).isNull();
        }

        @Test
        @DisplayName("should store multiple values")
        void shouldStoreMultipleValues() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.put("orderId", "order-789");
            context.put("paymentId", "payment-101");
            context.put("amount", 10000L);

            assertThat(context.get("orderId", String.class)).isEqualTo("order-789");
            assertThat(context.get("paymentId", String.class)).isEqualTo("payment-101");
            assertThat(context.get("amount", Long.class)).isEqualTo(10000L);
        }

        @Test
        @DisplayName("should overwrite existing value")
        void shouldOverwriteExistingValue() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.put("orderId", "order-789");
            context.put("orderId", "order-updated");

            assertThat(context.get("orderId", String.class)).isEqualTo("order-updated");
        }

        @Test
        @DisplayName("should check if key exists")
        void shouldCheckIfKeyExists() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.put("orderId", "order-789");

            assertThat(context.containsKey("orderId")).isTrue();
            assertThat(context.containsKey("nonExistent")).isFalse();
        }

        @Test
        @DisplayName("should return all data as unmodifiable map")
        void shouldReturnAllDataAsUnmodifiableMap() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.put("key1", "value1");
            context.put("key2", "value2");

            var data = context.getData();

            assertThat(data).hasSize(2);
            assertThat(data).containsEntry("key1", "value1");
            assertThat(data).containsEntry("key2", "value2");
        }
    }

    @Nested
    @DisplayName("Step Tracking")
    class StepTracking {

        @Test
        @DisplayName("should track executed steps")
        void shouldTrackExecutedSteps() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.markStepExecuted("CREATE_ORDER");
            context.markStepExecuted("PROCESS_PAYMENT");

            assertThat(context.isStepExecuted("CREATE_ORDER")).isTrue();
            assertThat(context.isStepExecuted("PROCESS_PAYMENT")).isTrue();
            assertThat(context.isStepExecuted("CONFIRM_PAYMENT")).isFalse();
        }

        @Test
        @DisplayName("should return executed steps in order")
        void shouldReturnExecutedStepsInOrder() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.markStepExecuted("CREATE_ORDER");
            context.markStepExecuted("PROCESS_PAYMENT");
            context.markStepExecuted("CONFIRM_PAYMENT");

            assertThat(context.getExecutedSteps())
                .containsExactly("CREATE_ORDER", "PROCESS_PAYMENT", "CONFIRM_PAYMENT");
        }

        @Test
        @DisplayName("should track current step index")
        void shouldTrackCurrentStepIndex() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            assertThat(context.getCurrentStepIndex()).isEqualTo(0);

            context.incrementStepIndex();
            assertThat(context.getCurrentStepIndex()).isEqualTo(1);

            context.incrementStepIndex();
            assertThat(context.getCurrentStepIndex()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should store failure information")
        void shouldStoreFailureInfo() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            context.setFailedStep("PROCESS_PAYMENT");
            context.setFailureReason("Payment gateway timeout");

            assertThat(context.getFailedStep()).isEqualTo("PROCESS_PAYMENT");
            assertThat(context.getFailureReason()).isEqualTo("Payment gateway timeout");
        }

        @Test
        @DisplayName("should return null for failure info when not set")
        void shouldReturnNullForFailureInfoWhenNotSet() {
            SagaContext context = SagaContext.create("saga-123", "correlation-456", "tenant-1");

            assertThat(context.getFailedStep()).isNull();
            assertThat(context.getFailureReason()).isNull();
        }
    }
}
