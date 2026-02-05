package com.fluxpay.engine.infrastructure.persistence.adapter;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.domain.port.outbound.OrderRepository;
import com.fluxpay.engine.domain.port.outbound.PaymentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for R2dbcPaymentRepository.
 * Uses Testcontainers to spin up a PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("R2dbcPaymentRepository Integration Tests")
class R2dbcPaymentRepositoryIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        // Delete payments first due to FK constraint
        databaseClient.sql("DELETE FROM payments").then().block();
        databaseClient.sql("DELETE FROM order_line_items").then().block();
        databaseClient.sql("DELETE FROM orders").then().block();
    }

    /**
     * Creates and saves a test order for use as FK reference in payments.
     */
    private Order createAndSaveOrder(String userId) {
        OrderLineItem lineItem = OrderLineItem.create(
                "PROD-001",
                "Test Product",
                1,
                Money.of(BigDecimal.valueOf(10000), Currency.KRW)
        );

        Order order = Order.create(
                userId,
                List.of(lineItem),
                Currency.KRW,
                null
        );

        return orderRepository.save(order).block();
    }

    private Payment createTestPayment(OrderId orderId, Money amount) {
        return Payment.create(orderId, amount);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should save new payment")
        void shouldSaveNewPayment() {
            // Given
            Order order = createAndSaveOrder("user-save-test");
            Payment payment = createTestPayment(order.getId(), Money.krw(10000));

            // When
            var result = paymentRepository.save(payment);

            // Then
            StepVerifier.create(result)
                    .assertNext(savedPayment -> {
                        assertThat(savedPayment.getId()).isNotNull();
                        assertThat(savedPayment.getOrderId()).isEqualTo(order.getId());
                        assertThat(savedPayment.getAmount().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(10000));
                        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.READY);
                        assertThat(savedPayment.getCreatedAt()).isNotNull();
                        assertThat(savedPayment.getUpdatedAt()).isNotNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should update existing payment")
        void shouldUpdateExistingPayment() {
            // Given
            Order order = createAndSaveOrder("user-update-test");
            Payment payment = createTestPayment(order.getId(), Money.krw(20000));
            Payment savedPayment = paymentRepository.save(payment).block();
            assertThat(savedPayment).isNotNull();

            // Start processing with a payment method
            savedPayment.startProcessing(PaymentMethod.card("Visa ending 1234"));

            // When
            var result = paymentRepository.save(savedPayment);

            // Then
            StepVerifier.create(result)
                    .assertNext(updatedPayment -> {
                        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
                        assertThat(updatedPayment.getPaymentMethod()).isNotNull();
                        assertThat(updatedPayment.getPaymentMethod().displayName())
                                .isEqualTo("Visa ending 1234");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should persist all payment fields through state transitions")
        void shouldPersistAllPaymentFieldsThroughStateTransitions() {
            // Given
            Order order = createAndSaveOrder("user-full-lifecycle");
            Payment payment = createTestPayment(order.getId(), Money.krw(50000));
            Payment savedPayment = paymentRepository.save(payment).block();
            assertThat(savedPayment).isNotNull();

            // Progress through payment lifecycle
            savedPayment.startProcessing(PaymentMethod.bankTransfer("KB Bank"));
            savedPayment = paymentRepository.save(savedPayment).block();
            assertThat(savedPayment).isNotNull();

            savedPayment.approve("txn-123456", "pk-789");
            savedPayment = paymentRepository.save(savedPayment).block();
            assertThat(savedPayment).isNotNull();

            savedPayment.confirm();
            savedPayment = paymentRepository.save(savedPayment).block();
            assertThat(savedPayment).isNotNull();

            // When - retrieve from database
            var result = paymentRepository.findById(savedPayment.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(found -> {
                        assertThat(found.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
                        assertThat(found.getPgTransactionId()).isEqualTo("txn-123456");
                        assertThat(found.getPgPaymentKey()).isEqualTo("pk-789");
                        assertThat(found.getApprovedAt()).isNotNull();
                        assertThat(found.getConfirmedAt()).isNotNull();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should find payment by id")
        void shouldFindPaymentById() {
            // Given
            Order order = createAndSaveOrder("user-findbyid");
            Payment payment = createTestPayment(order.getId(), Money.krw(15000));
            Payment savedPayment = paymentRepository.save(payment).block();
            assertThat(savedPayment).isNotNull();

            // When
            var result = paymentRepository.findById(savedPayment.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(found -> {
                        assertThat(found.getId()).isEqualTo(savedPayment.getId());
                        assertThat(found.getOrderId()).isEqualTo(order.getId());
                        assertThat(found.getAmount().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(15000));
                        assertThat(found.getStatus()).isEqualTo(PaymentStatus.READY);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty when payment not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            PaymentId nonExistentId = PaymentId.generate();

            // When
            var result = paymentRepository.findById(nonExistentId);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByOrderId")
    class FindByOrderId {

        @Test
        @DisplayName("should find payment by order id")
        void shouldFindByOrderId() {
            // Given
            Order order = createAndSaveOrder("user-findbyorderid");
            Payment payment = createTestPayment(order.getId(), Money.krw(25000));
            paymentRepository.save(payment).block();

            // When
            var result = paymentRepository.findByOrderId(order.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(found -> {
                        assertThat(found.getOrderId()).isEqualTo(order.getId());
                        assertThat(found.getAmount().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(25000));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty when no payment for order")
        void shouldReturnEmptyWhenNoPaymentForOrder() {
            // Given
            Order order = createAndSaveOrder("user-no-payment");
            // No payment created for this order

            // When
            var result = paymentRepository.findByOrderId(order.getId());

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("should find payments by status")
        void shouldFindByStatus() {
            // Given
            Order order1 = createAndSaveOrder("user-status-1");
            Order order2 = createAndSaveOrder("user-status-2");
            Order order3 = createAndSaveOrder("user-status-3");

            // Create payments with different statuses
            Payment readyPayment1 = createTestPayment(order1.getId(), Money.krw(10000));
            Payment readyPayment2 = createTestPayment(order2.getId(), Money.krw(20000));
            Payment processingPayment = createTestPayment(order3.getId(), Money.krw(30000));

            paymentRepository.save(readyPayment1).block();
            paymentRepository.save(readyPayment2).block();

            // Start processing the third payment
            processingPayment.startProcessing(PaymentMethod.card());
            paymentRepository.save(processingPayment).block();

            // When - find READY payments
            var result = paymentRepository.findByStatus(PaymentStatus.READY);

            // Then
            StepVerifier.create(result.collectList())
                    .assertNext(payments -> {
                        assertThat(payments).hasSize(2);
                        assertThat(payments).allMatch(p -> p.getStatus() == PaymentStatus.READY);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty flux when no payments with status")
        void shouldReturnEmptyFluxWhenNoPaymentsWithStatus() {
            // Given
            Order order = createAndSaveOrder("user-no-confirmed");
            Payment payment = createTestPayment(order.getId(), Money.krw(10000));
            paymentRepository.save(payment).block();

            // When - find CONFIRMED payments (none exist)
            var result = paymentRepository.findByStatus(PaymentStatus.CONFIRMED);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByPgPaymentKey")
    class FindByPgPaymentKey {

        @Test
        @DisplayName("should find payment by pg payment key")
        void shouldFindByPgPaymentKey() {
            // Given
            Order order = createAndSaveOrder("user-pgkey");
            Payment payment = createTestPayment(order.getId(), Money.krw(35000));
            payment.startProcessing(PaymentMethod.card());
            payment.approve("txn-abc", "unique-pg-key-123");
            paymentRepository.save(payment).block();

            // When
            var result = paymentRepository.findByPgPaymentKey("unique-pg-key-123");

            // Then
            StepVerifier.create(result)
                    .assertNext(found -> {
                        assertThat(found.getPgPaymentKey()).isEqualTo("unique-pg-key-123");
                        assertThat(found.getPgTransactionId()).isEqualTo("txn-abc");
                        assertThat(found.getStatus()).isEqualTo(PaymentStatus.APPROVED);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty when pg payment key not found")
        void shouldReturnEmptyWhenPgPaymentKeyNotFound() {
            // When
            var result = paymentRepository.findByPgPaymentKey("non-existent-key");

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("should return true when payment exists")
        void shouldReturnTrueWhenExists() {
            // Given
            Order order = createAndSaveOrder("user-exists-true");
            Payment payment = createTestPayment(order.getId(), Money.krw(5000));
            Payment savedPayment = paymentRepository.save(payment).block();
            assertThat(savedPayment).isNotNull();

            // When
            var result = paymentRepository.existsById(savedPayment.getId());

            // Then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return false when payment does not exist")
        void shouldReturnFalseWhenNotExists() {
            // Given
            PaymentId nonExistentId = PaymentId.generate();

            // When
            var result = paymentRepository.existsById(nonExistentId);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    class DataIntegrity {

        @Test
        @DisplayName("should preserve all payment fields after save and retrieve")
        void shouldPreserveAllFields() {
            // Given
            Order order = createAndSaveOrder("user-integrity");
            Payment payment = createTestPayment(order.getId(), Money.krw(100000));

            // Progress through entire lifecycle
            payment.startProcessing(PaymentMethod.easyPay("Kakao Pay"));
            payment.approve("pg-txn-integrity-test", "pg-key-integrity");
            payment.confirm();

            Payment savedPayment = paymentRepository.save(payment).block();
            assertThat(savedPayment).isNotNull();

            // When
            var result = paymentRepository.findById(savedPayment.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(found -> {
                        assertThat(found.getId()).isEqualTo(savedPayment.getId());
                        assertThat(found.getOrderId()).isEqualTo(order.getId());
                        assertThat(found.getAmount().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(100000));
                        assertThat(found.getAmount().currency()).isEqualTo(Currency.KRW);
                        assertThat(found.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
                        assertThat(found.getPaymentMethod()).isNotNull();
                        assertThat(found.getPaymentMethod().displayName()).isEqualTo("Kakao Pay");
                        assertThat(found.getPgTransactionId()).isEqualTo("pg-txn-integrity-test");
                        assertThat(found.getPgPaymentKey()).isEqualTo("pg-key-integrity");
                        assertThat(found.getCreatedAt()).isNotNull();
                        assertThat(found.getUpdatedAt()).isNotNull();
                        assertThat(found.getApprovedAt()).isNotNull();
                        assertThat(found.getConfirmedAt()).isNotNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should preserve failed payment details")
        void shouldPreserveFailedPaymentDetails() {
            // Given
            Order order = createAndSaveOrder("user-failed");
            Payment payment = createTestPayment(order.getId(), Money.krw(50000));
            payment.startProcessing(PaymentMethod.card("Card"));
            payment.fail("Insufficient funds");
            paymentRepository.save(payment).block();

            // When
            var result = paymentRepository.findById(payment.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(found -> {
                        assertThat(found.getStatus()).isEqualTo(PaymentStatus.FAILED);
                        assertThat(found.getFailureReason()).isEqualTo("Insufficient funds");
                        assertThat(found.getFailedAt()).isNotNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle payment with different currencies")
        void shouldHandlePaymentWithDifferentCurrencies() {
            // Given - create order with USD
            OrderLineItem usdLineItem = OrderLineItem.create(
                    "PROD-USD",
                    "USD Product",
                    1,
                    Money.of(BigDecimal.valueOf(100), Currency.USD)
            );
            Order usdOrder = Order.create("user-usd", List.of(usdLineItem), Currency.USD, null);
            usdOrder = orderRepository.save(usdOrder).block();
            assertThat(usdOrder).isNotNull();

            Payment usdPayment = Payment.create(usdOrder.getId(), Money.of(BigDecimal.valueOf(100), Currency.USD));
            paymentRepository.save(usdPayment).block();

            // When
            var result = paymentRepository.findById(usdPayment.getId());

            // Then
            StepVerifier.create(result)
                    .assertNext(found -> {
                        assertThat(found.getAmount().currency()).isEqualTo(Currency.USD);
                        assertThat(found.getAmount().amount())
                                .isEqualByComparingTo(BigDecimal.valueOf(100));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentOperations {

        @Test
        @DisplayName("should handle multiple payments for different orders")
        void shouldHandleMultiplePaymentsForDifferentOrders() {
            // Given
            Order order1 = createAndSaveOrder("user-concurrent-1");
            Order order2 = createAndSaveOrder("user-concurrent-2");
            Order order3 = createAndSaveOrder("user-concurrent-3");

            Payment payment1 = createTestPayment(order1.getId(), Money.krw(10000));
            Payment payment2 = createTestPayment(order2.getId(), Money.krw(20000));
            Payment payment3 = createTestPayment(order3.getId(), Money.krw(30000));

            // When - save all payments
            paymentRepository.save(payment1).block();
            paymentRepository.save(payment2).block();
            paymentRepository.save(payment3).block();

            // Then - verify each can be found by order ID
            StepVerifier.create(paymentRepository.findByOrderId(order1.getId()))
                    .assertNext(found -> assertThat(found.getAmount().amount())
                            .isEqualByComparingTo(BigDecimal.valueOf(10000)))
                    .verifyComplete();

            StepVerifier.create(paymentRepository.findByOrderId(order2.getId()))
                    .assertNext(found -> assertThat(found.getAmount().amount())
                            .isEqualByComparingTo(BigDecimal.valueOf(20000)))
                    .verifyComplete();

            StepVerifier.create(paymentRepository.findByOrderId(order3.getId()))
                    .assertNext(found -> assertThat(found.getAmount().amount())
                            .isEqualByComparingTo(BigDecimal.valueOf(30000)))
                    .verifyComplete();
        }
    }
}
