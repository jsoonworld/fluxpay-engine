package com.fluxpay.engine.presentation.api;

import com.fluxpay.engine.domain.exception.InvalidRefundException;
import com.fluxpay.engine.domain.exception.RefundNotFoundException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyResult;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import com.fluxpay.engine.domain.model.refund.RefundId;
import com.fluxpay.engine.domain.model.refund.RefundStatus;
import com.fluxpay.engine.domain.service.RefundService;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyService;
import com.fluxpay.engine.infrastructure.tenant.TenantWebFilter;
import com.fluxpay.engine.presentation.dto.request.CreateRefundRequest;
import com.fluxpay.engine.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RefundController.
 * Tests all refund API endpoints using WebFluxTest.
 */
@WebFluxTest(RefundController.class)
@Import({GlobalExceptionHandler.class, TenantWebFilter.class})
@TestPropertySource(properties = "fluxpay.tenant.enabled=true")
class RefundControllerTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TEST_TENANT_ID = "test-tenant";
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String TEST_IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RefundService refundService;

    @MockBean
    private IdempotencyService idempotencyService;

    private static final String BASE_URL = "/api/v1/refunds";
    private static final UUID TEST_PAYMENT_ID = UUID.randomUUID();
    private static final String TEST_REFUND_ID = "ref_" + UUID.randomUUID().toString().replace("-", "");

    private Refund testRefund;

    @BeforeEach
    void setUp() {
        testRefund = Refund.restore(
            RefundId.of(TEST_REFUND_ID),
            PaymentId.of(TEST_PAYMENT_ID),
            Money.of(BigDecimal.valueOf(5000), Currency.KRW),
            RefundStatus.REQUESTED,
            "Customer request",
            null,
            null,
            Instant.now(),
            null
        );

        // Configure IdempotencyService mock to always return MISS (pass through)
        when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(IdempotencyResult.miss()));
        when(idempotencyService.store(any(IdempotencyKey.class), anyString(), anyString(), anyInt(), any(Duration.class)))
            .thenReturn(Mono.empty());
        when(idempotencyService.releaseLock(any(IdempotencyKey.class)))
            .thenReturn(Mono.empty());
    }

    @Nested
    @DisplayName("POST /api/v1/refunds - Create Refund")
    class CreateRefundTests {

        @Test
        @DisplayName("should create refund with valid request and return 201")
        void shouldCreateRefund_withValidRequest_returns201() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                TEST_PAYMENT_ID.toString(),
                BigDecimal.valueOf(5000),
                "KRW",
                "Customer request"
            );

            when(refundService.createRefund(any(PaymentId.class), any(Money.class), anyString()))
                .thenReturn(Mono.just(testRefund));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.refundId").isEqualTo(TEST_REFUND_ID)
                .jsonPath("$.data.paymentId").isEqualTo(TEST_PAYMENT_ID.toString())
                .jsonPath("$.data.status").isEqualTo("REQUESTED")
                .jsonPath("$.data.amount").isEqualTo(5000)
                .jsonPath("$.data.currency").isEqualTo("KRW")
                .jsonPath("$.data.reason").isEqualTo("Customer request")
                .jsonPath("$.error").doesNotExist();
        }

        @Test
        @DisplayName("should create refund without reason and return 201")
        void shouldCreateRefund_withoutReason_returns201() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                TEST_PAYMENT_ID.toString(),
                BigDecimal.valueOf(5000),
                "KRW",
                null
            );

            Refund refundWithoutReason = Refund.restore(
                RefundId.of(TEST_REFUND_ID),
                PaymentId.of(TEST_PAYMENT_ID),
                Money.of(BigDecimal.valueOf(5000), Currency.KRW),
                RefundStatus.REQUESTED,
                null,
                null,
                null,
                Instant.now(),
                null
            );

            when(refundService.createRefund(any(PaymentId.class), any(Money.class), any()))
                .thenReturn(Mono.just(refundWithoutReason));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.refundId").isEqualTo(TEST_REFUND_ID)
                .jsonPath("$.data.reason").doesNotExist();
        }

        @Test
        @DisplayName("should fail with 400 when amount is zero")
        void shouldCreateRefund_withInvalidRequest_returns400() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                TEST_PAYMENT_ID.toString(),
                BigDecimal.ZERO,
                "KRW",
                "Customer request"
            );

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("VAL_001");
        }

        @Test
        @DisplayName("should fail with 400 when payment ID is blank")
        void shouldFailWithBlankPaymentId() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                "",
                BigDecimal.valueOf(5000),
                "KRW",
                "Customer request"
            );

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
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
            CreateRefundRequest request = new CreateRefundRequest(
                TEST_PAYMENT_ID.toString(),
                BigDecimal.valueOf(5000),
                "INVALID",
                "Customer request"
            );

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("VAL_001");
        }

        @Test
        @DisplayName("should fail with 400 when payment ID is invalid format")
        void shouldFailWithInvalidPaymentIdFormat() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                "not-a-uuid",
                BigDecimal.valueOf(5000),
                "KRW",
                "Customer request"
            );

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("VAL_001");
        }

        @Test
        @DisplayName("should return same result with duplicate idempotency key")
        void shouldCreateRefund_withDuplicateIdempotencyKey_returnsSameResult() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                TEST_PAYMENT_ID.toString(),
                BigDecimal.valueOf(5000),
                "KRW",
                "Customer request"
            );

            // Mock idempotency service to return HIT (already processed)
            String cachedResponse = """
                {"success":true,"data":{"refundId":"%s","paymentId":"%s","amount":5000,"currency":"KRW","status":"REQUESTED","reason":"Customer request","pgRefundId":null,"errorMessage":null,"requestedAt":"2026-02-06T10:00:00Z","completedAt":null},"error":null,"metadata":{"timestamp":"2026-02-06T10:00:00Z","traceId":"test-trace"}}
                """.formatted(TEST_REFUND_ID, TEST_PAYMENT_ID);

            when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(IdempotencyResult.hit(cachedResponse, 201)));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.refundId").isEqualTo(TEST_REFUND_ID);
        }

        @Test
        @DisplayName("should produce SHA-256 payload hash for idempotency")
        void shouldProduceSha256PayloadHash() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                TEST_PAYMENT_ID.toString(),
                BigDecimal.valueOf(5000),
                "KRW",
                "Customer request"
            );

            when(refundService.createRefund(any(PaymentId.class), any(Money.class), anyString()))
                .thenReturn(Mono.just(testRefund));

            // When
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

            // Then - verify the payload hash passed to acquireLock is a 64-char hex string (SHA-256)
            verify(idempotencyService).acquireLock(
                any(IdempotencyKey.class),
                argThat(hash -> hash != null && hash.matches("^[0-9a-f]{64}$")),
                any(Duration.class));
        }

        @Test
        @DisplayName("should fail with 400 when refund exceeds payment amount")
        void shouldFailWhenRefundExceedsPaymentAmount() {
            // Given
            CreateRefundRequest request = new CreateRefundRequest(
                TEST_PAYMENT_ID.toString(),
                BigDecimal.valueOf(100000),
                "KRW",
                "Customer request"
            );

            when(refundService.createRefund(any(PaymentId.class), any(Money.class), anyString()))
                .thenReturn(Mono.error(new InvalidRefundException(
                    PaymentId.of(TEST_PAYMENT_ID),
                    "Refund amount exceeds refundable amount",
                    "PAY_007")));

            // When & Then
            webTestClient.post()
                .uri(BASE_URL)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("PAY_007");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/refunds/{refundId} - Get Refund")
    class GetRefundTests {

        @Test
        @DisplayName("should get refund with existing ID and return 200")
        void shouldGetRefund_withExistingId_returns200() {
            // Given
            when(refundService.getRefund(any(RefundId.class)))
                .thenReturn(Mono.just(testRefund));

            // When & Then
            webTestClient.get()
                .uri(BASE_URL + "/" + TEST_REFUND_ID)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.refundId").isEqualTo(TEST_REFUND_ID)
                .jsonPath("$.data.paymentId").isEqualTo(TEST_PAYMENT_ID.toString())
                .jsonPath("$.data.status").isEqualTo("REQUESTED")
                .jsonPath("$.data.amount").isEqualTo(5000)
                .jsonPath("$.data.currency").isEqualTo("KRW");
        }

        @Test
        @DisplayName("should return 404 when refund not found")
        void shouldGetRefund_withNonExistingId_returns404() {
            // Given
            String nonExistentId = "ref_" + UUID.randomUUID().toString().replace("-", "");
            when(refundService.getRefund(any(RefundId.class)))
                .thenReturn(Mono.error(new RefundNotFoundException(RefundId.of(nonExistentId))));

            // When & Then
            webTestClient.get()
                .uri(BASE_URL + "/" + nonExistentId)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("PAY_009");
        }

        @Test
        @DisplayName("should return completed refund with all fields")
        void shouldGetCompletedRefund() {
            // Given
            Refund completedRefund = Refund.restore(
                RefundId.of(TEST_REFUND_ID),
                PaymentId.of(TEST_PAYMENT_ID),
                Money.of(BigDecimal.valueOf(5000), Currency.KRW),
                RefundStatus.COMPLETED,
                "Customer request",
                "pg-refund-123",
                null,
                Instant.now().minusSeconds(3600),
                Instant.now()
            );

            when(refundService.getRefund(any(RefundId.class)))
                .thenReturn(Mono.just(completedRefund));

            // When & Then
            webTestClient.get()
                .uri(BASE_URL + "/" + TEST_REFUND_ID)
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("COMPLETED")
                .jsonPath("$.data.pgRefundId").isEqualTo("pg-refund-123")
                .jsonPath("$.data.completedAt").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{paymentId}/refunds - Get Refunds by Payment")
    class GetRefundsByPaymentTests {

        @Test
        @DisplayName("should get refunds for payment and return 200 with list")
        void shouldGetRefundsByPayment_returns200WithList() {
            // Given
            Refund refund1 = Refund.restore(
                RefundId.of("ref_" + UUID.randomUUID().toString().replace("-", "")),
                PaymentId.of(TEST_PAYMENT_ID),
                Money.of(BigDecimal.valueOf(3000), Currency.KRW),
                RefundStatus.COMPLETED,
                "Partial refund 1",
                "pg-refund-001",
                null,
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(7000)
            );

            Refund refund2 = Refund.restore(
                RefundId.of("ref_" + UUID.randomUUID().toString().replace("-", "")),
                PaymentId.of(TEST_PAYMENT_ID),
                Money.of(BigDecimal.valueOf(2000), Currency.KRW),
                RefundStatus.REQUESTED,
                "Partial refund 2",
                null,
                null,
                Instant.now(),
                null
            );

            when(refundService.getRefundsByPayment(any(PaymentId.class)))
                .thenReturn(Flux.just(refund1, refund2));

            // When & Then
            webTestClient.get()
                .uri("/api/v1/payments/" + TEST_PAYMENT_ID + "/refunds")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].paymentId").isEqualTo(TEST_PAYMENT_ID.toString())
                .jsonPath("$.data[1].paymentId").isEqualTo(TEST_PAYMENT_ID.toString());
        }

        @Test
        @DisplayName("should return empty list when no refunds exist")
        void shouldReturnEmptyListWhenNoRefunds() {
            // Given
            when(refundService.getRefundsByPayment(any(PaymentId.class)))
                .thenReturn(Flux.empty());

            // When & Then
            webTestClient.get()
                .uri("/api/v1/payments/" + TEST_PAYMENT_ID + "/refunds")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("should return 400 when payment ID has invalid format")
        void shouldReturn400WhenInvalidPaymentIdFormat() {
            // When & Then
            webTestClient.get()
                .uri("/api/v1/payments/invalid-uuid/refunds")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
        }
    }
}
