package com.fluxpay.engine.domain.saga.payment;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.saga.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentSaga")
class PaymentSagaTest {

    @Mock
    private CreateOrderStep createOrderStep;

    @Mock
    private ProcessPaymentStep processPaymentStep;

    private PaymentSaga saga;

    @BeforeEach
    void setUp() {
        saga = new PaymentSaga(createOrderStep, processPaymentStep);
    }

    @Test
    @DisplayName("should have correct saga type")
    void shouldHaveCorrectSagaType() {
        assertThat(saga.getSagaType()).isEqualTo("PAYMENT_SAGA");
    }

    @Test
    @DisplayName("should return steps in correct order")
    void shouldReturnStepsInCorrectOrder() {
        List<SagaStep<?>> steps = saga.getSteps();

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0)).isInstanceOf(CreateOrderStep.class);
        assertThat(steps.get(1)).isInstanceOf(ProcessPaymentStep.class);
    }

    @Nested
    @DisplayName("OnComplete")
    class OnComplete {

        @Test
        @DisplayName("should return payment result from context")
        void shouldReturnPaymentResultFromContext() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            Order order = Order.create("user-123",
                List.of(OrderLineItem.create("prod-1", "Product 1", 2, Money.krw(10000))),
                Currency.KRW, Map.of());
            Payment payment = Payment.create(order.getId(), order.getTotalAmount());
            context.put("order", order);
            context.put("payment", payment);

            // When & Then
            StepVerifier.create(saga.onComplete(context))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.order()).isEqualTo(order);
                    assertThat(result.payment()).isEqualTo(payment);
                })
                .verifyComplete();
        }
    }
}
