package com.fluxpay.engine.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * R2DBC entity representing an idempotency key in the database.
 * Used as a fallback persistence layer when Redis is unavailable.
 */
@Table("idempotency_keys")
public class IdempotencyKeyEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Transient
    private boolean isNew = true;

    @Column("tenant_id")
    private String tenantId;

    @Column("endpoint")
    private String endpoint;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("payload_hash")
    private String payloadHash;

    @Column("response")
    private String response;

    @Column("http_status")
    private int httpStatus;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;

    // Default constructor for R2DBC
    public IdempotencyKeyEntity() {
    }

    // Getters and Setters

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * Mark the entity as not new (for updates).
     */
    public IdempotencyKeyEntity markAsExisting() {
        this.isNew = false;
        return this;
    }
}
