package com.fluxpay.engine.domain.service;

import com.fluxpay.engine.domain.exception.InvalidPaymentStateException;
import com.fluxpay.engine.domain.exception.PaymentNotFoundException;
import com.fluxpay.engine.domain.exception.PgClientException;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.domain.port.outbound.PaymentRepository;
import com.fluxpay.engine.domain.port.outbound.PgClient;
import com.fluxpay.engine.domain.port.outbound.PgClient.PgApprovalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentService following TDD RED phase.
 * These tests define the expected behavior before implementation.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PgClient pgClient;

    private PaymentService paymentService;

    private OrderId testOrderId;
    private PaymentId testPaymentId;
    private Money testAmount;
    private PaymentMethod testPaymentMethod;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, pgClient);

        testOrderId = OrderId.generate();
        testPaymentId = PaymentId.generate();
        testAmount = Money.of(BigDecimal.valueOf(10000), Currency.KRW);
        testPaymentMethod = PaymentMethod.card("Visa ending in 1234");

        testPayment = Payment.create(testOrderId, testAmount);
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePaymentTests {

        @Test
        @DisplayName("should create payment with READY status")
        void shouldCreatePayment() {
            // Given
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            Mono<Payment> result = paymentService.createPayment(testOrderId, testAmount);

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment).isNotNull();
                    assertThat(payment.getId()).isNotNull();
                    assertThat(payment.getOrderId()).isEqualTo(testOrderId);
                    assertThat(payment.getAmount()).isEqualTo(testAmount);
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
                    assertThat(payment.getCreatedAt()).isNotNull();
                })
                .verifyComplete();

            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPaymentTests {

        @Test
        @DisplayName("should return payment when found")
        void shouldReturnPaymentWhenFound() {
            // Given
            when(paymentRepository.findById(testPayment.getId()))
                .thenReturn(Mono.just(testPayment));

            // When
            Mono<Payment> result = paymentService.getPayment(testPayment.getId());

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment).isNotNull();
                    assertThat(payment.getId()).isEqualTo(testPayment.getId());
                })
                .verifyComplete();

            verify(paymentRepository).findById(testPayment.getId());
        }

        @Test
        @DisplayName("should throw PaymentNotFoundException when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            // Given
            PaymentId nonExistentId = PaymentId.generate();
            when(paymentRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Payment> result = paymentService.getPayment(nonExistentId);

            // Then
            StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(PaymentNotFoundException.class);
                })
                .verify();
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetPaymentByOrderIdTests {

        @Test
        @DisplayName("should return payment when found by order ID")
        void shouldReturnPaymentWhenFoundByOrderId() {
            // Given
            when(paymentRepository.findByOrderId(testOrderId))
                .thenReturn(Mono.just(testPayment));

            // When
            Mono<Payment> result = paymentService.getPaymentByOrderId(testOrderId);

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment).isNotNull();
                    assertThat(payment.getOrderId()).isEqualTo(testOrderId);
                })
                .verifyComplete();

            verify(paymentRepository).findByOrderId(testOrderId);
        }

        @Test
        @DisplayName("should throw PaymentNotFoundException when payment not found by order ID")
        void shouldThrowExceptionWhenPaymentNotFoundByOrderId() {
            // Given
            OrderId nonExistentOrderId = OrderId.generate();
            when(paymentRepository.findByOrderId(nonExistentOrderId)).thenReturn(Mono.empty());

            // When
            Mono<Payment> result = paymentService.getPaymentByOrderId(nonExistentOrderId);

            // Then
            StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(PaymentNotFoundException.class);
                })
                .verify();
        }
    }

    @Nested
    @DisplayName("requestApproval")
    class RequestApprovalTests {

        @Test
        @DisplayName("should request approval successfully and transition to APPROVED")
        void shouldRequestApprovalSuccessfully() {
            // Given
            Payment readyPayment = Payment.create(testOrderId, testAmount);
            String pgTransactionId = "pg_txn_123";

            when(paymentRepository.findById(readyPayment.getId()))
                .thenReturn(Mono.just(readyPayment));
            when(pgClient.requestApproval(
                eq(testOrderId.value().toString()),
                eq(testAmount),
                eq(testPaymentMethod)))
                .thenReturn(Mono.just(new PgApprovalResult(pgTransactionId, "payment_key_123", true, null)));
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            Mono<Payment> result = paymentService.requestApproval(readyPayment.getId(), testPaymentMethod);

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
                    assertThat(payment.getPaymentMethod()).isEqualTo(testPaymentMethod);
                    assertThat(payment.getPgTransactionId()).isEqualTo(pgTransactionId);
                    assertThat(payment.getApprovedAt()).isNotNull();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should handle approval failure from PG")
        void shouldHandleApprovalFailure() {
            // Given
            Payment readyPayment = Payment.create(testOrderId, testAmount);
            String errorMessage = "Insufficient funds";

            when(paymentRepository.findById(readyPayment.getId()))
                .thenReturn(Mono.just(readyPayment));
            when(pgClient.requestApproval(
                eq(testOrderId.value().toString()),
                eq(testAmount),
                eq(testPaymentMethod)))
                .thenReturn(Mono.just(new PgApprovalResult(null, null, false, errorMessage)));
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            Mono<Payment> result = paymentService.requestApproval(readyPayment.getId(), testPaymentMethod);

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(payment.getFailureReason()).isEqualTo(errorMessage);
                    assertThat(payment.isFailed()).isTrue();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should handle PG client exception")
        void shouldHandlePgClientException() {
            // Given
            Payment readyPayment = Payment.create(testOrderId, testAmount);

            when(paymentRepository.findById(readyPayment.getId()))
                .thenReturn(Mono.just(readyPayment));
            when(pgClient.requestApproval(
                eq(testOrderId.value().toString()),
                eq(testAmount),
                eq(testPaymentMethod)))
                .thenReturn(Mono.error(new PgClientException("Connection timeout")));
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            Mono<Payment> result = paymentService.requestApproval(readyPayment.getId(), testPaymentMethod);

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(payment.getFailureReason()).contains("Connection timeout");
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw exception when payment not found for approval")
        void shouldThrowExceptionWhenPaymentNotFoundForApproval() {
            // Given
            PaymentId nonExistentId = PaymentId.generate();
            when(paymentRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Payment> result = paymentService.requestApproval(nonExistentId, testPaymentMethod);

            // Then
            StepVerifier.create(result)
                .expectError(PaymentNotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("should confirm payment transitioning from APPROVED to CONFIRMED")
        void shouldConfirmPayment() {
            // Given
            Payment approvedPayment = Payment.create(testOrderId, testAmount);
            approvedPayment.startProcessing(testPaymentMethod);
            approvedPayment.approve("pg_txn_123", "payment_key_123");

            when(paymentRepository.findById(approvedPayment.getId()))
                .thenReturn(Mono.just(approvedPayment));
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            Mono<Payment> result = paymentService.confirmPayment(approvedPayment.getId());

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
                    assertThat(payment.getConfirmedAt()).isNotNull();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw exception when confirming non-approved payment")
        void shouldThrowExceptionWhenConfirmingNotApprovedPayment() {
            // Given
            Payment readyPayment = Payment.create(testOrderId, testAmount);

            when(paymentRepository.findById(readyPayment.getId()))
                .thenReturn(Mono.just(readyPayment));

            // When
            Mono<Payment> result = paymentService.confirmPayment(readyPayment.getId());

            // Then
            StepVerifier.create(result)
                .expectError(InvalidPaymentStateException.class)
                .verify();
        }

        @Test
        @DisplayName("should throw exception when payment not found for confirmation")
        void shouldThrowExceptionWhenPaymentNotFoundForConfirmation() {
            // Given
            PaymentId nonExistentId = PaymentId.generate();
            when(paymentRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Payment> result = paymentService.confirmPayment(nonExistentId);

            // Then
            StepVerifier.create(result)
                .expectError(PaymentNotFoundException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("failPayment")
    class FailPaymentTests {

        @Test
        @DisplayName("should mark payment as failed with reason")
        void shouldFailPayment() {
            // Given - Payment must be in PROCESSING state to fail
            // (READY can only transition to PROCESSING)
            Payment processingPayment = Payment.create(testOrderId, testAmount);
            processingPayment.startProcessing(testPaymentMethod);
            String failureReason = "User cancelled payment";

            when(paymentRepository.findById(processingPayment.getId()))
                .thenReturn(Mono.just(processingPayment));
            when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When
            Mono<Payment> result = paymentService.failPayment(processingPayment.getId(), failureReason);

            // Then
            StepVerifier.create(result)
                .assertNext(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(payment.getFailureReason()).isEqualTo(failureReason);
                    assertThat(payment.isFailed()).isTrue();
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("should throw exception when failing already failed payment")
        void shouldThrowExceptionWhenFailingAlreadyFailedPayment() {
            // Given - First put in PROCESSING, then fail it
            Payment failedPayment = Payment.create(testOrderId, testAmount);
            failedPayment.startProcessing(testPaymentMethod);
            failedPayment.fail("Initial failure");

            when(paymentRepository.findById(failedPayment.getId()))
                .thenReturn(Mono.just(failedPayment));

            // When
            Mono<Payment> result = paymentService.failPayment(failedPayment.getId(), "Another failure");

            // Then
            StepVerifier.create(result)
                .expectError(InvalidPaymentStateException.class)
                .verify();
        }

        @Test
        @DisplayName("should throw exception when payment not found for failing")
        void shouldThrowExceptionWhenPaymentNotFoundForFailing() {
            // Given
            PaymentId nonExistentId = PaymentId.generate();
            when(paymentRepository.findById(nonExistentId)).thenReturn(Mono.empty());

            // When
            Mono<Payment> result = paymentService.failPayment(nonExistentId, "Some reason");

            // Then
            StepVerifier.create(result)
                .expectError(PaymentNotFoundException.class)
                .verify();
        }
    }
}
