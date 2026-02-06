package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.model.webhook.Webhook;
import com.fluxpay.engine.domain.model.webhook.WebhookId;
import com.fluxpay.engine.domain.model.webhook.WebhookStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WebhookRepository interface contract.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("WebhookRepository")
class WebhookRepositoryTest {

    @Test
    @DisplayName("should define save method that returns saved Webhook")
    void shouldDefineSaveMethod() {
        WebhookRepository repository = new MockWebhookRepository();
        Webhook webhook = Webhook.create("payment.confirmed", "{\"id\":\"123\"}", "https://example.com/webhook");

        StepVerifier.create(repository.save(webhook))
            .assertNext(saved -> {
                assertThat(saved).isNotNull();
                assertThat(saved.getId()).isEqualTo(webhook.getId());
                assertThat(saved.getEventType()).isEqualTo("payment.confirmed");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findById method")
    void shouldDefineFindByIdMethod() {
        MockWebhookRepository repository = new MockWebhookRepository();
        Webhook webhook = Webhook.create("payment.confirmed", "{\"id\":\"123\"}", "https://example.com/webhook");
        repository.save(webhook).block();

        StepVerifier.create(repository.findById(webhook.getId()))
            .assertNext(found -> assertThat(found.getId()).isEqualTo(webhook.getId()))
            .verifyComplete();
    }

    @Test
    @DisplayName("should return empty Mono when webhook not found by ID")
    void shouldReturnEmptyMonoWhenWebhookNotFound() {
        WebhookRepository repository = new MockWebhookRepository();
        WebhookId nonExistentId = WebhookId.generate();

        StepVerifier.create(repository.findById(nonExistentId))
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findByStatus method")
    void shouldDefineFindByStatusMethod() {
        MockWebhookRepository repository = new MockWebhookRepository();
        Webhook webhook1 = Webhook.create("payment.confirmed", "{\"id\":\"1\"}", "https://example.com/webhook");
        Webhook webhook2 = Webhook.create("refund.completed", "{\"id\":\"2\"}", "https://example.com/webhook");
        repository.save(webhook1).block();
        repository.save(webhook2).block();

        StepVerifier.create(repository.findByStatus(WebhookStatus.PENDING).collectList())
            .assertNext(webhooks -> {
                assertThat(webhooks).hasSize(2);
                assertThat(webhooks).allMatch(w -> w.getStatus() == WebhookStatus.PENDING);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findPendingRetries method")
    void shouldDefineFindPendingRetriesMethod() {
        MockWebhookRepository repository = new MockWebhookRepository();

        // Create a webhook with next retry scheduled in the past
        Webhook webhook1 = Webhook.create("payment.confirmed", "{\"id\":\"1\"}", "https://example.com/webhook");
        webhook1.startSending();
        webhook1.recordFailedAttempt("Connection timeout", Instant.now().minusSeconds(60));
        repository.save(webhook1).block();

        // Create a webhook with next retry scheduled in the future
        Webhook webhook2 = Webhook.create("refund.completed", "{\"id\":\"2\"}", "https://example.com/webhook");
        webhook2.startSending();
        webhook2.recordFailedAttempt("Connection timeout", Instant.now().plusSeconds(3600));
        repository.save(webhook2).block();

        // Create a delivered webhook (should not appear)
        Webhook webhook3 = Webhook.create("order.created", "{\"id\":\"3\"}", "https://example.com/webhook");
        webhook3.startSending();
        webhook3.markDelivered();
        repository.save(webhook3).block();

        Instant cutoff = Instant.now();

        StepVerifier.create(repository.findPendingRetries(cutoff).collectList())
            .assertNext(webhooks -> {
                assertThat(webhooks).hasSize(1);
                assertThat(webhooks.get(0).getId()).isEqualTo(webhook1.getId());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("should define delete method")
    void shouldDefineDeleteMethod() {
        MockWebhookRepository repository = new MockWebhookRepository();
        Webhook webhook = Webhook.create("payment.confirmed", "{\"id\":\"123\"}", "https://example.com/webhook");
        repository.save(webhook).block();

        // Delete the webhook
        StepVerifier.create(repository.delete(webhook.getId()))
            .verifyComplete();

        // Verify it's deleted
        StepVerifier.create(repository.findById(webhook.getId()))
            .verifyComplete();
    }

    @Test
    @DisplayName("should return empty flux when no webhooks match status")
    void shouldReturnEmptyFluxWhenNoWebhooksMatchStatus() {
        MockWebhookRepository repository = new MockWebhookRepository();
        Webhook webhook = Webhook.create("payment.confirmed", "{\"id\":\"1\"}", "https://example.com/webhook");
        webhook.startSending();
        webhook.markDelivered();
        repository.save(webhook).block();

        StepVerifier.create(repository.findByStatus(WebhookStatus.FAILED).collectList())
            .assertNext(webhooks -> assertThat(webhooks).isEmpty())
            .verifyComplete();
    }

    @Test
    @DisplayName("should return empty flux when no pending retries before cutoff")
    void shouldReturnEmptyFluxWhenNoPendingRetriesBeforeCutoff() {
        MockWebhookRepository repository = new MockWebhookRepository();

        // Webhook with retry scheduled in the future
        Webhook webhook = Webhook.create("payment.confirmed", "{\"id\":\"1\"}", "https://example.com/webhook");
        webhook.startSending();
        webhook.recordFailedAttempt("Error", Instant.now().plusSeconds(3600));
        repository.save(webhook).block();

        StepVerifier.create(repository.findPendingRetries(Instant.now()).collectList())
            .assertNext(webhooks -> assertThat(webhooks).isEmpty())
            .verifyComplete();
    }

    /**
     * Mock implementation for testing the interface contract.
     */
    private static class MockWebhookRepository implements WebhookRepository {
        private final Map<WebhookId, Webhook> storage = new HashMap<>();

        @Override
        public Mono<Webhook> save(Webhook webhook) {
            storage.put(webhook.getId(), webhook);
            return Mono.just(webhook);
        }

        @Override
        public Mono<Webhook> findById(WebhookId id) {
            Webhook webhook = storage.get(id);
            return webhook != null ? Mono.just(webhook) : Mono.empty();
        }

        @Override
        public Flux<Webhook> findByStatus(WebhookStatus status) {
            return Flux.fromIterable(storage.values())
                .filter(w -> w.getStatus() == status);
        }

        @Override
        public Flux<Webhook> findPendingRetries(Instant before) {
            return Flux.fromIterable(storage.values())
                .filter(w -> w.getStatus() == WebhookStatus.RETRYING)
                .filter(w -> w.getNextRetryAt() != null && w.getNextRetryAt().isBefore(before));
        }

        @Override
        public Mono<Void> delete(WebhookId id) {
            storage.remove(id);
            return Mono.empty();
        }
    }
}
