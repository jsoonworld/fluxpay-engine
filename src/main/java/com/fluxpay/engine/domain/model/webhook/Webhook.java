package com.fluxpay.engine.domain.model.webhook;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a Webhook delivery attempt.
 * Tracks the webhook lifecycle from creation through delivery or failure.
 *
 * State transitions:
 * PENDING -> SENDING (startSending)
 * SENDING -> DELIVERED (markDelivered)
 * SENDING -> FAILED (markFailed)
 * SENDING -> RETRYING (recordFailedAttempt)
 * RETRYING -> SENDING (startSending)
 */
public class Webhook {

    private static final int DEFAULT_MAX_RETRIES = 5;

    private final WebhookId id;
    private final String eventType;
    private final String payload;
    private final String targetUrl;
    private WebhookStatus status;
    private int retryCount;
    private final int maxRetries;
    private Instant lastAttemptAt;
    private Instant nextRetryAt;
    private String lastError;
    private final Instant createdAt;
    private Instant deliveredAt;

    private Webhook(WebhookId id, String eventType, String payload, String targetUrl,
                    WebhookStatus status, int retryCount, int maxRetries,
                    Instant lastAttemptAt, Instant nextRetryAt, String lastError,
                    Instant createdAt, Instant deliveredAt) {
        this.id = Objects.requireNonNull(id, "Webhook ID is required");
        this.eventType = Objects.requireNonNull(eventType, "Event type is required");
        this.payload = Objects.requireNonNull(payload, "Payload is required");
        this.targetUrl = Objects.requireNonNull(targetUrl, "Target URL is required");
        this.status = Objects.requireNonNull(status, "Status is required");
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt is required");
        this.deliveredAt = deliveredAt;
    }

    /**
     * Creates a new Webhook in PENDING status.
     *
     * @param eventType the event type (e.g., payment.confirmed)
     * @param payload   the JSON payload to deliver
     * @param targetUrl the URL to deliver the webhook to
     * @return a new Webhook in PENDING status
     */
    public static Webhook create(String eventType, String payload, String targetUrl) {
        return create(eventType, payload, targetUrl, DEFAULT_MAX_RETRIES);
    }

    /**
     * Creates a new Webhook in PENDING status with custom max retries.
     *
     * @param eventType  the event type (e.g., payment.confirmed)
     * @param payload    the JSON payload to deliver
     * @param targetUrl  the URL to deliver the webhook to
     * @param maxRetries maximum number of retry attempts
     * @return a new Webhook in PENDING status
     */
    public static Webhook create(String eventType, String payload, String targetUrl, int maxRetries) {
        Objects.requireNonNull(eventType, "Event type is required");
        Objects.requireNonNull(payload, "Payload is required");
        Objects.requireNonNull(targetUrl, "Target URL is required");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }

        return new Webhook(
            WebhookId.generate(),
            eventType,
            payload,
            targetUrl,
            WebhookStatus.PENDING,
            0,
            maxRetries,
            null,
            null,
            null,
            Instant.now(),
            null
        );
    }

    /**
     * Restores a Webhook from persistence with all fields.
     */
    public static Webhook restore(WebhookId id, String eventType, String payload, String targetUrl,
                                   WebhookStatus status, int retryCount, int maxRetries,
                                   Instant lastAttemptAt, Instant nextRetryAt, String lastError,
                                   Instant createdAt, Instant deliveredAt) {
        return new Webhook(id, eventType, payload, targetUrl, status, retryCount, maxRetries,
            lastAttemptAt, nextRetryAt, lastError, createdAt, deliveredAt);
    }

    /**
     * Starts a delivery attempt.
     * Transitions from PENDING or RETRYING to SENDING.
     *
     * @throws InvalidWebhookStateException if transition is not allowed
     */
    public void startSending() {
        validateStateTransition(WebhookStatus.SENDING);
        this.status = WebhookStatus.SENDING;
        this.lastAttemptAt = Instant.now();
    }

    /**
     * Marks the webhook as successfully delivered.
     * Transitions from SENDING to DELIVERED.
     *
     * @throws InvalidWebhookStateException if transition is not allowed
     */
    public void markDelivered() {
        validateStateTransition(WebhookStatus.DELIVERED);
        this.status = WebhookStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        this.lastAttemptAt = this.deliveredAt;
    }

    /**
     * Marks the webhook as failed after exhausting retries or permanent failure.
     * Transitions from SENDING to FAILED.
     *
     * @param errorMessage the failure reason
     * @throws InvalidWebhookStateException if transition is not allowed
     */
    public void markFailed(String errorMessage) {
        validateStateTransition(WebhookStatus.FAILED);
        this.lastError = errorMessage;
        this.status = WebhookStatus.FAILED;
        this.lastAttemptAt = Instant.now();
    }

    /**
     * Records a failed delivery attempt and schedules retry.
     * Transitions from SENDING to RETRYING.
     * Increments retry count and sets next retry time.
     *
     * @param errorMessage the failure reason
     * @param nextRetryAt  when to retry next
     * @throws InvalidWebhookStateException if transition is not allowed
     */
    public void recordFailedAttempt(String errorMessage, Instant nextRetryAt) {
        validateStateTransition(WebhookStatus.RETRYING);
        this.status = WebhookStatus.RETRYING;
        this.retryCount++;
        this.lastError = errorMessage;
        this.lastAttemptAt = Instant.now();
        this.nextRetryAt = nextRetryAt;
    }

    /**
     * Checks if retry is allowed based on current state, retry count and max retries.
     */
    public boolean canRetry() {
        return (status == WebhookStatus.SENDING || status == WebhookStatus.RETRYING)
            && retryCount < maxRetries;
    }

    private void validateStateTransition(WebhookStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw new InvalidWebhookStateException(status, targetStatus);
        }
    }

    // Getters

    public WebhookId getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public WebhookStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Webhook webhook = (Webhook) o;
        return Objects.equals(id, webhook.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Webhook[id=" + id + ", eventType=" + eventType + ", status=" + status +
            ", retryCount=" + retryCount + "/" + maxRetries + "]";
    }
}
