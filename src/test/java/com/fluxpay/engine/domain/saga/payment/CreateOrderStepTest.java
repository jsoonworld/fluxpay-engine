package com.fluxpay.engine.domain.saga.payment;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.saga.SagaContext;
import com.fluxpay.engine.domain.service.OrderService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderStep")
class CreateOrderStepTest {

    @Mock
    private OrderService orderService;

    private CreateOrderStep step;

    @BeforeEach
    void setUp() {
        step = new CreateOrderStep(orderService);
    }

    @Test
    @DisplayName("should have correct step name")
    void shouldHaveCorrectStepName() {
        assertThat(step.getStepName()).isEqualTo("CREATE_ORDER");
    }

    @Test
    @DisplayName("should have correct order")
    void shouldHaveCorrectOrder() {
        assertThat(step.getOrder()).isEqualTo(1);
    }

    @Nested
    @DisplayName("Execute")
    class Execute {

        @Test
        @DisplayName("should create order and store orderId in context")
        void shouldCreateOrderAndStoreOrderIdInContext() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            context.put("userId", "user-123");
            context.put("lineItems", List.of(
                OrderLineItem.create("prod-1", "Product 1", 2, Money.krw(10000))
            ));
            context.put("currency", Currency.KRW);

            Order order = Order.create("user-123",
                List.of(OrderLineItem.create("prod-1", "Product 1", 2, Money.krw(10000))),
                Currency.KRW, Map.of());

            when(orderService.createOrder(eq("user-123"), anyList(), eq(Currency.KRW), any()))
                .thenReturn(Mono.just(order));

            // When & Then
            StepVerifier.create(step.execute(context))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getId()).isNotNull();
                })
                .verifyComplete();

            assertThat(context.get("orderId", OrderId.class)).isNotNull();
        }

        @Test
        @DisplayName("should propagate error when order creation fails")
        void shouldPropagateErrorWhenOrderCreationFails() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            context.put("userId", "user-123");
            context.put("lineItems", List.of(
                OrderLineItem.create("prod-1", "Product 1", 2, Money.krw(10000))
            ));
            context.put("currency", Currency.KRW);

            when(orderService.createOrder(eq("user-123"), anyList(), eq(Currency.KRW), any()))
                .thenReturn(Mono.error(new RuntimeException("Order creation failed")));

            // When & Then
            StepVerifier.create(step.execute(context))
                .expectErrorMessage("Order creation failed")
                .verify();
        }
    }

    @Nested
    @DisplayName("Compensate")
    class Compensate {

        @Test
        @DisplayName("should cancel order when orderId exists in context")
        void shouldCancelOrderWhenOrderIdExists() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");
            OrderId orderId = OrderId.generate();
            context.put("orderId", orderId);

            Order order = Order.create("user-123",
                List.of(OrderLineItem.create("prod-1", "Product 1", 2, Money.krw(10000))),
                Currency.KRW, Map.of());

            when(orderService.cancelOrder(orderId)).thenReturn(Mono.just(order));

            // When & Then
            StepVerifier.create(step.compensate(context))
                .verifyComplete();

            verify(orderService).cancelOrder(orderId);
        }

        @Test
        @DisplayName("should complete without error when orderId is not in context")
        void shouldCompleteWithoutErrorWhenOrderIdNotInContext() {
            // Given
            SagaContext context = SagaContext.create("saga-1", "corr-1", "tenant-1");

            // When & Then
            StepVerifier.create(step.compensate(context))
                .verifyComplete();

            verify(orderService, never()).cancelOrder(any());
        }
    }
}
