package com.fluxpay.engine.infrastructure.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TenantWebFilter following TDD.
 * Tests verify that the filter extracts X-Tenant-Id header
 * and propagates it through Reactor Context.
 */
class TenantWebFilterTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TEST_TENANT_ID = "service-a";

    private TenantWebFilter tenantWebFilter;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        tenantWebFilter = new TenantWebFilter();
        chain = mock(WebFilterChain.class);
    }

    @Nested
    @DisplayName("Header Extraction")
    class HeaderExtractionTests {

        @Test
        @DisplayName("should extract tenant ID from header and pass to chain")
        void shouldExtractTenantIdFromHeader() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should reject request when tenant header is missing")
        void shouldRejectWhenTenantHeaderMissing() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                    .build()
            );

            // When & Then
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TenantNotFoundException.class);
                    assertThat(error.getMessage()).contains("X-Tenant-Id");
                })
                .verify();

            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should reject request when tenant header is empty")
        void shouldRejectWhenTenantHeaderEmpty() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                    .header(TENANT_HEADER, "")
                    .build()
            );

            // When & Then
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TenantNotFoundException.class);
                    assertThat(error.getMessage()).contains("X-Tenant-Id");
                })
                .verify();

            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should reject request when tenant header is blank (whitespace only)")
        void shouldRejectWhenTenantHeaderBlank() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                    .header(TENANT_HEADER, "   ")
                    .build()
            );

            // When & Then
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TenantNotFoundException.class);
                    assertThat(error.getMessage()).contains("X-Tenant-Id");
                })
                .verify();

            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Context Propagation")
    class ContextPropagationTests {

        @Test
        @DisplayName("should propagate tenant ID through Reactor Context to chain")
        void shouldPropagateContextToChain() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                    .header(TENANT_HEADER, TEST_TENANT_ID)
                    .build()
            );

            // Capture the context passed to the chain
            when(chain.filter(any())).thenReturn(
                TenantContext.getTenantId()
                    .doOnNext(tenantId -> assertThat(tenantId).isEqualTo(TEST_TENANT_ID))
                    .then()
            );

            // When & Then
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should make tenant ID available via TenantContext.getTenantId()")
        void shouldMakeTenantIdAvailableViaTenantContext() {
            // Given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/payments")
                    .header(TENANT_HEADER, "tenant-xyz")
                    .build()
            );

            // Mock chain that verifies TenantContext works
            when(chain.filter(any())).thenReturn(
                TenantContext.getTenantId()
                    .flatMap(tenantId -> {
                        assertThat(tenantId).isEqualTo("tenant-xyz");
                        return Mono.empty();
                    })
            );

            // When & Then
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Path Exclusion")
    class PathExclusionTests {

        @Test
        @DisplayName("should skip tenant validation for actuator health endpoint")
        void shouldSkipTenantValidationForActuatorHealth() {
            // Given - no tenant header
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health")
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then - should pass without tenant header
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should skip tenant validation for actuator info endpoint")
        void shouldSkipTenantValidationForActuatorInfo() {
            // Given - no tenant header
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/info")
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then - should pass without tenant header
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should skip tenant validation for health endpoint")
        void shouldSkipTenantValidationForHealthEndpoint() {
            // Given - no tenant header
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/health")
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then - should pass without tenant header
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should skip tenant validation for swagger-ui endpoint")
        void shouldSkipTenantValidationForSwaggerUi() {
            // Given - no tenant header
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/index.html")
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then - should pass without tenant header
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should skip tenant validation for api-docs endpoint")
        void shouldSkipTenantValidationForApiDocs() {
            // Given - no tenant header
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api-docs/openapi.json")
                    .build()
            );
            when(chain.filter(any())).thenReturn(Mono.empty());

            // When & Then - should pass without tenant header
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should require tenant header for API endpoints")
        void shouldRequireTenantHeaderForApiEndpoints() {
            // Given - no tenant header for API endpoint
            MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                    .build()
            );

            // When & Then - should reject without tenant header
            StepVerifier.create(tenantWebFilter.filter(exchange, chain))
                .expectError(TenantNotFoundException.class)
                .verify();

            verify(chain, never()).filter(any());
        }
    }
}
