package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyResult;
import com.fluxpay.engine.domain.port.outbound.IdempotencyPort;
import com.fluxpay.engine.infrastructure.persistence.entity.IdempotencyKeyEntity;
import com.fluxpay.engine.infrastructure.persistence.repository.IdempotencyKeyR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * R2DBC-based implementation of IdempotencyPort.
 * This adapter uses PostgreSQL as a fallback persistence layer when Redis is unavailable.
 *
 * <p>Uses INSERT ON CONFLICT (via unique constraint) for atomic lock acquisition.
 */
@Component
@Qualifier("r2dbcIdempotencyAdapter")
public class R2dbcIdempotencyAdapter implements IdempotencyPort {

    private static final Logger log = LoggerFactory.getLogger(R2dbcIdempotencyAdapter.class);

    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";

    private final IdempotencyKeyR2dbcRepository repository;

    public R2dbcIdempotencyAdapter(IdempotencyKeyR2dbcRepository repository) {
        this.repository = repository;
    }

    /**
     * Check if a request with the given idempotency key has been processed.
     * Filters out expired entries based on expires_at.
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @return result indicating HIT (cached), MISS (new), CONFLICT (different payload), or PROCESSING
     */
    @Override
    public Mono<IdempotencyResult> check(IdempotencyKey key, String payloadHash) {
        Instant now = Instant.now();

        return repository.findByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtAfter(
                key.tenantId(),
                key.endpoint(),
                key.idempotencyKey(),
                now
            )
            .map(entity -> evaluateResult(entity, payloadHash))
            .defaultIfEmpty(IdempotencyResult.miss())
            .doOnSuccess(result -> log.debug("Idempotency check for key {}: {}", key.toCompositeKey(), result.status()));
    }

    /**
     * Atomically try to acquire a lock for the given idempotency key.
     * Uses INSERT with unique constraint - if insert fails due to duplicate, check existing record.
     *
     * @param key         the idempotency key
     * @param payloadHash SHA-256 hash of the request payload
     * @param ttl         time-to-live for the lock
     * @return MISS if lock acquired, HIT/CONFLICT/PROCESSING if key already exists
     */
    @Override
    public Mono<IdempotencyResult> acquireLock(IdempotencyKey key, String payloadHash, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setTenantId(key.tenantId());
        entity.setEndpoint(key.endpoint());
        entity.setIdempotencyKey(key.idempotencyKey());
        entity.setPayloadHash(payloadHash);
        entity.setResponse(STATUS_PROCESSING);
        entity.setHttpStatus(0);
        entity.setCreatedAt(now);
        entity.setExpiresAt(expiresAt);

        return repository.save(entity)
            .map(saved -> {
                log.debug("Lock acquired for key {}", key.toCompositeKey());
                return IdempotencyResult.miss();
            })
            .onErrorResume(DuplicateKeyException.class, ex -> {
                log.debug("Lock acquisition failed (duplicate), checking/cleaning expired for key {}", key.toCompositeKey());
                // First try to delete if expired, then retry insert or check existing
                return repository.deleteByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtBefore(
                        key.tenantId(), key.endpoint(), key.idempotencyKey(), now)
                    .filter(deleted -> deleted > 0)
                    .flatMap(deleted -> {
                        // Record was expired and deleted, retry insert
                        log.debug("Expired record deleted for key {}, retrying insert", key.toCompositeKey());
                        return repository.save(entity).map(saved -> IdempotencyResult.miss());
                    })
                    .switchIfEmpty(Mono.defer(() -> check(key, payloadHash)));
            });
    }

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
    @Override
    public Mono<Void> store(IdempotencyKey key, String payloadHash, String response, int httpStatus, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        return repository.updateResponse(
                key.tenantId(),
                key.endpoint(),
                key.idempotencyKey(),
                response,
                httpStatus,
                expiresAt
            )
            .doOnSuccess(updated -> log.debug("Stored idempotency result for key {}, updated={}", key.toCompositeKey(), updated))
            .then();
    }

    /**
     * Release a lock without storing a response.
     *
     * @param key the idempotency key
     * @return empty Mono when complete
     */
    @Override
    public Mono<Void> releaseLock(IdempotencyKey key) {
        return repository.deleteByTenantIdAndEndpointAndIdempotencyKey(
                key.tenantId(),
                key.endpoint(),
                key.idempotencyKey()
            )
            .doOnSuccess(deleted -> log.debug("Released lock for key {}, deleted={}", key.toCompositeKey(), deleted))
            .then();
    }

    private IdempotencyResult evaluateResult(IdempotencyKeyEntity entity, String payloadHash) {
        if (!entity.getPayloadHash().equals(payloadHash)) {
            return IdempotencyResult.conflict();
        }

        if (STATUS_PROCESSING.equals(entity.getResponse())) {
            return IdempotencyResult.processing();
        }

        return IdempotencyResult.hit(entity.getResponse(), entity.getHttpStatus());
    }
}
