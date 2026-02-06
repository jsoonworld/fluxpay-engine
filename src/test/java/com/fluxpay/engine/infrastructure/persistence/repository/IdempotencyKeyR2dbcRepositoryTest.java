package com.fluxpay.engine.infrastructure.persistence.repository;

import com.fluxpay.engine.infrastructure.persistence.entity.IdempotencyKeyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for IdempotencyKeyR2dbcRepository.
 * Uses Testcontainers to spin up a PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("IdempotencyKeyR2dbcRepository Integration Tests")
class IdempotencyKeyR2dbcRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fluxpay_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:postgresql://%s:%d/%s",
                        postgres.getHost(),
                        postgres.getFirstMappedPort(),
                        postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private IdempotencyKeyR2dbcRepository idempotencyKeyRepository;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        databaseClient.sql("DELETE FROM idempotency_keys").then().block();
    }

    private IdempotencyKeyEntity createTestEntity(String tenantId, String endpoint, String idempotencyKey) {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setTenantId(tenantId);
        entity.setEndpoint(endpoint);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setPayloadHash("sha256-hash-of-payload-1234567890abcdef");
        entity.setResponse("{\"status\":\"success\",\"paymentId\":\"pay_123\"}");
        entity.setHttpStatus(200);
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        return entity;
    }

    @Nested
    @DisplayName("save and find")
    class SaveAndFind {

        @Test
        @DisplayName("should save and find idempotency key")
        void shouldSaveAndFindIdempotencyKey() {
            // Given
            String tenantId = "tenant-123";
            String endpoint = "/api/v1/payments";
            String idempotencyKey = "idem-key-001";
            IdempotencyKeyEntity entity = createTestEntity(tenantId, endpoint, idempotencyKey);

            // When - save
            var saveResult = idempotencyKeyRepository.save(entity);

            // Then - verify save
            StepVerifier.create(saveResult)
                    .assertNext(saved -> {
                        assertThat(saved.getId()).isNotNull();
                        assertThat(saved.getTenantId()).isEqualTo(tenantId);
                        assertThat(saved.getEndpoint()).isEqualTo(endpoint);
                        assertThat(saved.getIdempotencyKey()).isEqualTo(idempotencyKey);
                        assertThat(saved.getPayloadHash()).isEqualTo("sha256-hash-of-payload-1234567890abcdef");
                        assertThat(saved.getResponse()).contains("success");
                        assertThat(saved.getHttpStatus()).isEqualTo(200);
                        assertThat(saved.getCreatedAt()).isNotNull();
                        assertThat(saved.getExpiresAt()).isNotNull();
                    })
                    .verifyComplete();

            // When - find
            var findResult = idempotencyKeyRepository
                    .findByTenantIdAndEndpointAndIdempotencyKey(tenantId, endpoint, idempotencyKey);

            // Then - verify find
            StepVerifier.create(findResult)
                    .assertNext(found -> {
                        assertThat(found.getTenantId()).isEqualTo(tenantId);
                        assertThat(found.getEndpoint()).isEqualTo(endpoint);
                        assertThat(found.getIdempotencyKey()).isEqualTo(idempotencyKey);
                        assertThat(found.getResponse()).contains("success");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty when idempotency key not found")
        void shouldReturnEmptyWhenNotFound() {
            // When
            var result = idempotencyKeyRepository
                    .findByTenantIdAndEndpointAndIdempotencyKey("non-existent", "/api/v1/test", "missing-key");

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should distinguish by tenant ID")
        void shouldDistinguishByTenantId() {
            // Given
            String endpoint = "/api/v1/payments";
            String idempotencyKey = "shared-key";

            IdempotencyKeyEntity entity1 = createTestEntity("tenant-A", endpoint, idempotencyKey);
            entity1.setResponse("{\"tenant\":\"A\"}");

            IdempotencyKeyEntity entity2 = createTestEntity("tenant-B", endpoint, idempotencyKey);
            entity2.setResponse("{\"tenant\":\"B\"}");

            idempotencyKeyRepository.save(entity1).block();
            idempotencyKeyRepository.save(entity2).block();

            // When - find for tenant A
            var resultA = idempotencyKeyRepository
                    .findByTenantIdAndEndpointAndIdempotencyKey("tenant-A", endpoint, idempotencyKey);

            // Then
            StepVerifier.create(resultA)
                    .assertNext(found -> assertThat(found.getResponse()).contains("\"tenant\":\"A\""))
                    .verifyComplete();

            // When - find for tenant B
            var resultB = idempotencyKeyRepository
                    .findByTenantIdAndEndpointAndIdempotencyKey("tenant-B", endpoint, idempotencyKey);

            // Then
            StepVerifier.create(resultB)
                    .assertNext(found -> assertThat(found.getResponse()).contains("\"tenant\":\"B\""))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should distinguish by endpoint")
        void shouldDistinguishByEndpoint() {
            // Given
            String tenantId = "tenant-123";
            String idempotencyKey = "shared-key";

            IdempotencyKeyEntity entity1 = createTestEntity(tenantId, "/api/v1/payments", idempotencyKey);
            entity1.setResponse("{\"endpoint\":\"payments\"}");

            IdempotencyKeyEntity entity2 = createTestEntity(tenantId, "/api/v1/orders", idempotencyKey);
            entity2.setResponse("{\"endpoint\":\"orders\"}");

            idempotencyKeyRepository.save(entity1).block();
            idempotencyKeyRepository.save(entity2).block();

            // When - find for payments endpoint
            var resultPayments = idempotencyKeyRepository
                    .findByTenantIdAndEndpointAndIdempotencyKey(tenantId, "/api/v1/payments", idempotencyKey);

            // Then
            StepVerifier.create(resultPayments)
                    .assertNext(found -> assertThat(found.getResponse()).contains("\"endpoint\":\"payments\""))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteByExpiresAtBefore")
    class DeleteExpired {

        @Test
        @DisplayName("should delete expired keys")
        void shouldDeleteExpiredKeys() {
            // Given
            Instant now = Instant.now();

            // Create expired key (expired 1 hour ago)
            IdempotencyKeyEntity expiredEntity = createTestEntity("tenant-1", "/api/v1/payments", "expired-key");
            expiredEntity.setExpiresAt(now.minus(1, ChronoUnit.HOURS));
            idempotencyKeyRepository.save(expiredEntity).block();

            // Create valid key (expires in 23 hours)
            IdempotencyKeyEntity validEntity = createTestEntity("tenant-1", "/api/v1/payments", "valid-key");
            validEntity.setExpiresAt(now.plus(23, ChronoUnit.HOURS));
            idempotencyKeyRepository.save(validEntity).block();

            // When
            var deleteResult = idempotencyKeyRepository.deleteByExpiresAtBefore(now);

            // Then - should delete 1 expired key
            StepVerifier.create(deleteResult)
                    .assertNext(deletedCount -> assertThat(deletedCount).isEqualTo(1))
                    .verifyComplete();

            // Verify expired key is gone
            StepVerifier.create(idempotencyKeyRepository
                    .findByTenantIdAndEndpointAndIdempotencyKey("tenant-1", "/api/v1/payments", "expired-key"))
                    .verifyComplete();

            // Verify valid key still exists
            StepVerifier.create(idempotencyKeyRepository
                    .findByTenantIdAndEndpointAndIdempotencyKey("tenant-1", "/api/v1/payments", "valid-key"))
                    .assertNext(found -> assertThat(found.getIdempotencyKey()).isEqualTo("valid-key"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return zero when no expired keys")
        void shouldReturnZeroWhenNoExpiredKeys() {
            // Given
            Instant now = Instant.now();

            // Create only valid keys
            IdempotencyKeyEntity validEntity = createTestEntity("tenant-1", "/api/v1/payments", "valid-key");
            validEntity.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
            idempotencyKeyRepository.save(validEntity).block();

            // When
            var deleteResult = idempotencyKeyRepository.deleteByExpiresAtBefore(now);

            // Then
            StepVerifier.create(deleteResult)
                    .assertNext(deletedCount -> assertThat(deletedCount).isEqualTo(0))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should delete multiple expired keys")
        void shouldDeleteMultipleExpiredKeys() {
            // Given
            Instant now = Instant.now();

            // Create 3 expired keys
            for (int i = 0; i < 3; i++) {
                IdempotencyKeyEntity expiredEntity = createTestEntity("tenant-1", "/api/v1/payments", "expired-key-" + i);
                expiredEntity.setExpiresAt(now.minus(i + 1, ChronoUnit.HOURS));
                idempotencyKeyRepository.save(expiredEntity).block();
            }

            // Create 1 valid key
            IdempotencyKeyEntity validEntity = createTestEntity("tenant-1", "/api/v1/payments", "valid-key");
            validEntity.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
            idempotencyKeyRepository.save(validEntity).block();

            // When
            var deleteResult = idempotencyKeyRepository.deleteByExpiresAtBefore(now);

            // Then - should delete 3 expired keys
            StepVerifier.create(deleteResult)
                    .assertNext(deletedCount -> assertThat(deletedCount).isEqualTo(3))
                    .verifyComplete();
        }
    }
}
