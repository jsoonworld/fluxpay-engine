package com.fluxpay.engine.domain.model.webhook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WebhookStatus enum.
 * Following TDD: RED -> GREEN -> REFACTOR
 *
 * State transitions:
 * PENDING -> SENDING
 * SENDING -> DELIVERED | FAILED | RETRYING
 * RETRYING -> SENDING
 */
@DisplayName("WebhookStatus")
class WebhookStatusTest {

    @Nested
    @DisplayName("State Transitions from PENDING")
    class PendingStateTransitions {

        @Test
        @DisplayName("PENDING should allow transition to SENDING")
        void pendingShouldAllowTransitionToSending() {
            assertThat(WebhookStatus.PENDING.canTransitionTo(WebhookStatus.SENDING)).isTrue();
        }

        @Test
        @DisplayName("PENDING should not allow transition to DELIVERED")
        void pendingShouldNotAllowTransitionToDelivered() {
            assertThat(WebhookStatus.PENDING.canTransitionTo(WebhookStatus.DELIVERED)).isFalse();
        }

        @Test
        @DisplayName("PENDING should not allow transition to FAILED")
        void pendingShouldNotAllowTransitionToFailed() {
            assertThat(WebhookStatus.PENDING.canTransitionTo(WebhookStatus.FAILED)).isFalse();
        }

        @Test
        @DisplayName("PENDING should not allow transition to RETRYING")
        void pendingShouldNotAllowTransitionToRetrying() {
            assertThat(WebhookStatus.PENDING.canTransitionTo(WebhookStatus.RETRYING)).isFalse();
        }
    }

    @Nested
    @DisplayName("State Transitions from SENDING")
    class SendingStateTransitions {

        @Test
        @DisplayName("SENDING should allow transition to DELIVERED")
        void sendingShouldAllowTransitionToDelivered() {
            assertThat(WebhookStatus.SENDING.canTransitionTo(WebhookStatus.DELIVERED)).isTrue();
        }

        @Test
        @DisplayName("SENDING should allow transition to FAILED")
        void sendingShouldAllowTransitionToFailed() {
            assertThat(WebhookStatus.SENDING.canTransitionTo(WebhookStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("SENDING should allow transition to RETRYING")
        void sendingShouldAllowTransitionToRetrying() {
            assertThat(WebhookStatus.SENDING.canTransitionTo(WebhookStatus.RETRYING)).isTrue();
        }

        @Test
        @DisplayName("SENDING should not allow transition to PENDING")
        void sendingShouldNotAllowTransitionToPending() {
            assertThat(WebhookStatus.SENDING.canTransitionTo(WebhookStatus.PENDING)).isFalse();
        }
    }

    @Nested
    @DisplayName("State Transitions from RETRYING")
    class RetryingStateTransitions {

        @Test
        @DisplayName("RETRYING should allow transition to SENDING")
        void retryingShouldAllowTransitionToSending() {
            assertThat(WebhookStatus.RETRYING.canTransitionTo(WebhookStatus.SENDING)).isTrue();
        }

        @Test
        @DisplayName("RETRYING should not allow transition to DELIVERED")
        void retryingShouldNotAllowTransitionToDelivered() {
            assertThat(WebhookStatus.RETRYING.canTransitionTo(WebhookStatus.DELIVERED)).isFalse();
        }

        @Test
        @DisplayName("RETRYING should not allow transition to FAILED")
        void retryingShouldNotAllowTransitionToFailed() {
            assertThat(WebhookStatus.RETRYING.canTransitionTo(WebhookStatus.FAILED)).isFalse();
        }

        @Test
        @DisplayName("RETRYING should not allow transition to PENDING")
        void retryingShouldNotAllowTransitionToPending() {
            assertThat(WebhookStatus.RETRYING.canTransitionTo(WebhookStatus.PENDING)).isFalse();
        }
    }

    @Nested
    @DisplayName("State Transitions from Terminal States")
    class TerminalStateTransitions {

        @Test
        @DisplayName("DELIVERED should not allow any transitions")
        void deliveredShouldNotAllowAnyTransitions() {
            assertThat(WebhookStatus.DELIVERED.canTransitionTo(WebhookStatus.PENDING)).isFalse();
            assertThat(WebhookStatus.DELIVERED.canTransitionTo(WebhookStatus.SENDING)).isFalse();
            assertThat(WebhookStatus.DELIVERED.canTransitionTo(WebhookStatus.FAILED)).isFalse();
            assertThat(WebhookStatus.DELIVERED.canTransitionTo(WebhookStatus.RETRYING)).isFalse();
        }

        @Test
        @DisplayName("FAILED should not allow any transitions")
        void failedShouldNotAllowAnyTransitions() {
            assertThat(WebhookStatus.FAILED.canTransitionTo(WebhookStatus.PENDING)).isFalse();
            assertThat(WebhookStatus.FAILED.canTransitionTo(WebhookStatus.SENDING)).isFalse();
            assertThat(WebhookStatus.FAILED.canTransitionTo(WebhookStatus.DELIVERED)).isFalse();
            assertThat(WebhookStatus.FAILED.canTransitionTo(WebhookStatus.RETRYING)).isFalse();
        }
    }

    @Nested
    @DisplayName("Terminal State")
    class TerminalState {

        @Test
        @DisplayName("PENDING should not be terminal")
        void pendingShouldNotBeTerminal() {
            assertThat(WebhookStatus.PENDING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("SENDING should not be terminal")
        void sendingShouldNotBeTerminal() {
            assertThat(WebhookStatus.SENDING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("RETRYING should not be terminal")
        void retryingShouldNotBeTerminal() {
            assertThat(WebhookStatus.RETRYING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("DELIVERED should be terminal")
        void deliveredShouldBeTerminal() {
            assertThat(WebhookStatus.DELIVERED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("FAILED should be terminal")
        void failedShouldBeTerminal() {
            assertThat(WebhookStatus.FAILED.isTerminal()).isTrue();
        }
    }
}
