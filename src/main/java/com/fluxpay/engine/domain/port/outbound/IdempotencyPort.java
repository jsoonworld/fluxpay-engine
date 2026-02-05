package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.infrastructure.idempotency.IdempotencyKey;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyResult;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Output port for idempotency operations.
 */
public interface IdempotencyPort {

    /**
     * Check if a request with the given idempotency key has been processed.
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @return result indicating HIT (cached), MISS (new), CONFLICT (different payload), or PROCESSING (in progress)
     */
    Mono<IdempotencyResult> check(IdempotencyKey key, String payloadHash);

    /**
     * Atomically try to acquire a lock for the given idempotency key.
     * This prevents race conditions where two concurrent requests both get MISS.
     *
     * <p>The operation is atomic: either the lock is acquired (returns MISS) or
     * the existing state is returned (HIT, CONFLICT, or PROCESSING).
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @param ttl         time-to-live for the lock (should be longer than max request processing time)
     * @return MISS if lock acquired, HIT/CONFLICT/PROCESSING if key already exists
     */
    Mono<IdempotencyResult> acquireLock(IdempotencyKey key, String payloadHash, Duration ttl);

    /**
     * Store the result of a processed request, updating an existing lock entry.
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @param response    JSON response to cache
     * @param httpStatus  HTTP status code to cache
     * @param ttl         time-to-live for the cached entry
     * @return empty Mono when complete
     */
    Mono<Void> store(IdempotencyKey key, String payloadHash, String response, int httpStatus, Duration ttl);

    /**
     * Release a lock without storing a response (e.g., on processing failure).
     * This allows the same idempotency key to be retried.
     *
     * @param key the idempotency key
     * @return empty Mono when complete
     */
    Mono<Void> releaseLock(IdempotencyKey key);
}
