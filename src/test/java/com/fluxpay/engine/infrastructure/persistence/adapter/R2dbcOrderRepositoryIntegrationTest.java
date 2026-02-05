package com.fluxpay.engine.infrastructure.persistence.adapter;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for R2dbcOrderRepository.
 * Uses Testcontainers to spin up a PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("R2dbcOrderRepository Integration Tests")
class R2dbcOrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fluxpay_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:postgresql://%s:%d/%s",
                        postgres.getHost(),
                        postgres.getFirstMappedPort(),
                        postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        databaseClient.sql("DELETE FROM order_line_items").then().block();
        databaseClient.sql("DELETE FROM orders").then().block();
    }

    private Order createTestOrder(String userId) {
        OrderLineItem lineItem = OrderLineItem.create(
                "PROD-001",
                "Test Product",
                2,
                Money.of(BigDecimal.valueOf(10000), Currency.KRW)
        );

        return Order.create(
                userId,
                List.of(lineItem),
                Currency.KRW,
                Map.of("source", "test", "channel", "WEB")
        );
    }

    private Order createTestOrderWithMultipleItems(String userId) {
        OrderLineItem lineItem1 = OrderLineItem.create(
                "PROD-001",
                "Product 1",
                2,
                Money.of(BigDecimal.valueOf(10000), Currency.KRW)
        );
        OrderLineItem lineItem2 = OrderLineItem.create(
                "PROD-002",
                "Product 2",
                1,
                Money.of(BigDecimal.valueOf(5000), Currency.KRW)
        );

        return Order.create(
                userId,
                List.of(lineItem1, lineItem2),
                Currency.KRW,
                null
        );
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should save new order with line items")
        void shouldSaveNewOrder() {
            // Given
            Order order = createTestOrder("user-123");

            // When
            var result = orderRepository.save(order);

            // Then
            StepVerifier.create(result)
                    .assertNext(savedOrder -> {
                        assertThat(savedOrder.getId()).isNotNull();
                        assertThat(savedOrder.getUserId()).isEqualTo("user-123");
                        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
                        assertThat(savedOrder.getLineItems()).hasSize(1);
                        assertThat(savedOrder.getTotalAmount().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(20000));
                        assertThat(savedOrder.getMetadata()).containsEntry("source", "test");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should save order with multiple line items")
        void shouldSaveOrderWithMultipleLineItems() {
            // Given
            Order order = createTestOrderWithMultipleItems("user-456");

            // When
            var result = orderRepository.save(order);

            // Then
            StepVerifier.create(result)
                    .assertNext(savedOrder -> {
                        assertThat(savedOrder.getLineItems()).hasSize(2);
                        assertThat(savedOrder.getTotalAmount().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(25000));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should update existing order")
        void shouldUpdateExistingOrder() {
            // Given
            Order order = createTestOrder("user-789");
            Order savedOrder = orderRepository.save(order).block();
            assertThat(savedOrder).isNotNull();

            // Mark as paid
            savedOrder.markAsPaid();

            // When
            var result = orderRepository.save(savedOrder);

            // Then
            StepVerifier.create(result)
                    .assertNext(updatedOrder -> {
                        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
                        assertThat(updatedOrder.getPaidAt()).isNotNull();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should find order by id with line items")
        void shouldFindOrderById() {
            // Given
            Order order = createTestOrder("user-111");
            Order savedOrder = orderRepository.save(order).block();
            assertThat(savedOrder).isNotNull();

            // When
            var result = orderRepository.findById(savedOrder.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(foundOrder -> {
                        assertThat(foundOrder.getId()).isEqualTo(savedOrder.getId());
                        assertThat(foundOrder.getUserId()).isEqualTo("user-111");
                        assertThat(foundOrder.getLineItems()).hasSize(1);
                        assertThat(foundOrder.getLineItems().get(0).getProductId()).isEqualTo("PROD-001");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty when order not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            OrderId nonExistentId = OrderId.generate();

            // When
            var result = orderRepository.findById(nonExistentId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("should find all orders for user")
        void shouldFindAllOrdersForUser() {
            // Given
            String userId = "user-multi";
            Order order1 = createTestOrder(userId);
            Order order2 = createTestOrder(userId);
            orderRepository.save(order1).block();
            orderRepository.save(order2).block();

            // When
            var result = orderRepository.findByUserId(userId);

            // Then
            StepVerifier.create(result.collectList())
                    .assertNext(orders -> {
                        assertThat(orders).hasSize(2);
                        assertThat(orders).allMatch(o -> o.getUserId().equals(userId));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty flux when no orders found")
        void shouldReturnEmptyFluxWhenNoOrders() {
            // When
            var result = orderRepository.findByUserId("non-existent-user");

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should not return orders of other users")
        void shouldNotReturnOrdersOfOtherUsers() {
            // Given
            String userId1 = "user-A";
            String userId2 = "user-B";
            orderRepository.save(createTestOrder(userId1)).block();
            orderRepository.save(createTestOrder(userId2)).block();

            // When
            var result = orderRepository.findByUserId(userId1);

            // Then
            StepVerifier.create(result.collectList())
                    .assertNext(orders -> {
                        assertThat(orders).hasSize(1);
                        assertThat(orders.get(0).getUserId()).isEqualTo(userId1);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("should return true when order exists")
        void shouldReturnTrueWhenExists() {
            // Given
            Order order = createTestOrder("user-exists");
            Order savedOrder = orderRepository.save(order).block();
            assertThat(savedOrder).isNotNull();

            // When
            var result = orderRepository.existsById(savedOrder.getId());

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return false when order does not exist")
        void shouldReturnFalseWhenNotExists() {
            // Given
            OrderId nonExistentId = OrderId.generate();

            // When
            var result = orderRepository.existsById(nonExistentId);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("should delete order and its line items")
        void shouldDeleteOrder() {
            // Given
            Order order = createTestOrder("user-delete");
            Order savedOrder = orderRepository.save(order).block();
            assertThat(savedOrder).isNotNull();

            // When
            var deleteResult = orderRepository.deleteById(savedOrder.getId());

            // Then
            StepVerifier.create(deleteResult)
                    .verifyComplete();

            // Verify order no longer exists
            StepVerifier.create(orderRepository.findById(savedOrder.getId()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should not fail when deleting non-existent order")
        void shouldNotFailWhenDeletingNonExistent() {
            // Given
            OrderId nonExistentId = OrderId.generate();

            // When
            var result = orderRepository.deleteById(nonExistentId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    class DataIntegrity {

        @Test
        @DisplayName("should preserve all order fields after save and retrieve")
        void shouldPreserveAllFields() {
            // Given
            Order order = createTestOrder("user-integrity");
            Order savedOrder = orderRepository.save(order).block();
            assertThat(savedOrder).isNotNull();

            // Mark as paid and complete
            savedOrder.markAsPaid();
            savedOrder.complete();
            orderRepository.save(savedOrder).block();

            // When
            var result = orderRepository.findById(savedOrder.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(foundOrder -> {
                        assertThat(foundOrder.getId()).isEqualTo(savedOrder.getId());
                        assertThat(foundOrder.getUserId()).isEqualTo("user-integrity");
                        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                        assertThat(foundOrder.getCurrency()).isEqualTo(Currency.KRW);
                        assertThat(foundOrder.getCreatedAt()).isNotNull();
                        assertThat(foundOrder.getUpdatedAt()).isNotNull();
                        assertThat(foundOrder.getPaidAt()).isNotNull();
                        assertThat(foundOrder.getCompletedAt()).isNotNull();
                        assertThat(foundOrder.getMetadata()).containsEntry("source", "test");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should preserve line item details after save and retrieve")
        void shouldPreserveLineItemDetails() {
            // Given
            Order order = createTestOrderWithMultipleItems("user-lineitem");
            Order savedOrder = orderRepository.save(order).block();
            assertThat(savedOrder).isNotNull();

            // When
            var result = orderRepository.findById(savedOrder.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(foundOrder -> {
                        assertThat(foundOrder.getLineItems()).hasSize(2);

                        OrderLineItem item1 = foundOrder.getLineItems().stream()
                                .filter(i -> i.getProductId().equals("PROD-001"))
                                .findFirst().orElseThrow();
                        assertThat(item1.getProductName()).isEqualTo("Product 1");
                        assertThat(item1.getQuantity()).isEqualTo(2);
                        assertThat(item1.getUnitPrice().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(10000));
                        assertThat(item1.getTotalPrice().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(20000));

                        OrderLineItem item2 = foundOrder.getLineItems().stream()
                                .filter(i -> i.getProductId().equals("PROD-002"))
                                .findFirst().orElseThrow();
                        assertThat(item2.getProductName()).isEqualTo("Product 2");
                        assertThat(item2.getQuantity()).isEqualTo(1);
                    })
                    .verifyComplete();
        }
    }
}
