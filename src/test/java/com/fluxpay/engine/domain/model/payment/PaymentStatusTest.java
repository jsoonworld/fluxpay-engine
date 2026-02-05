package com.fluxpay.engine.domain.model.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentStatus")
class PaymentStatusTest {

    @Nested
    @DisplayName("enum values")
    class EnumValues {

        @Test
        @DisplayName("should have exactly 6 status values")
        void shouldHaveExactlySixStatusValues() {
            assertThat(PaymentStatus.values()).hasSize(6);
        }

        @Test
        @DisplayName("should contain all expected status values")
        void shouldContainAllExpectedStatusValues() {
            assertThat(PaymentStatus.values())
                .containsExactlyInAnyOrder(
                    PaymentStatus.READY,
                    PaymentStatus.PROCESSING,
                    PaymentStatus.APPROVED,
                    PaymentStatus.CONFIRMED,
                    PaymentStatus.FAILED,
                    PaymentStatus.REFUNDED
                );
        }
    }

    @Nested
    @DisplayName("state transitions from READY")
    class ReadyTransitions {

        @Test
        @DisplayName("should allow transition from READY to PROCESSING")
        void shouldAllowTransitionToProcessing() {
            assertThat(PaymentStatus.READY.canTransitionTo(PaymentStatus.PROCESSING)).isTrue();
        }

