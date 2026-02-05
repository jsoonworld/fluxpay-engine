package com.fluxpay.engine.infrastructure.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TenantContext following TDD.
 * These tests define the expected behavior for tenant context propagation
 * using Reactor Context.
 */
class TenantContextTest {

    private static final String TEST_TENANT_ID = "tenant-123";

    @Nested
    @DisplayName("getTenantId")
    class GetTenantIdTests {

        @Test
        @DisplayName("should get tenant ID from context")
        void shouldGetTenantIdFromContext() {
            // Given
            Mono<String> tenantIdMono = TenantContext.getTenantId()
                .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID));

            // When & Then
            StepVerifier.create(tenantIdMono)
                .assertNext(tenantId -> {
                    assertThat(tenantId).isEqualTo(TEST_TENANT_ID);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw TenantNotFoundException when tenant not in context")
        void shouldThrowWhenTenantNotInContext() {
            // Given
            Mono<String> tenantIdMono = TenantContext.getTenantId();

            // When & Then
            StepVerifier.create(tenantIdMono)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(TenantNotFoundException.class);
                    assertThat(error.getMessage()).contains("Tenant ID not found");
                })
                .verify();
        }
    }

    @Nested
    @DisplayName("getTenantIdOrEmpty")
    class GetTenantIdOrEmptyTests {

        @Test
        @DisplayName("should return tenant ID when present in context")
        void shouldReturnTenantIdWhenPresent() {
            // Given
            Mono<String> tenantIdMono = TenantContext.getTenantIdOrEmpty()
                .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, TEST_TENANT_ID));

            // When & Then
            StepVerifier.create(tenantIdMono)
                .assertNext(tenantId -> {
                    assertThat(tenantId).isEqualTo(TEST_TENANT_ID);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should return empty Mono when tenant not in context")
        void shouldReturnEmptyForOptionalTenantId() {
            // Given
            Mono<String> tenantIdMono = TenantContext.getTenantIdOrEmpty();

            // When & Then
            StepVerifier.create(tenantIdMono)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("withTenant")
    class WithTenantTests {

        @Test
        @DisplayName("should add tenant to context")
        void shouldAddTenantToContext() {
            // Given
            Mono<String> testMono = Mono.just("test-value")
                .flatMap(value -> TenantContext.getTenantId()
                    .map(tenantId -> value + "-" + tenantId))
                .transform(TenantContext.withTenant(TEST_TENANT_ID));

            // When & Then
            StepVerifier.create(testMono)
                .assertNext(result -> {
                    assertThat(result).isEqualTo("test-value-" + TEST_TENANT_ID);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should allow retrieving tenant ID after applying withTenant")
        void shouldAllowRetrievingTenantIdAfterApplyingWithTenant() {
            // Given
            Mono<String> tenantIdMono = TenantContext.getTenantId()
                .transform(TenantContext.withTenant(TEST_TENANT_ID));

            // When & Then
            StepVerifier.create(tenantIdMono)
                .assertNext(tenantId -> {
                    assertThat(tenantId).isEqualTo(TEST_TENANT_ID);
                })
                .verifyComplete();
        }
    }
}
