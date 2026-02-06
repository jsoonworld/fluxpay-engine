package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.port.outbound.IdempotencyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis-based implementation of IdempotencyPort.
 * Uses Redis Hash data structure to store idempotency information with atomic lock acquisition.
 *
 * <p>The adapter stores the following fields in a Redis hash:
 * <ul>
 *   <li>hash - SHA-256 hash of the request payload</li>
 *   <li>status - "processing" or "completed"</li>
 *   <li>response - Cached JSON response (only when completed)</li>
 *   <li>httpStatus - Cached HTTP status code (only when completed)</li>
 * </ul>
 */
@Component
@Qualifier("redisIdempotencyAdapter")
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyAdapter.class);

    private static final String FIELD_HASH = "hash";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RESPONSE = "response";
    private static final String FIELD_HTTP_STATUS = "httpStatus";

    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";

    /**
     * Lua script for atomic lock acquisition.
     * Returns: "ACQUIRED" if lock acquired, "PROCESSING" if in progress,
     * "HIT:response:status" if completed with same hash, "CONFLICT" if different hash
     */
    private static final String ACQUIRE_LOCK_SCRIPT = """
        local key = KEYS[1]
        local payloadHash = ARGV[1]
        local ttlSeconds = tonumber(ARGV[2])

        local existing = redis.call('HGETALL', key)
        if #existing == 0 then
            -- Key doesn't exist, acquire lock
            redis.call('HSET', key, 'hash', payloadHash, 'status', 'processing')
            redis.call('EXPIRE', key, ttlSeconds)
            return 'ACQUIRED'
        end

        -- Convert array to table
        local data = {}
        for i = 1, #existing, 2 do
            data[existing[i]] = existing[i + 1]
        end

        local storedHash = data['hash']
        local storedStatus = data['status']

        if storedHash ~= payloadHash then
            return 'CONFLICT'
        end

        if storedStatus == 'processing' then
            return 'PROCESSING'
        end

        -- Completed with same hash - return cached response
        local response = data['response'] or ''
        local httpStatus = data['httpStatus'] or '200'
        return 'HIT:' .. response .. ':' .. httpStatus
        """;

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisScript<String> acquireLockScript;

    public RedisIdempotencyAdapter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.acquireLockScript = RedisScript.of(ACQUIRE_LOCK_SCRIPT, String.class);
    }

    @Override
    public Mono<IdempotencyResult> check(IdempotencyKey key, String payloadHash) {
        String redisKey = key.toRedisKey();

        return redisTemplate.<String, String>opsForHash()
            .entries(redisKey)
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .map(storedData -> evaluateIdempotencyResult(storedData, payloadHash))
            .doOnSuccess(result -> log.debug("Idempotency check for key {}: {}", redisKey, result.status()))
            .onErrorMap(ex -> new RedisIdempotencyException(
                "Failed to check idempotency key: " + redisKey, ex));
    }

    @Override
    public Mono<IdempotencyResult> acquireLock(IdempotencyKey key, String payloadHash, Duration ttl) {
        String redisKey = key.toRedisKey();
        long ttlSeconds = ttl.toSeconds();

        return redisTemplate.execute(
                acquireLockScript,
                List.of(redisKey),
                List.of(payloadHash, String.valueOf(ttlSeconds))
            )
            .next()
            .map(this::parseAcquireLockResult)
            .doOnSuccess(result -> log.debug("Acquire lock for key {}: {}", redisKey, result.status()))
            .onErrorMap(ex -> new RedisIdempotencyException(
                "Failed to acquire idempotency lock: " + redisKey, ex));
    }

    @Override
    public Mono<Void> store(IdempotencyKey key, String payloadHash, String response, int httpStatus, Duration ttl) {
        String redisKey = key.toRedisKey();

        Map<String, String> fields = Map.of(
            FIELD_HASH, payloadHash,
            FIELD_STATUS, STATUS_COMPLETED,
            FIELD_RESPONSE, response,
            FIELD_HTTP_STATUS, String.valueOf(httpStatus)
        );

        return redisTemplate.<String, String>opsForHash()
            .putAll(redisKey, fields)
            .flatMap(success -> redisTemplate.expire(redisKey, ttl))
            .then()
            .doOnSuccess(v -> log.debug("Stored idempotency result for key {} with TTL {}", redisKey, ttl))
            .onErrorMap(ex -> new RedisIdempotencyException(
                "Failed to store idempotency result for key: " + redisKey, ex));
    }

    @Override
    public Mono<Void> releaseLock(IdempotencyKey key) {
        String redisKey = key.toRedisKey();

        return redisTemplate.delete(redisKey)
            .then()
            .doOnSuccess(v -> log.debug("Released idempotency lock for key {}", redisKey))
            .onErrorMap(ex -> new RedisIdempotencyException(
                "Failed to release idempotency lock: " + redisKey, ex));
    }

    private IdempotencyResult parseAcquireLockResult(String result) {
        if ("ACQUIRED".equals(result)) {
            return IdempotencyResult.miss();
        }
        if ("PROCESSING".equals(result)) {
            return IdempotencyResult.processing();
        }
        if ("CONFLICT".equals(result)) {
            return IdempotencyResult.conflict();
        }
        if (result != null && result.startsWith("HIT:")) {
            // Format: HIT:response:httpStatus
            String remainder = result.substring(4);
            int lastColonIndex = remainder.lastIndexOf(':');
            if (lastColonIndex > 0) {
                String response = remainder.substring(0, lastColonIndex);
                int httpStatus = Integer.parseInt(remainder.substring(lastColonIndex + 1));
                return IdempotencyResult.hit(response, httpStatus);
            }
        }
        log.warn("Unexpected acquire lock result: {}, treating as MISS", result);
        return IdempotencyResult.miss();
    }

    private IdempotencyResult evaluateIdempotencyResult(Map<String, String> storedData, String payloadHash) {
        if (storedData == null || storedData.isEmpty()) {
            return IdempotencyResult.miss();
        }

        String storedHash = storedData.get(FIELD_HASH);
        String storedStatus = storedData.get(FIELD_STATUS);
        String storedResponse = storedData.get(FIELD_RESPONSE);
        String storedHttpStatus = storedData.get(FIELD_HTTP_STATUS);

        if (storedHash == null) {
            log.warn("Incomplete idempotency data found (no hash), treating as MISS");
            return IdempotencyResult.miss();
        }

        if (!storedHash.equals(payloadHash)) {
            return IdempotencyResult.conflict();
        }

        if (STATUS_PROCESSING.equals(storedStatus)) {
            return IdempotencyResult.processing();
        }

        if (storedResponse == null || storedHttpStatus == null) {
            log.warn("Incomplete idempotency data found, treating as PROCESSING");
            return IdempotencyResult.processing();
        }

        int httpStatus = Integer.parseInt(storedHttpStatus);
        return IdempotencyResult.hit(storedResponse, httpStatus);
    }
}
