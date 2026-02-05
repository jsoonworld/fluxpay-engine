package com.fluxpay.engine.presentation.api;

import com.fluxpay.engine.domain.exception.InvalidPaymentStateException;
import com.fluxpay.engine.domain.exception.PaymentNotFoundException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.domain.service.PaymentService;
import com.fluxpay.engine.presentation.dto.request.ApprovePaymentRequest;
import com.fluxpay.engine.presentation.dto.request.CreatePaymentRequest;
import com.fluxpay.engine.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentController.
 * Tests all payment API endpoints using WebFluxTest.
 */
@WebFluxTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TEST_TENANT_ID = "test-tenant";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    private static final String BASE_URL = "/api/v1/payments";
    private static final UUID TEST_PAYMENT_ID = UUID.randomUUID();
    private static final UUID TEST_ORDER_ID = UUID.randomUUID();

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = Payment.restore(
            PaymentId.of(TEST_PAYMENT_ID),
            OrderId.of(TEST_ORDER_ID),
            Money.of(BigDecimal.valueOf(10000), Currency.KRW),
            PaymentStatus.READY,
            null,
            null,
            null,
            null,
            Instant.now(),
            Instant.now(),
            null,
            null,
            null
        );
    }

    @Nested
    @DisplayName("POST /api/v1/payments - Create Payment")
    class CreatePaymentTests {

        @Test
        @DisplayName("should create payment with valid request and return 201")
        void shouldCreatePayment() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                TEST_ORDER_ID.toString(),
                BigDecimal.valueOf(10000),
                "KRW"
            );

            when(paymentService.createPayment(any(OrderId.class), any(Money.class)))
                .thenReturn(Mono.just(testPayment));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.paymentId").isEqualTo(TEST_PAYMENT_ID.toString())
                .jsonPath("$.data.orderId").isEqualTo(TEST_ORDER_ID.toString())
                .jsonPath("$.data.status").isEqualTo("READY")
                .jsonPath("$.data.amount").isEqualTo(10000)
                .jsonPath("$.data.currency").isEqualTo("KRW")
                .jsonPath("$.error").doesNotExist();
        }

        @Test
        @DisplayName("should fail with 400 when amount is zero")
        void shouldFailWithInvalidAmount() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                TEST_ORDER_ID.toString(),
                BigDecimal.ZERO,
                "KRW"
            );

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("VAL_001");
        }

        @Test
        @DisplayName("should fail with 400 when orderId is blank")
        void shouldFailWithBlankOrderId() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "",
                BigDecimal.valueOf(10000),
                "KRW"
            );

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("VAL_001");
        }

        @Test
        @DisplayName("should fail with 400 when currency is invalid")
        void shouldFailWithInvalidCurrency() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                TEST_ORDER_ID.toString(),
                BigDecimal.valueOf(10000),
                "INVALID"
            );

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("VAL_001");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{paymentId}/approve - Approve Payment")
    class ApprovePaymentTests {

        @Test
        @DisplayName("should approve payment with valid request and return 200")
        void shouldApprovePayment() {
            // Given
            ApprovePaymentRequest request = new ApprovePaymentRequest("CARD", null);

            Payment approvedPayment = Payment.restore(
                PaymentId.of(TEST_PAYMENT_ID),
                OrderId.of(TEST_ORDER_ID),
                Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                PaymentStatus.APPROVED,
                PaymentMethod.card(),
                "pg-txn-123",
                "payment-key-123",
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null,
                null
            );

            when(paymentService.requestApproval(any(PaymentId.class), any(PaymentMethod.class)))
                .thenReturn(Mono.just(approvedPayment));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID + "/approve")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.paymentId").isEqualTo(TEST_PAYMENT_ID.toString())
                .jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.paymentMethod").isEqualTo("CARD")
                .jsonPath("$.data.pgTransactionId").isEqualTo("pg-txn-123");
        }

        @Test
        @DisplayName("should approve payment with EASY_PAY method and provider")
        void shouldApprovePaymentWithEasyPay() {
            // Given
            ApprovePaymentRequest request = new ApprovePaymentRequest("EASY_PAY", "KAKAO_PAY");

            Payment approvedPayment = Payment.restore(
                PaymentId.of(TEST_PAYMENT_ID),
                OrderId.of(TEST_ORDER_ID),
                Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                PaymentStatus.APPROVED,
                PaymentMethod.easyPay("KAKAO_PAY"),
                "pg-txn-123",
                "payment-key-123",
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null,
                null
            );

            when(paymentService.requestApproval(any(PaymentId.class), any(PaymentMethod.class)))
                .thenReturn(Mono.just(approvedPayment));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID + "/approve")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.paymentMethod").isEqualTo("EASY_PAY");
        }

        @Test
        @DisplayName("should fail with 404 when payment not found")
        void shouldFailWithPaymentNotFound() {
            // Given
            ApprovePaymentRequest request = new ApprovePaymentRequest("CARD", null);

            when(paymentService.requestApproval(any(PaymentId.class), any(PaymentMethod.class)))
                .thenReturn(Mono.error(new PaymentNotFoundException(PaymentId.of(TEST_PAYMENT_ID))));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID + "/approve")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("PAY_001");
        }

        @Test
        @DisplayName("should fail with 400 when payment method is invalid")
        void shouldFailWithInvalidPaymentMethod() {
            // Given
            ApprovePaymentRequest request = new ApprovePaymentRequest("INVALID", null);

            // When & Then
            webTestClient.post()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID + "/approve")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("VAL_001");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{paymentId}/confirm - Confirm Payment")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("should confirm payment and return 200")
        void shouldConfirmPayment() {
            // Given
            Payment confirmedPayment = Payment.restore(
                PaymentId.of(TEST_PAYMENT_ID),
                OrderId.of(TEST_ORDER_ID),
                Money.of(BigDecimal.valueOf(10000), Currency.KRW),
                PaymentStatus.CONFIRMED,
                PaymentMethod.card(),
                "pg-txn-123",
                "payment-key-123",
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null
            );

            when(paymentService.confirmPayment(any(PaymentId.class)))
                .thenReturn(Mono.just(confirmedPayment));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID + "/confirm")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.paymentId").isEqualTo(TEST_PAYMENT_ID.toString())
                .jsonPath("$.data.status").isEqualTo("CONFIRMED")
                .jsonPath("$.data.confirmedAt").isNotEmpty();
        }

        @Test
        @DisplayName("should fail with 404 when payment not found")
        void shouldFailWithPaymentNotFound() {
            // Given
            when(paymentService.confirmPayment(any(PaymentId.class)))
                .thenReturn(Mono.error(new PaymentNotFoundException(PaymentId.of(TEST_PAYMENT_ID))));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID + "/confirm")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("PAY_001");
        }

        @Test
        @DisplayName("should fail with 400 when payment is in invalid state")
        void shouldReturn400WhenInvalidPaymentState() {
            // Given
            when(paymentService.confirmPayment(any(PaymentId.class)))
                .thenReturn(Mono.error(new InvalidPaymentStateException(
                    PaymentStatus.READY, PaymentStatus.CONFIRMED)));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID + "/confirm")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("PAY_006");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{paymentId} - Get Payment")
    class GetPaymentTests {

        @Test
        @DisplayName("should get payment by ID and return 200")
        void shouldGetPayment() {
            // Given
            when(paymentService.getPayment(any(PaymentId.class)))
                .thenReturn(Mono.just(testPayment));

            // When & Then
            webTestClient.get()
                .uri(BASE_URL + "/" + TEST_PAYMENT_ID)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.paymentId").isEqualTo(TEST_PAYMENT_ID.toString())
                .jsonPath("$.data.orderId").isEqualTo(TEST_ORDER_ID.toString())
                .jsonPath("$.data.status").isEqualTo("READY")
                .jsonPath("$.data.amount").isEqualTo(10000)
                .jsonPath("$.data.currency").isEqualTo("KRW");
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(paymentService.getPayment(any(PaymentId.class)))
                .thenReturn(Mono.error(new PaymentNotFoundException(PaymentId.of(nonExistentId))));

            // When & Then
            webTestClient.get()
                .uri(BASE_URL + "/" + nonExistentId)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("PAY_001");
        }

        @Test
        @DisplayName("should return 400 when payment ID has invalid format")
        void shouldReturn400WhenInvalidPaymentIdFormat() {
            // When & Then
            webTestClient.get()
                .uri(BASE_URL + "/invalid-uuid")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
        }
    }
}
