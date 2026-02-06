package com.fluxpay.engine.domain.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SagaStatus")
class SagaStatusTest {

    @Test
    @DisplayName("should have all required status values")
    void shouldHaveAllRequiredStatusValues() {
        SagaStatus[] values = SagaStatus.values();

        assertThat(values).containsExactlyInAnyOrder(
            SagaStatus.STARTED,
            SagaStatus.PROCESSING,
            SagaStatus.COMPLETED,
            SagaStatus.COMPENSATING,
            SagaStatus.COMPENSATED,
            SagaStatus.FAILED
        );
    }

    @Test
    @DisplayName("STARTED should transition to PROCESSING")
    void startedShouldTransitionToProcessing() {
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.PROCESSING)).isTrue();
    }

    @Test
    @DisplayName("STARTED should not transition to COMPLETED")
    void startedShouldNotTransitionToCompleted() {
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("PROCESSING should transition to COMPLETED")
    void processingShouldTransitionToCompleted() {
        assertThat(SagaStatus.PROCESSING.canTransitionTo(SagaStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("PROCESSING should transition to COMPENSATING")
    void processingShouldTransitionToCompensating() {
        assertThat(SagaStatus.PROCESSING.canTransitionTo(SagaStatus.COMPENSATING)).isTrue();
    }

    @Test
    @DisplayName("COMPENSATING should transition to COMPENSATED")
    void compensatingShouldTransitionToCompensated() {
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.COMPENSATED)).isTrue();
    }

    @Test
    @DisplayName("COMPENSATING should transition to FAILED")
    void compensatingShouldTransitionToFailed() {
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.FAILED)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SagaStatus.class, names = {"COMPLETED", "COMPENSATED", "FAILED"})
    @DisplayName("Terminal states should not transition to any other state")
    void terminalStatesShouldNotTransition(SagaStatus terminalStatus) {
        for (SagaStatus target : SagaStatus.values()) {
            if (target != terminalStatus) {
                assertThat(terminalStatus.canTransitionTo(target))
                    .as("Terminal status %s should not transition to %s", terminalStatus, target)
                    .isFalse();
            }
        }
    }

    @Test
    @DisplayName("isTerminal should return true for terminal states")
    void isTerminalShouldReturnTrueForTerminalStates() {
        assertThat(SagaStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(SagaStatus.COMPENSATED.isTerminal()).isTrue();
        assertThat(SagaStatus.FAILED.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("isTerminal should return false for non-terminal states")
    void isTerminalShouldReturnFalseForNonTerminalStates() {
        assertThat(SagaStatus.STARTED.isTerminal()).isFalse();
        assertThat(SagaStatus.PROCESSING.isTerminal()).isFalse();
        assertThat(SagaStatus.COMPENSATING.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("isActive should return true for active states")
    void isActiveShouldReturnTrueForActiveStates() {
        assertThat(SagaStatus.STARTED.isActive()).isTrue();
        assertThat(SagaStatus.PROCESSING.isActive()).isTrue();
        assertThat(SagaStatus.COMPENSATING.isActive()).isTrue();
    }

    @Test
    @DisplayName("isActive should return false for terminal states")
    void isActiveShouldReturnFalseForTerminalStates() {
        assertThat(SagaStatus.COMPLETED.isActive()).isFalse();
        assertThat(SagaStatus.COMPENSATED.isActive()).isFalse();
        assertThat(SagaStatus.FAILED.isActive()).isFalse();
    }
}
