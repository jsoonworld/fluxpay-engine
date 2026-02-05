package com.fluxpay.engine.presentation.api;

import com.fluxpay.engine.domain.exception.OrderNotFoundException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.service.OrderService;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyKey;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyResult;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyService;
import com.fluxpay.engine.infrastructure.tenant.TenantWebFilter;
import com.fluxpay.engine.presentation.dto.request.CreateOrderRequest;
import com.fluxpay.engine.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderController.
 * Tests all order API endpoints using WebFluxTest.
 */
@WebFluxTest(OrderController.class)
@Import({GlobalExceptionHandler.class, TenantWebFilter.class})
@org.springframework.test.context.TestPropertySource(properties = "fluxpay.tenant.enabled=true")
class OrderControllerTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TEST_TENANT_ID = "test-tenant";
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String TEST_IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @MockBean
    private IdempotencyService idempotencyService;

    private Order testOrder;
    private final String testUserId = "user-123";

    @BeforeEach
    void setUp() {
        OrderLineItem lineItem = OrderLineItem.create(
            "product-1",
            "Test Product",
            2,
            Money.of(BigDecimal.valueOf(10000), Currency.KRW)
        );
        testOrder = Order.create(
            testUserId,
            List.of(lineItem),
            Currency.KRW,
            Map.of("note", "test order")
        );

        // Configure IdempotencyService mock to always return MISS (pass through)
        when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
            .thenReturn(reactor.core.publisher.Mono.just(IdempotencyResult.miss()));
        when(idempotencyService.store(any(IdempotencyKey.class), anyString(), anyString(), anyInt(), any(Duration.class)))
            .thenReturn(reactor.core.publisher.Mono.empty());
        when(idempotencyService.releaseLock(any(IdempotencyKey.class)))
            .thenReturn(reactor.core.publisher.Mono.empty());
    }

    // ===== POST /api/v1/orders - Create Order Tests =====

    @Test
    @DisplayName("POST /api/v1/orders - Should create order with valid request and return 201")
    void shouldCreateOrder() {
        // Given
        when(orderService.createOrder(
            eq(testUserId),
            any(),
            eq(Currency.KRW),
            any()
        )).thenReturn(Mono.just(testOrder));

        CreateOrderRequest.LineItemRequest lineItemRequest =
            new CreateOrderRequest.LineItemRequest(
                "product-1",
                "Test Product",
                2,
                BigDecimal.valueOf(10000)
            );
        CreateOrderRequest request = new CreateOrderRequest(
            testUserId,
            List.of(lineItemRequest),
            "KRW",
            Map.of("note", "test order")
        );

        // When & Then
        webTestClient.post()
            .uri("/api/v1/orders")
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.userId").isEqualTo(testUserId)
            .jsonPath("$.data.status").isEqualTo("PENDING")
            .jsonPath("$.data.currency").isEqualTo("KRW")
            .jsonPath("$.data.lineItems").isArray()
            .jsonPath("$.data.lineItems[0].productId").isEqualTo("product-1")
            .jsonPath("$.data.lineItems[0].productName").isEqualTo("Test Product")
            .jsonPath("$.data.lineItems[0].quantity").isEqualTo(2)
            .jsonPath("$.error").doesNotExist();
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should fail with 400 when lineItems is empty")
    void shouldFailWithEmptyLineItems() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            testUserId,
            List.of(),  // Empty line items
            "KRW",
            null
        );

        // When & Then
        webTestClient.post()
            .uri("/api/v1/orders")
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
    @DisplayName("POST /api/v1/orders - Should fail with 400 when currency is invalid")
    void shouldFailWithInvalidCurrency() {
        // Given
        String invalidRequestJson = """
            {
                "userId": "user-123",
                "lineItems": [
                    {
                        "productId": "product-1",
                        "productName": "Test Product",
                        "quantity": 2,
                        "unitPrice": 10000
                    }
                ],
                "currency": "INVALID"
            }
            """;

        // When & Then
        webTestClient.post()
            .uri("/api/v1/orders")
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequestJson)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("VAL_001");
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should fail with 400 when userId is blank")
    void shouldFailWithBlankUserId() {
        // Given
        CreateOrderRequest.LineItemRequest lineItemRequest =
            new CreateOrderRequest.LineItemRequest(
                "product-1",
                "Test Product",
                2,
                BigDecimal.valueOf(10000)
            );
        CreateOrderRequest request = new CreateOrderRequest(
            "  ",  // Blank userId
            List.of(lineItemRequest),
            "KRW",
            null
        );

        // When & Then
        webTestClient.post()
            .uri("/api/v1/orders")
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
    @DisplayName("POST /api/v1/orders - Should fail with 400 when quantity is zero")
    void shouldFailWithZeroQuantity() {
        // Given
        String invalidRequestJson = """
            {
                "userId": "user-123",
                "lineItems": [
                    {
                        "productId": "product-1",
                        "productName": "Test Product",
                        "quantity": 0,
                        "unitPrice": 10000
                    }
                ],
                "currency": "KRW"
            }
            """;

        // When & Then
        webTestClient.post()
            .uri("/api/v1/orders")
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequestJson)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("VAL_001");
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should fail with 400 when unitPrice is zero")
    void shouldFailWithInvalidUnitPrice() {
        // Given
        String invalidRequestJson = """
            {
                "userId": "user-123",
                "lineItems": [
                    {
                        "productId": "product-1",
                        "productName": "Test Product",
                        "quantity": 2,
                        "unitPrice": 0
                    }
                ],
                "currency": "KRW"
            }
            """;

        // When & Then
        webTestClient.post()
            .uri("/api/v1/orders")
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .header(IDEMPOTENCY_HEADER, TEST_IDEMPOTENCY_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequestJson)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("VAL_001");
    }

    // ===== GET /api/v1/orders/{orderId} - Get Order by ID Tests =====

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should get order by ID and return 200")
    void shouldGetOrder() {
        // Given
        String orderId = testOrder.getId().value().toString();
        when(orderService.getOrder(any(OrderId.class)))
            .thenReturn(Mono.just(testOrder));

        // When & Then
        webTestClient.get()
            .uri("/api/v1/orders/{orderId}", orderId)
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.orderId").isEqualTo(orderId)
            .jsonPath("$.data.userId").isEqualTo(testUserId)
            .jsonPath("$.data.status").isEqualTo("PENDING")
            .jsonPath("$.data.currency").isEqualTo("KRW")
            .jsonPath("$.error").doesNotExist();
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should return 404 when order not found")
    void shouldReturn404WhenOrderNotFound() {
        // Given
        String nonExistentOrderId = UUID.randomUUID().toString();
        when(orderService.getOrder(any(OrderId.class)))
            .thenReturn(Mono.error(new OrderNotFoundException(nonExistentOrderId)));

        // When & Then
        webTestClient.get()
            .uri("/api/v1/orders/{orderId}", nonExistentOrderId)
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("ORD_001");
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should return 400 when orderId is invalid UUID")
    void shouldReturn400WhenOrderIdInvalid() {
        // Given
        String invalidOrderId = "invalid-uuid";
        when(orderService.getOrder(any(OrderId.class)))
            .thenReturn(Mono.error(new IllegalArgumentException("Invalid UUID format")));

        // When & Then
        webTestClient.get()
            .uri("/api/v1/orders/{orderId}", invalidOrderId)
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("VAL_001");
    }

    // ===== GET /api/v1/orders?userId={userId} - Get Orders by User ID Tests =====

    @Test
    @DisplayName("GET /api/v1/orders?userId - Should get orders by userId and return 200")
    void shouldGetOrdersByUserId() {
        // Given
        OrderLineItem lineItem1 = OrderLineItem.create(
            "product-1", "Product 1", 1, Money.krw(5000));
        OrderLineItem lineItem2 = OrderLineItem.create(
            "product-2", "Product 2", 2, Money.krw(3000));

        Order order1 = Order.create(testUserId, List.of(lineItem1), Currency.KRW, null);
        Order order2 = Order.create(testUserId, List.of(lineItem2), Currency.KRW, null);

        when(orderService.getOrdersByUserId(testUserId))
            .thenReturn(Flux.just(order1, order2));

        // When & Then
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/orders")
                .queryParam("userId", testUserId)
                .build())
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray()
            .jsonPath("$.data.length()").isEqualTo(2)
            .jsonPath("$.data[0].userId").isEqualTo(testUserId)
            .jsonPath("$.data[1].userId").isEqualTo(testUserId)
            .jsonPath("$.error").doesNotExist();
    }

    @Test
    @DisplayName("GET /api/v1/orders?userId - Should return empty list when user has no orders")
    void shouldReturnEmptyListWhenNoOrders() {
        // Given
        String userWithNoOrders = "user-no-orders";
        when(orderService.getOrdersByUserId(userWithNoOrders))
            .thenReturn(Flux.empty());

        // When & Then
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/orders")
                .queryParam("userId", userWithNoOrders)
                .build())
            .header(TENANT_HEADER, TEST_TENANT_ID)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray()
            .jsonPath("$.data.length()").isEqualTo(0)
            .jsonPath("$.error").doesNotExist();
    }
}
