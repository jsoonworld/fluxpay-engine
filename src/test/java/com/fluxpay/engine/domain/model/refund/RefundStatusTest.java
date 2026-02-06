package com.fluxpay.engine.domain.model.refund;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RefundStatus enum.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("RefundStatus")
class RefundStatusTest {

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("REQUESTED should allow transition to PROCESSING")
        void requestedShouldAllowTransitionToProcessing() {
            assertThat(RefundStatus.REQUESTED.canTransitionTo(RefundStatus.PROCESSING)).isTrue();
        }

        @Test
        @DisplayName("REQUESTED should not allow transition to COMPLETED")
        void requestedShouldNotAllowTransitionToCompleted() {
            assertThat(RefundStatus.REQUESTED.canTransitionTo(RefundStatus.COMPLETED)).isFalse();
        }

        @Test
        @DisplayName("REQUESTED should not allow transition to FAILED")
        void requestedShouldNotAllowTransitionToFailed() {
            assertThat(RefundStatus.REQUESTED.canTransitionTo(RefundStatus.FAILED)).isFalse();
        }

        @Test
        @DisplayName("PROCESSING should allow transition to COMPLETED")
        void processingShouldAllowTransitionToCompleted() {
            assertThat(RefundStatus.PROCESSING.canTransitionTo(RefundStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("PROCESSING should allow transition to FAILED")
        void processingShouldAllowTransitionToFailed() {
            assertThat(RefundStatus.PROCESSING.canTransitionTo(RefundStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("PROCESSING should not allow transition to REQUESTED")
        void processingShouldNotAllowTransitionToRequested() {
            assertThat(RefundStatus.PROCESSING.canTransitionTo(RefundStatus.REQUESTED)).isFalse();
        }

        @Test
        @DisplayName("COMPLETED should not allow any transitions")
        void completedShouldNotAllowAnyTransitions() {
            assertThat(RefundStatus.COMPLETED.canTransitionTo(RefundStatus.REQUESTED)).isFalse();
            assertThat(RefundStatus.COMPLETED.canTransitionTo(RefundStatus.PROCESSING)).isFalse();
            assertThat(RefundStatus.COMPLETED.canTransitionTo(RefundStatus.FAILED)).isFalse();
        }

        @Test
        @DisplayName("FAILED should not allow any transitions")
        void failedShouldNotAllowAnyTransitions() {
            assertThat(RefundStatus.FAILED.canTransitionTo(RefundStatus.REQUESTED)).isFalse();
            assertThat(RefundStatus.FAILED.canTransitionTo(RefundStatus.PROCESSING)).isFalse();
            assertThat(RefundStatus.FAILED.canTransitionTo(RefundStatus.COMPLETED)).isFalse();
        }
    }

    @Nested
    @DisplayName("Terminal State")
    class TerminalState {

        @Test
        @DisplayName("REQUESTED should not be terminal")
        void requestedShouldNotBeTerminal() {
            assertThat(RefundStatus.REQUESTED.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PROCESSING should not be terminal")
        void processingShouldNotBeTerminal() {
            assertThat(RefundStatus.PROCESSING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("COMPLETED should be terminal")
        void completedShouldBeTerminal() {
            assertThat(RefundStatus.COMPLETED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("FAILED should be terminal")
        void failedShouldBeTerminal() {
            assertThat(RefundStatus.FAILED.isTerminal()).isTrue();
        }
    }
}
