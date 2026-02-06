package com.fluxpay.engine.domain.saga.payment;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentStep")
class ProcessPaymentStepTest {

    @Mock
    private PaymentService paymentService;

    private ProcessPaymentStep step;

    @BeforeEach
    void setUp() {
        step = new ProcessPaymentStep(paymentService);
    }

    @Test
    @DisplayName("should have correct step name")
    void shouldHaveCorrectStepName() {
        assertThat(step.getStepName()).isEqualTo("PROCESS_PAYMENT");
    }

    @Test
    @DisplayName("should have correct order")
    void shouldHaveCorrectOrder() {
        assertThat(step.getOrder()).isEqualTo(2);
    }

    @Nested
    @DisplayName("Execute")
    class Execute {

        @Test
        @DisplayName("should initiate payment and store paymentId in context")
        void shouldInitiatePaymentAndStorePaymentIdInContext() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            Order order = Order.create("user-123",
                List.of(OrderLineItem.create("prod-1", "Product 1", 2, Money.krw(10000))),
                Currency.KRW, Map.of());
            context.put("order", order);

            Payment payment = Payment.create(order.getId(), order.getTotalAmount());

            when(paymentService.createPayment(order.getId(), order.getTotalAmount()))
                .thenReturn(Mono.just(payment));

            // When & Then
            StepVerifier.create(step.execute(context))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getId()).isNotNull();
                })
                .verifyComplete();

            assertThat(context.get("paymentId", PaymentId.class)).isNotNull();
        }

        @Test
        @DisplayName("should propagate error when payment initiation fails")
        void shouldPropagateErrorWhenPaymentInitiationFails() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            Order order = Order.create("user-123",
                List.of(OrderLineItem.create("prod-1", "Product 1", 2, Money.krw(10000))),
                Currency.KRW, Map.of());
            context.put("order", order);

            when(paymentService.createPayment(order.getId(), order.getTotalAmount()))
                .thenReturn(Mono.error(new RuntimeException("Payment initiation failed")));

            // When & Then
            StepVerifier.create(step.execute(context))
                .expectErrorMessage("Payment initiation failed")
                .verify();
        }
    }

    @Nested
    @DisplayName("Compensate")
    class Compensate {

        @Test
        @DisplayName("should cancel payment when paymentId exists in context")
        void shouldCancelPaymentWhenPaymentIdExists() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            PaymentId paymentId = PaymentId.generate();
            context.put("paymentId", paymentId);

            when(paymentService.failPayment(paymentId, "Saga compensation"))
                .thenReturn(Mono.just(Payment.create(
                    com.fluxpay.engine.domain.model.order.OrderId.generate(), Money.krw(10000))));

            // When & Then
            StepVerifier.create(step.compensate(context))
                .verifyComplete();

            verify(paymentService).failPayment(paymentId, "Saga compensation");
        }

        @Test
        @DisplayName("should complete without error when paymentId is not in context")
        void shouldCompleteWithoutErrorWhenPaymentIdNotInContext() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");

            // When & Then
            StepVerifier.create(step.compensate(context))
                .verifyComplete();

            verify(paymentService, never()).failPayment(any(), any());
        }
    }
}
