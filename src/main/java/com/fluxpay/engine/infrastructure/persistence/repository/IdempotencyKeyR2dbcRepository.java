package com.fluxpay.engine.infrastructure.persistence.repository;

import com.fluxpay.engine.infrastructure.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Spring Data R2DBC repository interface for IdempotencyKeyEntity.
 * Provides reactive CRUD operations for idempotency keys.
 */
public interface IdempotencyKeyR2dbcRepository extends ReactiveCrudRepository<IdempotencyKeyEntity, Long> {

    /**
     * Find an idempotency key by tenant ID, endpoint, and idempotency key.
     *
     * @param tenantId       the tenant ID
     * @param endpoint       the API endpoint
     * @param idempotencyKey the idempotency key
     * @return the idempotency key entity, or empty if not found
     */
    Mono<IdempotencyKeyEntity> findByTenantIdAndEndpointAndIdempotencyKey(
        String tenantId, String endpoint, String idempotencyKey);

    /**
     * Find an idempotency key that has not expired.
     *
     * @param tenantId       the tenant ID
     * @param endpoint       the API endpoint
     * @param idempotencyKey the idempotency key
     * @param expiresAfter   filter keys with expires_at > this timestamp
     * @return the idempotency key entity, or empty if not found or expired
     */
    Mono<IdempotencyKeyEntity> findByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtAfter(
        String tenantId, String endpoint, String idempotencyKey, Instant expiresAfter);

    /**
     * Update the response and status of an existing idempotency key.
     *
     * @param tenantId       the tenant ID
     * @param endpoint       the API endpoint
     * @param idempotencyKey the idempotency key
     * @param response       the response to store
     * @param httpStatus     the HTTP status code
     * @param expiresAt      the new expiration time
     * @return the count of updated records
     */
    @Modifying
    @Query("""
        UPDATE idempotency_keys
        SET response = :response, http_status = :httpStatus, expires_at = :expiresAt
        WHERE tenant_id = :tenantId AND endpoint = :endpoint AND idempotency_key = :idempotencyKey
        """)
    Mono<Integer> updateResponse(
        String tenantId, String endpoint, String idempotencyKey,
        String response, int httpStatus, Instant expiresAt);

    /**
     * Delete an idempotency key by tenant ID, endpoint, and idempotency key.
     *
     * @param tenantId       the tenant ID
     * @param endpoint       the API endpoint
     * @param idempotencyKey the idempotency key
     * @return the count of deleted records
     */
    @Modifying
    @Query("""
        DELETE FROM idempotency_keys
        WHERE tenant_id = :tenantId AND endpoint = :endpoint AND idempotency_key = :idempotencyKey
        """)
    Mono<Integer> deleteByTenantIdAndEndpointAndIdempotencyKey(
        String tenantId, String endpoint, String idempotencyKey);

    /**
     * Delete all idempotency keys that have expired before the given timestamp.
     * Used for cleanup of expired keys by a scheduled job.
     *
     * @param expiresAt the cutoff timestamp; keys with expires_at before this will be deleted
     * @return the count of deleted records
     */
    @Modifying
    @Query("DELETE FROM idempotency_keys WHERE expires_at < :expiresAt")
    Mono<Integer> deleteByExpiresAtBefore(Instant expiresAt);

    /**
     * Delete an idempotency key if it has expired before the given timestamp.
     * Used to clean up expired records during lock acquisition.
     *
     * @param tenantId       the tenant ID
     * @param endpoint       the API endpoint
     * @param idempotencyKey the idempotency key
     * @param expiresAt      the cutoff timestamp; key will be deleted if expires_at is before this
     * @return the count of deleted records (0 or 1)
     */
    @Modifying
    @Query("""
        DELETE FROM idempotency_keys
        WHERE tenant_id = :tenantId AND endpoint = :endpoint AND idempotency_key = :idempotencyKey
        AND expires_at < :expiresAt
        """)
    Mono<Long> deleteByTenantIdAndEndpointAndIdempotencyKeyAndExpiresAtBefore(
        String tenantId, String endpoint, String idempotencyKey, Instant expiresAt);
}
