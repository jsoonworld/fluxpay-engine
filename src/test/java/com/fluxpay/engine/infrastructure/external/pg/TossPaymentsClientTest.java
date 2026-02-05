package com.fluxpay.engine.infrastructure.external.pg;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class TossPaymentsClientTest {

    private static WireMockServer wireMockServer;
    private TossPaymentsClient client;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        client = new TossPaymentsClient(webClient);
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("Should confirm payment successfully")
    void shouldConfirmPaymentSuccessfully() {
        // Given
        String paymentKey = "toss_pk_test123";
        String orderId = "order_123";
        Money amount = Money.krw(10000);

        stubFor(post(urlEqualTo("/payments/confirm"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentKey": "toss_pk_test123",
                        "orderId": "order_123",
                        "status": "DONE",
                        "totalAmount": 10000,
                        "method": "카드",
                        "transactionKey": "tx_abc123",
                        "approvedAt": "2026-02-04T10:30:00+09:00"
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.confirmPayment(paymentKey, orderId, amount))
            .assertNext(result -> {
                Assertions.assertTrue(result.success());
                Assertions.assertEquals("tx_abc123", result.transactionId());
                Assertions.assertNull(result.errorMessage());
            })
            .verifyComplete();

        verify(postRequestedFor(urlEqualTo("/payments/confirm"))
            .withRequestBody(containing("\"paymentKey\":\"toss_pk_test123\""))
            .withRequestBody(containing("\"orderId\":\"order_123\"")));
    }

    @Test
    @DisplayName("Should handle amount mismatch error")
    void shouldHandleAmountMismatchError() {
        // Given
        stubFor(post(urlEqualTo("/payments/confirm"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "code": "INVALID_AMOUNT",
                        "message": "결제 금액이 일치하지 않습니다."
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.confirmPayment("pk", "order", Money.krw(10000)))
            .assertNext(result -> {
                Assertions.assertFalse(result.success());
                Assertions.assertNull(result.transactionId());
                Assertions.assertTrue(result.errorMessage().contains("INVALID_AMOUNT"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle payment not in DONE status")
    void shouldHandlePaymentNotDoneStatus() {
        // Given
        stubFor(post(urlEqualTo("/payments/confirm"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentKey": "toss_pk_test123",
                        "orderId": "order_123",
                        "status": "IN_PROGRESS",
                        "totalAmount": 10000,
                        "method": "카드",
                        "transactionKey": null,
                        "approvedAt": null
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.confirmPayment("toss_pk_test123", "order_123", Money.krw(10000)))
            .assertNext(result -> {
                Assertions.assertFalse(result.success());
                Assertions.assertNull(result.transactionId());
                Assertions.assertTrue(result.errorMessage().contains("IN_PROGRESS"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should cancel payment successfully")
    void shouldCancelPaymentSuccessfully() {
        // Given
        String paymentKey = "toss_pk_test123";
        String reason = "Customer request";

        stubFor(post(urlEqualTo("/payments/toss_pk_test123/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentKey": "toss_pk_test123",
                        "orderId": "order_123",
                        "status": "CANCELED",
                        "cancels": [
                            {
                                "cancelAmount": 10000,
                                "cancelReason": "Customer request",
                                "canceledAt": "2026-02-04T11:00:00+09:00",
                                "transactionKey": "tx_cancel_123"
                            }
                        ]
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.cancelPayment(paymentKey, reason))
            .assertNext(result -> {
                Assertions.assertTrue(result.success());
                Assertions.assertEquals("tx_cancel_123", result.transactionId());
            })
            .verifyComplete();

        verify(postRequestedFor(urlEqualTo("/payments/toss_pk_test123/cancel"))
            .withRequestBody(containing("\"cancelReason\":\"Customer request\"")));
    }

    @Test
    @DisplayName("Should handle cancel not allowed error")
    void shouldHandleCancelNotAllowedError() {
        // Given
        stubFor(post(urlEqualTo("/payments/pk/cancel"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "code": "CANCEL_NOT_ALLOWED_PAYMENT",
                        "message": "취소할 수 없는 결제입니다."
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.cancelPayment("pk", "reason"))
            .assertNext(result -> {
                Assertions.assertFalse(result.success());
                Assertions.assertTrue(result.errorMessage().contains("CANCEL_NOT_ALLOWED"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle cancel response not in CANCELED status")
    void shouldHandleCancelNotCanceledStatus() {
        // Given
        stubFor(post(urlEqualTo("/payments/pk123/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentKey": "pk123",
                        "orderId": "order_123",
                        "status": "DONE",
                        "cancels": []
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.cancelPayment("pk123", "reason"))
            .assertNext(result -> {
                Assertions.assertFalse(result.success());
                Assertions.assertTrue(result.errorMessage().contains("DONE"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should request approval - mock response")
    void shouldRequestApproval() {
        // Given
        String orderId = "order_123";
        Money amount = Money.krw(10000);
        PaymentMethod method = PaymentMethod.card();

        // When & Then
        StepVerifier.create(client.requestApproval(orderId, amount, method))
            .assertNext(result -> {
                Assertions.assertTrue(result.success());
                Assertions.assertNotNull(result.paymentKey());
                Assertions.assertNotNull(result.transactionId());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle connection error during confirm")
    void shouldHandleConnectionErrorDuringConfirm() {
        // Given - simulate a connection reset
        stubFor(post(urlEqualTo("/payments/confirm"))
            .willReturn(aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        // When & Then
        StepVerifier.create(client.confirmPayment("pk", "order", Money.krw(10000)))
            .assertNext(result -> {
                Assertions.assertFalse(result.success());
                Assertions.assertNull(result.transactionId());
                Assertions.assertNotNull(result.errorMessage());
                // Error message will contain details about the connection issue
                // (either "Connection error", "Request error", or "Unexpected error")
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle connection error during cancel")
    void shouldHandleConnectionErrorDuringCancel() {
        // Given - simulate a connection reset
        stubFor(post(urlEqualTo("/payments/pk/cancel"))
            .willReturn(aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        // When & Then
        StepVerifier.create(client.cancelPayment("pk", "reason"))
            .assertNext(result -> {
                Assertions.assertFalse(result.success());
                Assertions.assertNull(result.transactionId());
                Assertions.assertNotNull(result.errorMessage());
                // Error message will contain details about the connection issue
                // (either "Connection error", "Request error", or "Unexpected error")
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle cancel response with null cancels list")
    void shouldHandleCancelWithNullCancelsList() {
        // Given
        stubFor(post(urlEqualTo("/payments/pk_null/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentKey": "pk_null",
                        "orderId": "order_123",
                        "status": "CANCELED",
                        "cancels": null
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.cancelPayment("pk_null", "reason"))
            .assertNext(result -> {
                Assertions.assertTrue(result.success());
                Assertions.assertNull(result.transactionId());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle cancel response with empty cancels list")
    void shouldHandleCancelWithEmptyCancelsList() {
        // Given
        stubFor(post(urlEqualTo("/payments/pk_empty/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentKey": "pk_empty",
                        "orderId": "order_123",
                        "status": "CANCELED",
                        "cancels": []
                    }
                    """)));

        // When & Then
        StepVerifier.create(client.cancelPayment("pk_empty", "reason"))
            .assertNext(result -> {
                Assertions.assertTrue(result.success());
                Assertions.assertNull(result.transactionId());
            })
            .verifyComplete();
    }
}
