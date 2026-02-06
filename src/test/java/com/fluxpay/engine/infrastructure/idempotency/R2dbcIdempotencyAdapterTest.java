package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyResult;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyStatus;
import com.fluxpay.engine.infrastructure.persistence.entity.IdempotencyKeyEntity;
import com.fluxpay.engine.infrastructure.persistence.repository.IdempotencyKeyR2dbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for R2dbcIdempotencyAdapter following TDD RED phase.
 * These tests define the expected behavior before implementation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("R2dbcIdempotencyAdapter")
class R2dbcIdempotencyAdapterTest {

    @Mock
    private IdempotencyKeyR2dbcRepository repository;

    private R2dbcIdempotencyAdapter adapter;

    private IdempotencyKey testKey;
    private String testPayloadHash;
    private String testResponse;
    private int testHttpStatus;
    private Duration testTtl;

    @BeforeEach
    void setUp() {
        adapter = new R2dbcIdempotencyAdapter(repository);

        testKey = new IdempotencyKey("tenant-123", "/api/v1/payments", "idem-key-456");
        testPayloadHash = "sha256-hash-abc123";
        testResponse = "{\"paymentId\": \"pay_123\", \"status\": \"COMPLETED\"}";
        testHttpStatus = 200;
        testTtl = Duration.ofHours(24);
    }

    @Test
    @DisplayName("check should return MISS when idempotency key is not found or expired")
    void shouldReturnMissWhenNotFound() {
        // Given
        when(repository.findByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtAfter(
            eq("tenant-123"),
            eq("/api/v1/payments"),
            eq("idem-key-456"),
            any(Instant.class)
        )).thenReturn(Mono.empty());

        // When
        Mono<IdempotencyResult> result = adapter.check(testKey, testPayloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isMiss()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.MISS);
                assertThat(idempotencyResult.cachedResponse()).isNull();
                assertThat(idempotencyResult.cachedHttpStatus()).isZero();
            })
            .verifyComplete();

        verify(repository).findByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtAfter(
            eq("tenant-123"), eq("/api/v1/payments"), eq("idem-key-456"), any(Instant.class)
        );
    }

    @Test
    @DisplayName("check should return HIT when found with same payload hash")
    void shouldReturnHitWhenFoundWithSamePayload() {
        // Given
        IdempotencyKeyEntity existingEntity = createTestEntity(testPayloadHash);

        when(repository.findByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtAfter(
            eq("tenant-123"),
            eq("/api/v1/payments"),
            eq("idem-key-456"),
            any(Instant.class)
        )).thenReturn(Mono.just(existingEntity));

        // When
        Mono<IdempotencyResult> result = adapter.check(testKey, testPayloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isHit()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.HIT);
                assertThat(idempotencyResult.cachedResponse()).isEqualTo(testResponse);
                assertThat(idempotencyResult.cachedHttpStatus()).isEqualTo(testHttpStatus);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("check should return CONFLICT when found with different payload hash")
    void shouldReturnConflictWhenFoundWithDifferentPayload() {
        // Given
        String differentPayloadHash = "different-sha256-hash";
        IdempotencyKeyEntity existingEntity = createTestEntity("original-hash");

        when(repository.findByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtAfter(
            eq("tenant-123"),
            eq("/api/v1/payments"),
            eq("idem-key-456"),
            any(Instant.class)
        )).thenReturn(Mono.just(existingEntity));

        // When
        Mono<IdempotencyResult> result = adapter.check(testKey, differentPayloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isConflict()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.CONFLICT);
                assertThat(idempotencyResult.cachedResponse()).isNull();
                assertThat(idempotencyResult.cachedHttpStatus()).isZero();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("check should return PROCESSING when response indicates processing status")
    void shouldReturnProcessingWhenInProgress() {
        // Given
        IdempotencyKeyEntity processingEntity = new IdempotencyKeyEntity();
        processingEntity.setId(1L);
        processingEntity.setTenantId("tenant-123");
        processingEntity.setEndpoint("/api/v1/payments");
        processingEntity.setIdempotencyKey("idem-key-456");
        processingEntity.setPayloadHash(testPayloadHash);
        processingEntity.setResponse("processing");
        processingEntity.setHttpStatus(0);
        processingEntity.setCreatedAt(Instant.now());
        processingEntity.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));

        when(repository.findByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtAfter(
            eq("tenant-123"),
            eq("/api/v1/payments"),
            eq("idem-key-456"),
            any(Instant.class)
        )).thenReturn(Mono.just(processingEntity));

        // When
        Mono<IdempotencyResult> result = adapter.check(testKey, testPayloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isProcessing()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.PROCESSING);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("store should update existing idempotency key")
    void shouldUpdateExistingIdempotencyKey() {
        // Given
        when(repository.updateResponse(
            eq("tenant-123"),
            eq("/api/v1/payments"),
            eq("idem-key-456"),
            eq(testResponse),
            eq(testHttpStatus),
            any(Instant.class)
        )).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = adapter.store(testKey, testPayloadHash, testResponse, testHttpStatus, testTtl);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(repository).updateResponse(
            eq("tenant-123"),
            eq("/api/v1/payments"),
            eq("idem-key-456"),
            eq(testResponse),
            eq(testHttpStatus),
            any(Instant.class)
        );
    }

    @Test
    @DisplayName("releaseLock should delete idempotency key")
    void shouldReleaseLockByDeletingKey() {
        // Given
        when(repository.deleteByTenantIdAndEndpointAndIdempotencyKey(
            eq("tenant-123"),
            eq("/api/v1/payments"),
            eq("idem-key-456")
        )).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = adapter.releaseLock(testKey);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(repository).deleteByTenantIdAndEndpointAndIdempotencyKey(
            "tenant-123", "/api/v1/payments", "idem-key-456"
        );
    }

    /**
     * Helper method to create a test IdempotencyKeyEntity.
     */
    private IdempotencyKeyEntity createTestEntity(String payloadHash) {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setId(1L);
        entity.setTenantId("tenant-123");
        entity.setEndpoint("/api/v1/payments");
        entity.setIdempotencyKey("idem-key-456");
        entity.setPayloadHash(payloadHash);
        entity.setResponse(testResponse);
        entity.setHttpStatus(testHttpStatus);
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        return entity;
    }
}
