package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyResult;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyStatus;
import com.fluxpay.engine.domain.port.outbound.IdempotencyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdempotencyService.
 * Following TDD: These tests are written before the implementation.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyPort redisAdapter;

    @Mock
    private IdempotencyPort r2dbcAdapter;

    private IdempotencyService idempotencyService;

    private static final IdempotencyKey TEST_KEY = new IdempotencyKey("tenant1", "/payments", "uuid-123");
    private static final String TEST_PAYLOAD_HASH = "sha256-hash-of-payload";
    private static final String TEST_RESPONSE = "{\"paymentId\":\"pay_123\",\"status\":\"SUCCESS\"}";
    private static final int TEST_HTTP_STATUS = 201;
    private static final Duration TEST_TTL = Duration.ofHours(24);

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(redisAdapter, r2dbcAdapter);
    }

    @Nested
    @DisplayName("Check operation tests")
    class CheckOperationTests {

        @Test
        @DisplayName("Should return MISS for new request when both Redis and PostgreSQL return MISS")
        void shouldReturnMissForNewRequest() {
            // Given - When Redis returns MISS, also check PostgreSQL
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(IdempotencyResult.miss()));
            when(r2dbcAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(IdempotencyResult.miss()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isMiss()).isTrue();
                    assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.MISS);
                })
                .verifyComplete();

            verify(redisAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
            verify(r2dbcAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
        }

        @Test
        @DisplayName("Should return HIT for duplicate request with same payload")
        void shouldReturnHitForDuplicateRequest() {
            // Given
            IdempotencyResult cachedResult = IdempotencyResult.hit(TEST_RESPONSE, TEST_HTTP_STATUS);
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(cachedResult));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isHit()).isTrue();
                    assertThat(idempotencyResult.cachedResponse()).isEqualTo(TEST_RESPONSE);
                    assertThat(idempotencyResult.cachedHttpStatus()).isEqualTo(TEST_HTTP_STATUS);
                })
                .verifyComplete();

            verify(redisAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
            verifyNoInteractions(r2dbcAdapter);
        }

        @Test
        @DisplayName("Should return CONFLICT for request with different payload")
        void shouldReturnConflictForDifferentPayload() {
            // Given
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(IdempotencyResult.conflict()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isConflict()).isTrue();
                    assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.CONFLICT);
                })
                .verifyComplete();

            verify(redisAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
            verifyNoInteractions(r2dbcAdapter);
        }

        @Test
        @DisplayName("Should fallback to PostgreSQL when Redis check fails")
        void shouldFallbackToPostgreSqlWhenRedisCheckFails() {
            // Given
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.error(new RedisIdempotencyException("Redis connection failed")));
            when(r2dbcAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(IdempotencyResult.miss()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isMiss()).isTrue();
                })
                .verifyComplete();

            verify(redisAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
            verify(r2dbcAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
        }

        @Test
        @DisplayName("Should return HIT from PostgreSQL fallback when Redis fails")
        void shouldReturnHitFromPostgreSqlFallback() {
            // Given
            IdempotencyResult cachedResult = IdempotencyResult.hit(TEST_RESPONSE, TEST_HTTP_STATUS);
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.error(new RuntimeException("Redis unavailable")));
            when(r2dbcAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(cachedResult));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isHit()).isTrue();
                    assertThat(idempotencyResult.cachedResponse()).isEqualTo(TEST_RESPONSE);
                    assertThat(idempotencyResult.cachedHttpStatus()).isEqualTo(TEST_HTTP_STATUS);
                })
                .verifyComplete();

            verify(redisAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
            verify(r2dbcAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
        }

        @Test
        @DisplayName("Should propagate error when both Redis and PostgreSQL fail")
        void shouldPropagateErrorWhenBothFail() {
            // Given
            RuntimeException postgresError = new RuntimeException("PostgreSQL connection failed");
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.error(new RedisIdempotencyException("Redis connection failed")));
            when(r2dbcAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.error(postgresError));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(redisAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
            verify(r2dbcAdapter).check(TEST_KEY, TEST_PAYLOAD_HASH);
        }
    }

    @Nested
    @DisplayName("Store operation tests")
    class StoreOperationTests {

        @Test
        @DisplayName("Should store in both Redis and PostgreSQL layers")
        void shouldStoreInBothLayers() {
            // Given
            when(redisAdapter.store(eq(TEST_KEY), eq(TEST_PAYLOAD_HASH), eq(TEST_RESPONSE), eq(TEST_HTTP_STATUS), eq(TEST_TTL)))
                .thenReturn(Mono.empty());
            when(r2dbcAdapter.store(eq(TEST_KEY), eq(TEST_PAYLOAD_HASH), eq(TEST_RESPONSE), eq(TEST_HTTP_STATUS), eq(TEST_TTL)))
                .thenReturn(Mono.empty());

            // When
            Mono<Void> result = idempotencyService.store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            verify(redisAdapter).store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);
            verify(r2dbcAdapter).store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);
        }

        @Test
        @DisplayName("Should continue with PostgreSQL when Redis store fails")
        void shouldContinueWithPostgreSqlWhenRedisStoreFails() {
            // Given
            when(redisAdapter.store(any(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(Mono.error(new RedisIdempotencyException("Redis write failed")));
            when(r2dbcAdapter.store(any(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(Mono.empty());

            // When
            Mono<Void> result = idempotencyService.store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            verify(redisAdapter).store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);
            verify(r2dbcAdapter).store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);
        }

        @Test
        @DisplayName("Should fail when PostgreSQL store fails even if Redis succeeds")
        void shouldFailWhenPostgreSqlStoreFails() {
            // Given
            when(redisAdapter.store(any(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(Mono.empty());
            when(r2dbcAdapter.store(any(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(Mono.error(new RuntimeException("PostgreSQL write failed")));

            // When
            Mono<Void> result = idempotencyService.store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

            verify(redisAdapter).store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);
            verify(r2dbcAdapter).store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);
        }

        @Test
        @DisplayName("Should succeed even when only PostgreSQL store succeeds")
        void shouldSucceedWhenOnlyPostgreSqlStoreSucceeds() {
            // Given - Redis fails but PostgreSQL succeeds
            when(redisAdapter.store(any(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(Mono.error(new RedisIdempotencyException("Redis unavailable")));
            when(r2dbcAdapter.store(any(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(Mono.empty());

            // When
            Mono<Void> result = idempotencyService.store(TEST_KEY, TEST_PAYLOAD_HASH, TEST_RESPONSE, TEST_HTTP_STATUS, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Edge case tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle expired key as MISS (Redis returns empty)")
        void shouldHandleExpiredKeyAsMiss() {
            // Given - When a key expires in Redis, check returns MISS
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(IdempotencyResult.miss()));
            when(r2dbcAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(IdempotencyResult.miss()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isMiss()).isTrue();
                    assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.MISS);
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should return PROCESSING status when request is in progress")
        void shouldReturnProcessingStatus() {
            // Given
            when(redisAdapter.check(TEST_KEY, TEST_PAYLOAD_HASH))
                .thenReturn(Mono.just(IdempotencyResult.processing()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.check(TEST_KEY, TEST_PAYLOAD_HASH);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isProcessing()).isTrue();
                    assertThat(idempotencyResult.status()).isEqualTo(IdempotencyStatus.PROCESSING);
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("AcquireLock operation tests")
    class AcquireLockOperationTests {

        @Test
        @DisplayName("Should acquire lock and return MISS when key does not exist")
        void shouldAcquireLockAndReturnMiss() {
            // Given
            when(redisAdapter.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL))
                .thenReturn(Mono.just(IdempotencyResult.miss()));
            when(r2dbcAdapter.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL))
                .thenReturn(Mono.just(IdempotencyResult.miss()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isMiss()).isTrue();
                })
                .verifyComplete();

            verify(redisAdapter).acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);
            verify(r2dbcAdapter).acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);
        }

        @Test
        @DisplayName("Should return HIT when lock already exists with same payload")
        void shouldReturnHitWhenLockExists() {
            // Given
            IdempotencyResult cachedResult = IdempotencyResult.hit(TEST_RESPONSE, TEST_HTTP_STATUS);
            when(redisAdapter.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL))
                .thenReturn(Mono.just(cachedResult));

            // When
            Mono<IdempotencyResult> result = idempotencyService.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isHit()).isTrue();
                    assertThat(idempotencyResult.cachedResponse()).isEqualTo(TEST_RESPONSE);
                })
                .verifyComplete();

            verify(redisAdapter).acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);
            verifyNoInteractions(r2dbcAdapter);
        }

        @Test
        @DisplayName("Should return PROCESSING when lock is held by another request")
        void shouldReturnProcessingWhenLockHeld() {
            // Given
            when(redisAdapter.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL))
                .thenReturn(Mono.just(IdempotencyResult.processing()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isProcessing()).isTrue();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should fallback to PostgreSQL when Redis acquireLock fails")
        void shouldFallbackToPostgreSqlOnRedisFailure() {
            // Given
            when(redisAdapter.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL))
                .thenReturn(Mono.error(new RedisIdempotencyException("Redis unavailable")));
            when(r2dbcAdapter.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL))
                .thenReturn(Mono.just(IdempotencyResult.miss()));

            // When
            Mono<IdempotencyResult> result = idempotencyService.acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);

            // Then
            StepVerifier.create(result)
                .assertNext(idempotencyResult -> {
                    assertThat(idempotencyResult.isMiss()).isTrue();
                })
                .verifyComplete();

            verify(redisAdapter).acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);
            verify(r2dbcAdapter).acquireLock(TEST_KEY, TEST_PAYLOAD_HASH, TEST_TTL);
        }
    }

    @Nested
    @DisplayName("ReleaseLock operation tests")
    class ReleaseLockOperationTests {

        @Test
        @DisplayName("Should release lock in both Redis and PostgreSQL")
        void shouldReleaseLockInBothLayers() {
            // Given
            when(redisAdapter.releaseLock(TEST_KEY)).thenReturn(Mono.empty());
            when(r2dbcAdapter.releaseLock(TEST_KEY)).thenReturn(Mono.empty());

            // When
            Mono<Void> result = idempotencyService.releaseLock(TEST_KEY);

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            verify(redisAdapter).releaseLock(TEST_KEY);
            verify(r2dbcAdapter).releaseLock(TEST_KEY);
        }

        @Test
        @DisplayName("Should continue when Redis releaseLock fails")
        void shouldContinueWhenRedisReleaseFails() {
            // Given
            when(redisAdapter.releaseLock(TEST_KEY))
                .thenReturn(Mono.error(new RedisIdempotencyException("Redis unavailable")));
            when(r2dbcAdapter.releaseLock(TEST_KEY)).thenReturn(Mono.empty());

            // When
            Mono<Void> result = idempotencyService.releaseLock(TEST_KEY);

            // Then
            StepVerifier.create(result)
                .verifyComplete();

            verify(redisAdapter).releaseLock(TEST_KEY);
            verify(r2dbcAdapter).releaseLock(TEST_KEY);
        }
    }
}
