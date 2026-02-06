package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyResult;
import com.fluxpay.engine.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IdempotencyFilter following TDD.
 * Tests verify that the filter correctly handles idempotency for mutating endpoints.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TEST_TENANT_ID = "tenant-abc";
    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_REQUEST_BODY = "{\"amount\":10000,\"currency\":\"KRW\"}";
    private static final String CACHED_RESPONSE = "{\"success\":true,\"data\":{\"id\":\"pay_123\"}}";

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private WebFilterChain chain;

    private IdempotencyFilter idempotencyFilter;
    private IdempotencyProperties properties;

    @BeforeEach
    void setUp() {
        properties = IdempotencyProperties.defaults();
        idempotencyFilter = new IdempotencyFilter(idempotencyService, properties);
    }

    @Nested
    @DisplayName("GET Request Passthrough")
    class GetRequestPassthroughTests {

        @Test
        @DisplayName("should pass through GET requests without idempotency check")
        void shouldPassThroughGetRequests() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/payments")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
            verify(idempotencyService, never()).acquireLock(any(), anyString(), any());
        }

        @Test
        @DisplayName("should pass through GET requests even without idempotency header")
        void shouldPassThroughGetRequestsWithoutIdempotencyHeader() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders/123")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Idempotency Key Validation")
    class IdempotencyKeyValidationTests {

        @Test
        @DisplayName("should return 400 when idempotency key is missing for POST /api/v1/payments")
        void shouldReturn400WhenIdempotencyKeyMissing() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IdempotencyKeyMissingException.class);
                })
                .verify();

            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 400 when idempotency key is missing for POST /api/v1/orders")
        void shouldReturn400WhenIdempotencyKeyMissingForOrders() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/orders")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .expectError(IdempotencyKeyMissingException.class)
                .verify();

            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 400 when idempotency key has invalid format")
        void shouldReturn400WhenIdempotencyKeyInvalidFormat() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, "not-a-valid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IdempotencyKeyInvalidException.class);
                })
                .verify();

            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 400 when idempotency key is empty")
        void shouldReturn400WhenIdempotencyKeyEmpty() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, "")
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .expectError(IdempotencyKeyMissingException.class)
                .verify();

            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Cache Hit Scenarios")
    class CacheHitTests {

        @Test
        @DisplayName("should return cached response on cache hit")
        void shouldReturnCachedResponseOnHit() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, VALID_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(IdempotencyResult.hit(CACHED_RESPONSE, 200)));

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            // Verify response was written
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);

            // Chain should NOT be called for cache hits
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return cached response with original status code")
        void shouldReturnCachedResponseWithOriginalStatusCode() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, VALID_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // Cached response with 201 status
            when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(IdempotencyResult.hit(CACHED_RESPONSE, 201)));

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Conflict Scenarios")
    class ConflictTests {

        @Test
        @DisplayName("should return 409 Conflict when same key used with different payload")
        void shouldReturn409OnConflict() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, VALID_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(IdempotencyResult.conflict()));

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IdempotencyConflictException.class);
                })
                .verify();

            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Processing Scenarios")
    class ProcessingTests {

        @Test
        @DisplayName("should return 409 when request is already being processed")
        void shouldReturn409WhenProcessing() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, VALID_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(IdempotencyResult.processing()));

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(IdempotencyProcessingException.class);
                })
                .verify();

            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Cache Miss Scenarios")
    class CacheMissTests {

        @Test
        @DisplayName("should process and cache new request on cache miss")
        void shouldProcessAndCacheNewRequest() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/payments")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, VALID_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TEST_REQUEST_BODY);

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(IdempotencyResult.miss()));
            when(chain.filter(any())).thenReturn(Mono.empty());
            when(idempotencyService.store(any(IdempotencyKey.class), anyString(), anyString(), anyInt(), any(Duration.class)))
                .thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            // Chain should be called for cache miss
            verify(chain).filter(any());
            // Result should be stored
            verify(idempotencyService).store(any(IdempotencyKey.class), anyString(), anyString(), anyInt(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Health Endpoint Exclusion")
    class HealthEndpointExclusionTests {

        @Test
        @DisplayName("should not apply idempotency to health endpoints")
        void shouldNotApplyToHealthEndpoints() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/actuator/health")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
            verify(idempotencyService, never()).acquireLock(any(), anyString(), any());
        }

        @Test
        @DisplayName("should not apply idempotency to actuator endpoints")
        void shouldNotApplyToActuatorEndpoints() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/actuator/info")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
            verify(idempotencyService, never()).acquireLock(any(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("PUT Request Handling")
    class PutRequestHandlingTests {

        @Test
        @DisplayName("should require idempotency key for PUT /api/v1/orders/{id}/cancel")
        void shouldRequireIdempotencyKeyForOrderCancel() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .put("/api/v1/orders/123/cancel")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .build();

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .expectError(IdempotencyKeyMissingException.class)
                .verify();

            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should process PUT request with valid idempotency key")
        void shouldProcessPutRequestWithValidIdempotencyKey() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                .put("/api/v1/orders/123/cancel")
                .header(TENANT_HEADER, TEST_TENANT_ID)
                .header(IDEMPOTENCY_HEADER, VALID_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .build();

            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(idempotencyService.acquireLock(any(IdempotencyKey.class), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(IdempotencyResult.miss()));
            when(chain.filter(any())).thenReturn(Mono.empty());
            when(idempotencyService.store(any(IdempotencyKey.class), anyString(), anyString(), anyInt(), any(Duration.class)))
                .thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(any());
        }
    }

    @Nested
    @DisplayName("Delete Request Handling")
    class DeleteRequestHandlingTests {

        @Test
        @DisplayName("should pass through DELETE requests without idempotency (DELETE is idempotent by nature)")
        void shouldPassThroughDeleteRequests() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/payments/123")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
            verify(idempotencyService, never()).acquireLock(any(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Endpoint Pattern Matching")
    class EndpointPatternMatchingTests {

        @Test
        @DisplayName("should not require idempotency for non-API endpoints")
        void shouldNotRequireIdempotencyForNonApiEndpoints() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/other/endpoint")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
            verify(idempotencyService, never()).acquireLock(any(), anyString(), any());
        }

        @Test
        @DisplayName("should not require idempotency for state transition endpoints like confirm")
        void shouldNotRequireIdempotencyForConfirmEndpoint() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/payments/123/confirm")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
            verify(idempotencyService, never()).acquireLock(any(), anyString(), any());
        }

        @Test
        @DisplayName("should not require idempotency for state transition endpoints like approve")
        void shouldNotRequireIdempotencyForApproveEndpoint() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/payments/123/approve")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(
                idempotencyFilter.filter(exchange, chain)
                    .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID))
            )
                .verifyComplete();

            verify(chain).filter(exchange);
            verify(idempotencyService, never()).acquireLock(any(), anyString(), any());
        }
    }
}
