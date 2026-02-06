package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyResult;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyStatus;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisIdempotencyAdapter.
 * Following TDD: These tests are written before the implementation.
 */
class RedisIdempotencyAdapterTest {

    private ReactiveRedisTemplate<String, String> redisTemplate;
    private ReactiveHashOperations<String, String, String> hashOperations;
    private RedisIdempotencyAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked") // Mockito mock() returns raw types due to generic type erasure
    void setUp() {
        redisTemplate = mock(ReactiveRedisTemplate.class);
        hashOperations = mock(ReactiveHashOperations.class);
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        adapter = new RedisIdempotencyAdapter(redisTemplate);
    }

    @Test
    @DisplayName("Should return MISS for new key that does not exist in Redis")
    void shouldReturnMissForNewKey() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-123");
        String payloadHash = "sha256-hash-of-payload";
        String redisKey = key.toRedisKey();

        when(hashOperations.entries(redisKey)).thenReturn(Flux.empty());

        // When
        Mono<IdempotencyResult> result = adapter.check(key, payloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isMiss()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.MISS);
                assertThat(idempotencyResult.cachedResponse()).isNull();
                assertThat(idempotencyResult.cachedHttpStatus()).isZero();
            })
            .verifyComplete();

        verify(hashOperations).entries(redisKey);
    }

    @Test
    @DisplayName("Should return HIT for existing key with same payload hash")
    void shouldReturnHitForExistingKeyWithSamePayload() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-456");
        String payloadHash = "sha256-hash-of-payload";
        String redisKey = key.toRedisKey();
        String cachedResponse = "{\"paymentId\":\"pay_123\",\"status\":\"SUCCESS\"}";
        int cachedHttpStatus = 201;

        Flux<Map.Entry<String, String>> storedData = Flux.just(
            new AbstractMap.SimpleEntry<>("hash", payloadHash),
            new AbstractMap.SimpleEntry<>("response", cachedResponse),
            new AbstractMap.SimpleEntry<>("httpStatus", String.valueOf(cachedHttpStatus))
        );

        when(hashOperations.entries(redisKey)).thenReturn(storedData);

        // When
        Mono<IdempotencyResult> result = adapter.check(key, payloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isHit()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.HIT);
                assertThat(idempotencyResult.cachedResponse()).isEqualTo(cachedResponse);
                assertThat(idempotencyResult.cachedHttpStatus()).isEqualTo(cachedHttpStatus);
            })
            .verifyComplete();

        verify(hashOperations).entries(redisKey);
    }

    @Test
    @DisplayName("Should return CONFLICT for existing key with different payload hash")
    void shouldReturnConflictForExistingKeyWithDifferentPayload() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-789");
        String newPayloadHash = "sha256-new-hash";
        String storedPayloadHash = "sha256-different-hash";
        String redisKey = key.toRedisKey();

        Flux<Map.Entry<String, String>> storedData = Flux.just(
            new AbstractMap.SimpleEntry<>("hash", storedPayloadHash),
            new AbstractMap.SimpleEntry<>("response", "{\"paymentId\":\"pay_999\"}"),
            new AbstractMap.SimpleEntry<>("httpStatus", "200")
        );

        when(hashOperations.entries(redisKey)).thenReturn(storedData);

        // When
        Mono<IdempotencyResult> result = adapter.check(key, newPayloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isConflict()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.CONFLICT);
                assertThat(idempotencyResult.cachedResponse()).isNull();
                assertThat(idempotencyResult.cachedHttpStatus()).isZero();
            })
            .verifyComplete();

        verify(hashOperations).entries(redisKey);
    }

    @Test
    @DisplayName("Should store result successfully with TTL")
    void shouldStoreResultSuccessfully() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-store-123");
        String payloadHash = "sha256-payload-hash";
        String response = "{\"paymentId\":\"pay_new\",\"status\":\"CREATED\"}";
        int httpStatus = 201;
        Duration ttl = Duration.ofHours(24);
        String redisKey = key.toRedisKey();

        when(hashOperations.putAll(eq(redisKey), anyMap())).thenReturn(Mono.just(true));
        when(redisTemplate.expire(eq(redisKey), eq(ttl))).thenReturn(Mono.just(true));

        // When
        Mono<Void> result = adapter.store(key, payloadHash, response, httpStatus, ttl);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(redisKey), mapCaptor.capture());

        Map<String, String> capturedMap = mapCaptor.getValue();
        assertThat(capturedMap).containsEntry("hash", payloadHash);
        assertThat(capturedMap).containsEntry("response", response);
        assertThat(capturedMap).containsEntry("httpStatus", String.valueOf(httpStatus));

        verify(redisTemplate).expire(redisKey, ttl);
    }

    @Test
    @DisplayName("Should handle Redis error during check gracefully")
    void shouldHandleRedisErrorDuringCheck() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-error");
        String payloadHash = "sha256-hash";
        String redisKey = key.toRedisKey();

        when(hashOperations.entries(redisKey))
            .thenReturn(Flux.error(new RuntimeException("Redis connection failed")));

        // When
        Mono<IdempotencyResult> result = adapter.check(key, payloadHash);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(ex ->
                ex instanceof RedisIdempotencyException &&
                ex.getMessage().contains("Failed to check idempotency key") &&
                ex.getCause() instanceof RuntimeException)
            .verify();
    }

    @Test
    @DisplayName("Should handle Redis error during store gracefully")
    void shouldHandleRedisErrorDuringStore() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-store-error");
        String payloadHash = "sha256-hash";
        String response = "{}";
        int httpStatus = 200;
        Duration ttl = Duration.ofHours(1);
        String redisKey = key.toRedisKey();

        when(hashOperations.putAll(eq(redisKey), anyMap()))
            .thenReturn(Mono.error(new RuntimeException("Redis write failed")));

        // When
        Mono<Void> result = adapter.store(key, payloadHash, response, httpStatus, ttl);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(ex ->
                ex instanceof RedisIdempotencyException &&
                ex.getMessage().contains("Failed to store idempotency result") &&
                ex.getCause() instanceof RuntimeException)
            .verify();
    }

    @Test
    @DisplayName("Should handle incomplete stored data as PROCESSING")
    void shouldHandleIncompleteStoredDataAsProcessing() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-incomplete");
        String payloadHash = "sha256-hash";
        String redisKey = key.toRedisKey();

        // Missing 'response' and 'httpStatus' fields - indicates processing in progress
        Flux<Map.Entry<String, String>> incompleteData = Flux.just(
            new AbstractMap.SimpleEntry<>("hash", payloadHash),
            new AbstractMap.SimpleEntry<>("status", "processing")
        );

        when(hashOperations.entries(redisKey)).thenReturn(incompleteData);

        // When
        Mono<IdempotencyResult> result = adapter.check(key, payloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                // Incomplete data should be treated as PROCESSING
                assertThat(idempotencyResult.isProcessing()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle expire failure gracefully during store")
    void shouldHandleExpireFailureGracefully() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-expire-fail");
        String payloadHash = "sha256-hash";
        String response = "{}";
        int httpStatus = 200;
        Duration ttl = Duration.ofHours(1);
        String redisKey = key.toRedisKey();

        when(hashOperations.putAll(eq(redisKey), anyMap())).thenReturn(Mono.just(true));
        when(redisTemplate.expire(eq(redisKey), eq(ttl)))
            .thenReturn(Mono.error(new RuntimeException("Expire failed")));

        // When
        Mono<Void> result = adapter.store(key, payloadHash, response, httpStatus, ttl);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(ex ->
                ex instanceof RedisIdempotencyException &&
                ex.getMessage().contains("Failed to store idempotency result"))
            .verify();
    }

    @Test
    @DisplayName("Should return PROCESSING status for in-progress request")
    void shouldReturnProcessingStatus() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-processing");
        String payloadHash = "sha256-hash";
        String redisKey = key.toRedisKey();

        Flux<Map.Entry<String, String>> processingData = Flux.just(
            new AbstractMap.SimpleEntry<>("hash", payloadHash),
            new AbstractMap.SimpleEntry<>("status", "processing")
        );

        when(hashOperations.entries(redisKey)).thenReturn(processingData);

        // When
        Mono<IdempotencyResult> result = adapter.check(key, payloadHash);

        // Then
        StepVerifier.create(result)
            .assertNext(idempotencyResult -> {
                assertThat(idempotencyResult.isProcessing()).isTrue();
                assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.PROCESSING);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should release lock by deleting key")
    void shouldReleaseLockByDeletingKey() {
        // Given
        IdempotencyKey key = new IdempotencyKey("tenant1", "/payments", "uuid-release");
        String redisKey = key.toRedisKey();

        when(redisTemplate.delete(redisKey)).thenReturn(Mono.just(1L));

        // When
        Mono<Void> result = adapter.releaseLock(key);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(redisTemplate).delete(redisKey);
    }
}
