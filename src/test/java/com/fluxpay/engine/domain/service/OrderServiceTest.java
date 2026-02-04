package com.fluxpay.engine.domain.service;

import com.fluxpay.engine.domain.exception.OrderNotFoundException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.model.order.OrderStatus;
import com.fluxpay.engine.domain.port.outbound.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderService following TDD RED phase.
 * These tests define the expected behavior before implementation.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private OrderLineItem testLineItem;
    private Order testOrder;
    private OrderId testOrderId;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "user_123";
        testOrderId = OrderId.generate();

        // Create test line item
        testLineItem = OrderLineItem.create(
            "prod_001",
            "Test Product",
            2,
            Money.of(BigDecimal.valueOf(10000), Currency.KRW)
        );

        // Create test order using the domain factory method
        testOrder = Order.create(
            testUserId,
            List.of(testLineItem),
            Currency.KRW,
            Map.of("source", "test")
        );
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("should create order and save to repository")
        void shouldCreateOrder() {
            // Given
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Mono.just(order);
            });

            List<OrderLineItem> lineItems = List.of(testLineItem);
            Currency currency = Currency.KRW;
            Map<String, Object> metadata = Map.of("source", "test");

            // When
            Mono<Order> result = orderService.createOrder(testUserId, lineItems, currency, metadata);

            // Then
            StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order).isNotNull();
                    assertThat(order.getId()).isNotNull();
                    assertThat(order.getUserId()).isEqualTo(testUserId);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.getLineItems()).hasSize(1);
                    assertThat(order.getCurrency()).isEqualTo(Currency.KRW);
                    assertThat(order.getTotalAmount().amount())
                        .isEqualByComparingTo(BigDecimal.valueOf(20000));
                })
                .verifyComplete();

            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should create order with multiple line items")
        void shouldCreateOrderWithMultipleLineItems() {
            // Given
            OrderLineItem lineItem1 = OrderLineItem.create(
                "prod_001", "Product 1", 2,
                Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );
            OrderLineItem lineItem2 = OrderLineItem.create(
                "prod_002", "Product 2", 1,
                Money.of(BigDecimal.valueOf(5000), Currency.KRW)
            );

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Mono.just(order);
            });

            // When
            Mono<Order> result = orderService.createOrder(
                testUserId,
                List.of(lineItem1, lineItem2),
                Currency.KRW,
                null
            );

            // Then
            StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getLineItems()).hasSize(2);
                    assertThat(order.getTotalAmount().amount())
                        .isEqualByComparingTo(BigDecimal.valueOf(25000));
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrderTests {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrderById() {
            // Given
            when(orderRepository.findById(testOrderId)).thenReturn(Mono.just(testOrder));

            // When
            Mono<Order> result = orderService.getOrder(testOrderId);

            // Then
            StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order).isNotNull();
                    assertThat(order.getUserId()).isEqualTo(testUserId);
                })
                .verifyComplete();

            verify(orderRepository).findById(testOrderId);
        }

        @Test
        @DisplayName("should throw OrderNotFoundException when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            OrderId nonExistentId = OrderId.generate();
            when(orderRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Order> result = orderService.getOrder(nonExistentId);

            // Then
            StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(OrderNotFoundException.class);
                })
                .verify();
        }
    }

    @Nested
    @DisplayName("getOrdersByUserId")
    class GetOrdersByUserIdTests {

        @Test
        @DisplayName("should return orders for user")
        void shouldReturnOrdersByUserId() {
            // Given
            Order order1 = Order.create(testUserId, List.of(testLineItem), Currency.KRW, null);
            Order order2 = Order.create(testUserId, List.of(testLineItem), Currency.KRW, null);

            when(orderRepository.findByUserId(testUserId)).thenReturn(Flux.just(order1, order2));

            // When
            Flux<Order> result = orderService.getOrdersByUserId(testUserId);

            // Then
            StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

            verify(orderRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("should return empty flux when user has no orders")
        void shouldReturnEmptyFluxWhenNoOrders() {
            // Given
            String userWithNoOrders = "user_no_orders";
            when(orderRepository.findByUserId(userWithNoOrders)).thenReturn(Flux.empty());

            // When
            Flux<Order> result = orderService.getOrdersByUserId(userWithNoOrders);

            // Then
            StepVerifier.create(result)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("markOrderAsPaid")
    class MarkOrderAsPaidTests {

        @Test
        @DisplayName("should mark order as paid")
        void shouldMarkOrderAsPaid() {
            // Given
            Order pendingOrder = Order.create(testUserId, List.of(testLineItem), Currency.KRW, null);

            when(orderRepository.findById(any(OrderId.class))).thenReturn(Mono.just(pendingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Mono.just(order);
            });

            // When
            Mono<Order> result = orderService.markOrderAsPaid(pendingOrder.getId());

            // Then
            StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
                    assertThat(order.getPaidAt()).isNotNull();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw exception when order not found for marking as paid")
        void shouldThrowExceptionWhenOrderNotFoundForPaid() {
            // Given
            OrderId nonExistentId = OrderId.generate();
            when(orderRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Order> result = orderService.markOrderAsPaid(nonExistentId);

            // Then
            StepVerifier.create(result)
                .expectError(OrderNotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("completeOrder")
    class CompleteOrderTests {

        @Test
        @DisplayName("should complete order")
        void shouldCompleteOrder() {
            // Given
            Order paidOrder = Order.create(testUserId, List.of(testLineItem), Currency.KRW, null);
            paidOrder.markAsPaid(); // Must be in PAID state to complete

            when(orderRepository.findById(any(OrderId.class))).thenReturn(Mono.just(paidOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Mono.just(order);
            });

            // When
            Mono<Order> result = orderService.completeOrder(paidOrder.getId());

            // Then
            StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getCompletedAt()).isNotNull();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw exception when order not found for completion")
        void shouldThrowExceptionWhenOrderNotFoundForComplete() {
            // Given
            OrderId nonExistentId = OrderId.generate();
            when(orderRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Order> result = orderService.completeOrder(nonExistentId);

            // Then
            StepVerifier.create(result)
                .expectError(OrderNotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("should cancel order")
        void shouldCancelOrder() {
            // Given
            Order pendingOrder = Order.create(testUserId, List.of(testLineItem), Currency.KRW, null);

            when(orderRepository.findById(any(OrderId.class))).thenReturn(Mono.just(pendingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Mono.just(order);
            });

            // When
            Mono<Order> result = orderService.cancelOrder(pendingOrder.getId());

            // Then
            StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw exception when order not found for cancellation")
        void shouldThrowExceptionWhenOrderNotFoundForCancel() {
            // Given
            OrderId nonExistentId = OrderId.generate();
            when(orderRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Order> result = orderService.cancelOrder(nonExistentId);

            // Then
            StepVerifier.create(result)
                .expectError(OrderNotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("failOrder")
    class FailOrderTests {

        @Test
        @DisplayName("should fail order")
        void shouldFailOrder() {
            // Given
            Order pendingOrder = Order.create(testUserId, List.of(testLineItem), Currency.KRW, null);

            when(orderRepository.findById(any(OrderId.class))).thenReturn(Mono.just(pendingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Mono.just(order);
            });

            // When
            Mono<Order> result = orderService.failOrder(pendingOrder.getId());

            // Then
            StepVerifier.create(result)
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw exception when order not found for failing")
        void shouldThrowExceptionWhenOrderNotFoundForFail() {
            // Given
            OrderId nonExistentId = OrderId.generate();
            when(orderRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Order> result = orderService.failOrder(nonExistentId);

            // Then
            StepVerifier.create(result)
                .expectError(OrderNotFoundException.class)
                .verify();
        }
    }
}