        @Test
        @DisplayName("should not allow transition from READY to APPROVED")
        void shouldNotAllowTransitionToApproved() {
            assertThat(PaymentStatus.READY.canTransitionTo(PaymentStatus.APPROVED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from READY to CONFIRMED")
        void shouldNotAllowTransitionToConfirmed() {
            assertThat(PaymentStatus.READY.canTransitionTo(PaymentStatus.CONFIRMED)).isFalse();
        }

        @Test
        @DisplayName("should allow transition from READY to FAILED")
        void shouldAllowTransitionToFailed() {
            assertThat(PaymentStatus.READY.canTransitionTo(PaymentStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("should not allow transition from READY to REFUNDED")
        void shouldNotAllowTransitionToRefunded() {
            assertThat(PaymentStatus.READY.canTransitionTo(PaymentStatus.REFUNDED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from READY to READY")
        void shouldNotAllowTransitionToReady() {
            assertThat(PaymentStatus.READY.canTransitionTo(PaymentStatus.READY)).isFalse();
        }
    }

    @Nested
    @DisplayName("state transitions from PROCESSING")
    class ProcessingTransitions {

        @Test
        @DisplayName("should allow transition from PROCESSING to APPROVED")
        void shouldAllowTransitionToApproved() {
            assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.APPROVED)).isTrue();
        }

        @Test
        @DisplayName("should allow transition from PROCESSING to FAILED")
        void shouldAllowTransitionToFailed() {
            assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("should not allow transition from PROCESSING to READY")
        void shouldNotAllowTransitionToReady() {
            assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.READY)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from PROCESSING to CONFIRMED")
        void shouldNotAllowTransitionToConfirmed() {
            assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.CONFIRMED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from PROCESSING to REFUNDED")
        void shouldNotAllowTransitionToRefunded() {
            assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.REFUNDED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from PROCESSING to PROCESSING")
        void shouldNotAllowTransitionToProcessing() {
            assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.PROCESSING)).isFalse();
        }
    }

    @Nested
    @DisplayName("state transitions from APPROVED")
    class ApprovedTransitions {

        @Test
        @DisplayName("should allow transition from APPROVED to CONFIRMED")
        void shouldAllowTransitionToConfirmed() {
            assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.CONFIRMED)).isTrue();
        }

        @Test
        @DisplayName("should allow transition from APPROVED to FAILED")
        void shouldAllowTransitionToFailed() {
            assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("should not allow transition from APPROVED to READY")
        void shouldNotAllowTransitionToReady() {
            assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.READY)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from APPROVED to PROCESSING")
        void shouldNotAllowTransitionToProcessing() {
            assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.PROCESSING)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from APPROVED to REFUNDED")
        void shouldNotAllowTransitionToRefunded() {
            assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.REFUNDED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from APPROVED to APPROVED")
        void shouldNotAllowTransitionToApproved() {
            assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.APPROVED)).isFalse();
        }
    }

    @Nested
    @DisplayName("state transitions from CONFIRMED")
    class ConfirmedTransitions {

        @Test
        @DisplayName("should allow transition from CONFIRMED to REFUNDED")
        void shouldAllowTransitionToRefunded() {
            assertThat(PaymentStatus.CONFIRMED.canTransitionTo(PaymentStatus.REFUNDED)).isTrue();
        }

        @Test
        @DisplayName("should not allow transition from CONFIRMED to READY")
        void shouldNotAllowTransitionToReady() {
            assertThat(PaymentStatus.CONFIRMED.canTransitionTo(PaymentStatus.READY)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from CONFIRMED to PROCESSING")
        void shouldNotAllowTransitionToProcessing() {
            assertThat(PaymentStatus.CONFIRMED.canTransitionTo(PaymentStatus.PROCESSING)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from CONFIRMED to APPROVED")
        void shouldNotAllowTransitionToApproved() {
            assertThat(PaymentStatus.CONFIRMED.canTransitionTo(PaymentStatus.APPROVED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from CONFIRMED to CONFIRMED")
        void shouldNotAllowTransitionToConfirmed() {
            assertThat(PaymentStatus.CONFIRMED.canTransitionTo(PaymentStatus.CONFIRMED)).isFalse();
        }

        @Test
        @DisplayName("should not allow transition from CONFIRMED to FAILED")
        void shouldNotAllowTransitionToFailed() {
            assertThat(PaymentStatus.CONFIRMED.canTransitionTo(PaymentStatus.FAILED)).isFalse();
        }
    }

    @Nested
    @DisplayName("state transitions from terminal states")
    class TerminalStateTransitions {

        @ParameterizedTest(name = "FAILED should not allow transition to {0}")
        @EnumSource(PaymentStatus.class)
        @DisplayName("FAILED should not allow any transitions")
        void failedShouldNotAllowAnyTransitions(PaymentStatus target) {
            assertThat(PaymentStatus.FAILED.canTransitionTo(target)).isFalse();
        }

        @ParameterizedTest(name = "REFUNDED should not allow transition to {0}")
        @EnumSource(PaymentStatus.class)
        @DisplayName("REFUNDED should not allow any transitions")
        void refundedShouldNotAllowAnyTransitions(PaymentStatus target) {
            assertThat(PaymentStatus.REFUNDED.canTransitionTo(target)).isFalse();
        }
    }

    @Nested
    @DisplayName("comprehensive transition matrix")
    class TransitionMatrix {

        @ParameterizedTest(name = "{0} -> {1} should be {2}")
        @MethodSource("provideTransitionCases")
        @DisplayName("should validate all state transition rules")
        void shouldValidateAllStateTransitionRules(PaymentStatus from, PaymentStatus to, boolean expected) {
            assertThat(from.canTransitionTo(to)).isEqualTo(expected);
        }

        static Stream<Arguments> provideTransitionCases() {
            return Stream.of(
                // READY transitions
                Arguments.of(PaymentStatus.READY, PaymentStatus.READY, false),
                Arguments.of(PaymentStatus.READY, PaymentStatus.PROCESSING, true),
                Arguments.of(PaymentStatus.READY, PaymentStatus.APPROVED, false),
                Arguments.of(PaymentStatus.READY, PaymentStatus.CONFIRMED, false),
                Arguments.of(PaymentStatus.READY, PaymentStatus.FAILED, true),
                Arguments.of(PaymentStatus.READY, PaymentStatus.REFUNDED, false),

                // PROCESSING transitions
                Arguments.of(PaymentStatus.PROCESSING, PaymentStatus.READY, false),
                Arguments.of(PaymentStatus.PROCESSING, PaymentStatus.PROCESSING, false),
                Arguments.of(PaymentStatus.PROCESSING, PaymentStatus.APPROVED, true),
                Arguments.of(PaymentStatus.PROCESSING, PaymentStatus.CONFIRMED, false),
                Arguments.of(PaymentStatus.PROCESSING, PaymentStatus.FAILED, true),
                Arguments.of(PaymentStatus.PROCESSING, PaymentStatus.REFUNDED, false),

                // APPROVED transitions
                Arguments.of(PaymentStatus.APPROVED, PaymentStatus.READY, false),
                Arguments.of(PaymentStatus.APPROVED, PaymentStatus.PROCESSING, false),
                Arguments.of(PaymentStatus.APPROVED, PaymentStatus.APPROVED, false),
                Arguments.of(PaymentStatus.APPROVED, PaymentStatus.CONFIRMED, true),
                Arguments.of(PaymentStatus.APPROVED, PaymentStatus.FAILED, true),
                Arguments.of(PaymentStatus.APPROVED, PaymentStatus.REFUNDED, false),

                // CONFIRMED transitions
                Arguments.of(PaymentStatus.CONFIRMED, PaymentStatus.READY, false),
                Arguments.of(PaymentStatus.CONFIRMED, PaymentStatus.PROCESSING, false),
                Arguments.of(PaymentStatus.CONFIRMED, PaymentStatus.APPROVED, false),
                Arguments.of(PaymentStatus.CONFIRMED, PaymentStatus.CONFIRMED, false),
                Arguments.of(PaymentStatus.CONFIRMED, PaymentStatus.FAILED, false),
                Arguments.of(PaymentStatus.CONFIRMED, PaymentStatus.REFUNDED, true),

                // FAILED transitions (terminal state)
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.READY, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.PROCESSING, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.APPROVED, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.CONFIRMED, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.FAILED, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.REFUNDED, false),

                // REFUNDED transitions (terminal state)
                Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.READY, false),
                Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.PROCESSING, false),
                Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.APPROVED, false),
                Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.CONFIRMED, false),
                Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.FAILED, false),
                Arguments.of(PaymentStatus.REFUNDED, PaymentStatus.REFUNDED, false)
            );
        }
    }

    @Nested
    @DisplayName("terminal state check")
    class TerminalStateCheck {

        @Test
        @DisplayName("READY should not be terminal")
        void readyShouldNotBeTerminal() {
            assertThat(PaymentStatus.READY.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PROCESSING should not be terminal")
        void processingShouldNotBeTerminal() {
            assertThat(PaymentStatus.PROCESSING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("APPROVED should not be terminal")
        void approvedShouldNotBeTerminal() {
            assertThat(PaymentStatus.APPROVED.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("CONFIRMED should not be terminal since it can transition to REFUNDED")
        void confirmedShouldNotBeTerminal() {
            assertThat(PaymentStatus.CONFIRMED.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("FAILED should be terminal")
        void failedShouldBeTerminal() {
            assertThat(PaymentStatus.FAILED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("REFUNDED should be terminal")
        void refundedShouldBeTerminal() {
            assertThat(PaymentStatus.REFUNDED.isTerminal()).isTrue();
        }
    }

    @Nested
    @DisplayName("canBeConfirmed check")
    class CanBeConfirmedCheck {

        @Test
        @DisplayName("READY cannot be confirmed")
        void readyCannotBeConfirmed() {
            assertThat(PaymentStatus.READY.canBeConfirmed()).isFalse();
        }

        @Test
        @DisplayName("PROCESSING cannot be confirmed")
        void processingCannotBeConfirmed() {
            assertThat(PaymentStatus.PROCESSING.canBeConfirmed()).isFalse();
        }

        @Test
        @DisplayName("APPROVED can be confirmed")
        void approvedCanBeConfirmed() {
            assertThat(PaymentStatus.APPROVED.canBeConfirmed()).isTrue();
        }

        @Test
        @DisplayName("CONFIRMED cannot be confirmed again")
        void confirmedCannotBeConfirmedAgain() {
            assertThat(PaymentStatus.CONFIRMED.canBeConfirmed()).isFalse();
        }

        @Test
        @DisplayName("FAILED cannot be confirmed")
        void failedCannotBeConfirmed() {
            assertThat(PaymentStatus.FAILED.canBeConfirmed()).isFalse();
        }

        @Test
        @DisplayName("REFUNDED cannot be confirmed")
        void refundedCannotBeConfirmed() {
            assertThat(PaymentStatus.REFUNDED.canBeConfirmed()).isFalse();
        }
    }
}
