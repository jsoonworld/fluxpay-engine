package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.port.outbound.IdempotencyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service that orchestrates idempotency operations using Redis (primary) and PostgreSQL (fallback).
 *
 * <p>This service implements a 2-layer strategy with atomic lock acquisition:
 * <ul>
 *   <li>AcquireLock: Atomically acquire a processing lock, try Redis first, fallback to PostgreSQL</li>
 *   <li>Store: Update lock with response in both Redis and PostgreSQL</li>
 *   <li>ReleaseLock: Remove lock on processing failure to allow retry</li>
 * </ul>
 *
 * <p>The atomic lock acquisition prevents race conditions where concurrent requests
 * with the same idempotency key could both be processed.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyPort redisAdapter;
    private final IdempotencyPort r2dbcAdapter;

    public IdempotencyService(
            @Qualifier("redisIdempotencyAdapter") IdempotencyPort redisAdapter,
            @Qualifier("r2dbcIdempotencyAdapter") IdempotencyPort r2dbcAdapter) {
        this.redisAdapter = redisAdapter;
        this.r2dbcAdapter = r2dbcAdapter;
    }

    /**
     * Check if a request with the given idempotency key has been processed.
     *
     * <p>Strategy: Try Redis first, fallback to PostgreSQL on Redis failure.
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @return result indicating HIT (cached), MISS (new), CONFLICT (different payload), or PROCESSING
     */
    public Mono<IdempotencyResult> check(IdempotencyKey key, String payloadHash) {
        return redisAdapter.check(key, payloadHash)
            .flatMap(result -> {
                if (result.isMiss()) {
                    return r2dbcAdapter.check(key, payloadHash);
                }
                return Mono.just(result);
            })
            .onErrorResume(ex -> {
                log.warn("Redis check failed for key {}, falling back to PostgreSQL: {}",
                    key.toCompositeKey(), ex.getMessage());
                return r2dbcAdapter.check(key, payloadHash);
            })
            .doOnSuccess(result -> {
                if (result != null) {
                    log.debug("Idempotency check result for key {}: {}",
                        key.toCompositeKey(), result.status());
                }
            });
    }

    /**
     * Atomically acquire a lock for the given idempotency key.
     *
     * <p>Strategy: Try Redis first. On success (MISS), also try PostgreSQL to ensure
     * durability. On Redis failure, fallback to PostgreSQL only.
     *
     * <p>Returns:
     * <ul>
     *   <li>MISS - Lock acquired, proceed with processing</li>
     *   <li>HIT - Already processed, return cached response</li>
     *   <li>CONFLICT - Same key with different payload</li>
     *   <li>PROCESSING - Another request is being processed</li>
     * </ul>
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @param ttl         time-to-live for the lock
     * @return result indicating lock status
     */
    public Mono<IdempotencyResult> acquireLock(IdempotencyKey key, String payloadHash, Duration ttl) {
        return redisAdapter.acquireLock(key, payloadHash, ttl)
            .flatMap(redisResult -> {
                if (redisResult.isMiss()) {
                    // Lock acquired in Redis, also acquire in PostgreSQL for durability
                    return r2dbcAdapter.acquireLock(key, payloadHash, ttl)
                        .map(dbResult -> {
                            // If DB says something different than MISS, Redis might have stale data
                            // Trust DB as source of truth if it has completed data
                            if (dbResult.isHit() || dbResult.isConflict()) {
                                log.warn("Redis lock acquired but DB returned {}, using DB result", dbResult.status());
                                return dbResult;
                            }
                            return redisResult;
                        })
                        .onErrorResume(ex -> {
                            log.warn("PostgreSQL lock acquisition failed, using Redis result: {}", ex.getMessage());
                            return Mono.just(redisResult);
                        });
                }
                return Mono.just(redisResult);
            })
            .onErrorResume(ex -> {
                log.warn("Redis acquireLock failed for key {}, falling back to PostgreSQL: {}",
                    key.toCompositeKey(), ex.getMessage());
                return r2dbcAdapter.acquireLock(key, payloadHash, ttl);
            })
            .doOnSuccess(result -> log.debug("Acquire lock result for key {}: {}", key.toCompositeKey(), result.status()));
    }

    /**
     * Store the result of a processed request.
     *
     * <p>Strategy: Store in both Redis and PostgreSQL concurrently.
     * If Redis fails, log a warning and continue with PostgreSQL.
     * PostgreSQL is considered the source of truth.
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @param response    JSON response to cache
     * @param httpStatus  HTTP status code to cache
     * @param ttl         time-to-live for the cached entry
     * @return empty Mono when complete
     */
    public Mono<Void> store(IdempotencyKey key, String payloadHash,
                            String response, int httpStatus, Duration ttl) {
        Mono<Void> redisStore = redisAdapter.store(key, payloadHash, response, httpStatus, ttl)
            .doOnSuccess(v -> log.debug("Stored idempotency result in Redis for key {}", key.toCompositeKey()))
            .onErrorResume(ex -> {
                log.warn("Redis store failed for key {}, continuing with PostgreSQL: {}",
                    key.toCompositeKey(), ex.getMessage());
                return Mono.empty();
            });

        Mono<Void> postgresStore = r2dbcAdapter.store(key, payloadHash, response, httpStatus, ttl)
            .doOnSuccess(v -> log.debug("Stored idempotency result in PostgreSQL for key {}", key.toCompositeKey()));

        return Mono.when(redisStore, postgresStore)
            .doOnSuccess(v -> log.debug("Idempotency store completed for key {}", key.toCompositeKey()));
    }

    /**
     * Release a lock without storing a response (e.g., on processing failure).
     *
     * <p>Strategy: Release in both Redis and PostgreSQL.
     *
     * @param key the idempotency key
     * @return empty Mono when complete
     */
    public Mono<Void> releaseLock(IdempotencyKey key) {
        Mono<Void> redisRelease = redisAdapter.releaseLock(key)
            .doOnSuccess(v -> log.debug("Released lock in Redis for key {}", key.toCompositeKey()))
            .onErrorResume(ex -> {
                log.warn("Redis releaseLock failed for key {}: {}", key.toCompositeKey(), ex.getMessage());
                return Mono.empty();
            });

        Mono<Void> postgresRelease = r2dbcAdapter.releaseLock(key)
            .doOnSuccess(v -> log.debug("Released lock in PostgreSQL for key {}", key.toCompositeKey()))
            .onErrorResume(ex -> {
                log.warn("PostgreSQL releaseLock failed for key {}: {}", key.toCompositeKey(), ex.getMessage());
                return Mono.empty();
            });

        return Mono.when(redisRelease, postgresRelease)
            .doOnSuccess(v -> log.debug("Lock release completed for key {}", key.toCompositeKey()));
    }
}
