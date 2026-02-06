package com.fluxpay.engine.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * R2DBC entity for refunds table.
 */
@Table("refunds")
public class RefundEntity {

    @Id
    private Long id;

    @Column("refund_id")
    private String refundId;

    @Column("tenant_id")
    private String tenantId;

    @Column("payment_id")
    private String paymentId;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("reason")
    private String reason;

    @Column("status")
    private String status;

    @Column("pg_refund_id")
    private String pgRefundId;

    @Column("requested_at")
    private Instant requestedAt;

    @Column("completed_at")
    private Instant completedAt;

    @Column("error_message")
    private String errorMessage;

    // Default constructor for R2DBC
    public RefundEntity() {
    }

    // All-args constructor
    public RefundEntity(Long id, String refundId, String tenantId, String paymentId,
                        BigDecimal amount, String currency, String reason, String status,
                        String pgRefundId, Instant requestedAt, Instant completedAt,
                        String errorMessage) {
        this.id = id;
        this.refundId = refundId;
        this.tenantId = tenantId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.status = status;
        this.pgRefundId = pgRefundId;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPgRefundId() {
        return pgRefundId;
    }

    public void setPgRefundId(String pgRefundId) {
        this.pgRefundId = pgRefundId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
