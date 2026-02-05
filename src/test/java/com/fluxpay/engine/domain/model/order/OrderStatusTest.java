package com.fluxpay.engine.domain.model.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderStatus")
class OrderStatusTest {

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        @DisplayName("should have exactly 5 status values")
        void shouldHaveExactlyFiveStatusValues() {
            assertThat(OrderStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("should contain all expected status values")
        void shouldContainAllExpectedStatusValues() {
            assertThat(OrderStatus.values())
                .containsExactlyInAnyOrder(
                    OrderStatus.PENDING,
                    OrderStatus.PAID,
                    OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED,
                    OrderStatus.FAILED
                );
        }
    }

    @Nested
    @DisplayName("state transitions from PENDING")
    class PendingTransitions {

        @Test
        @DisplayName("should allow transition from PENDING to PAID")
        void shouldAllowTransitionToPaid() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PAID)).isTrue();
        }

        @Test
        @DisplayName("should allow transition from PENDING to CANCELLED")
        void shouldAllowTransitionToCancelled() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("should allow transition from PENDING to FAILED")
        void shouldAllowTransitionToFailed() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("should not allow transition from PENDING to COMPLETED")
        void shouldNotAllowTransitionToCompleted() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.COMPLETED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from PENDING to PENDING")
        void shouldNotAllowTransitionToPending() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING)).isFalse();
        }
    }

    @Nested
    @DisplayName("state transitions from PAID")
    class PaidTransitions {

        @Test
        @DisplayName("should allow transition from PAID to COMPLETED")
        void shouldAllowTransitionToCompleted() {
            assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("should allow transition from PAID to CANCELLED")
        void shouldAllowTransitionToCancelled() {
            assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("should allow transition from PAID to FAILED")
        void shouldAllowTransitionToFailed() {
            assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("should not allow transition from PAID to PENDING")
        void shouldNotAllowTransitionToPending() {
            assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PENDING)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from PAID to PAID")
        void shouldNotAllowTransitionToPaid() {
            assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PAID)).isFalse();
        }
    }

    @Nested
    @DisplayName("state transitions from terminal states")
    class TerminalStateTransitions {

        @ParameterizedTest(name = "COMPLETED should not allow transition to {0}")
        @EnumSource(OrderStatus.class)
        @DisplayName("COMPLETED should not allow any transitions")
        void completedShouldNotAllowAnyTransitions(OrderStatus target) {
            assertThat(OrderStatus.COMPLETED.canTransitionTo(target)).isFalse();
        }

        @ParameterizedTest(name = "CANCELLED should not allow transition to {0}")
        @EnumSource(OrderStatus.class)
        @DisplayName("CANCELLED should not allow any transitions")
        void cancelledShouldNotAllowAnyTransitions(OrderStatus target) {
            assertThat(OrderStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }

        @ParameterizedTest(name = "FAILED should not allow transition to {0}")
        @EnumSource(OrderStatus.class)
        @DisplayName("FAILED should not allow any transitions")
        void failedShouldNotAllowAnyTransitions(OrderStatus target) {
            assertThat(OrderStatus.FAILED.canTransitionTo(target)).isFalse();
        }
    }

    @Nested
    @DisplayName("comprehensive transition matrix")
    class TransitionMatrix {

        @ParameterizedTest(name = "{0} -> {1} should be {2}")
        @MethodSource("provideTransitionCases")
        @DisplayName("should validate all state transition rules")
        void shouldValidateAllStateTransitionRules(OrderStatus from, OrderStatus to, boolean expected) {
            assertThat(from.canTransitionTo(to)).isEqualTo(expected);
        }

        static Stream<Arguments> provideTransitionCases() {
            return Stream.of(
                // PENDING transitions
                Arguments.of(OrderStatus.PENDING, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.PENDING, OrderStatus.PAID, true),
                Arguments.of(OrderStatus.PENDING, OrderStatus.COMPLETED, false),
                Arguments.of(OrderStatus.PENDING, OrderStatus.CANCELLED, true),
                Arguments.of(OrderStatus.PENDING, OrderStatus.FAILED, true),

                // PAID transitions
                Arguments.of(OrderStatus.PAID, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.PAID, OrderStatus.PAID, false),
                Arguments.of(OrderStatus.PAID, OrderStatus.COMPLETED, true),
                Arguments.of(OrderStatus.PAID, OrderStatus.CANCELLED, true),
                Arguments.of(OrderStatus.PAID, OrderStatus.FAILED, true),

                // COMPLETED transitions (terminal state)
                Arguments.of(OrderStatus.COMPLETED, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.COMPLETED, OrderStatus.PAID, false),
                Arguments.of(OrderStatus.COMPLETED, OrderStatus.COMPLETED, false),
                Arguments.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED, false),
                Arguments.of(OrderStatus.COMPLETED, OrderStatus.FAILED, false),

                // CANCELLED transitions (terminal state)
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.PAID, false),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.COMPLETED, false),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.CANCELLED, false),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.FAILED, false),

                // FAILED transitions (terminal state)
                Arguments.of(OrderStatus.FAILED, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.FAILED, OrderStatus.PAID, false),
                Arguments.of(OrderStatus.FAILED, OrderStatus.COMPLETED, false),
                Arguments.of(OrderStatus.FAILED, OrderStatus.CANCELLED, false),
                Arguments.of(OrderStatus.FAILED, OrderStatus.FAILED, false)
            );
        }
    }

    @Nested
    @DisplayName("terminal state check")
    class TerminalStateCheck {

        @Test
        @DisplayName("PENDING should not be terminal")
        void pendingShouldNotBeTerminal() {
            assertThat(OrderStatus.PENDING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PAID should not be terminal")
        void paidShouldNotBeTerminal() {
            assertThat(OrderStatus.PAID.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("COMPLETED should be terminal")
        void completedShouldBeTerminal() {
            assertThat(OrderStatus.COMPLETED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED should be terminal")
        void cancelledShouldBeTerminal() {
            assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("FAILED should be terminal")
        void failedShouldBeTerminal() {
            assertThat(OrderStatus.FAILED.isTerminal()).isTrue();
        }
    }
}
