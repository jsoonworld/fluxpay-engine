package com.fluxpay.engine.domain.port.outbound;

import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.refund.Refund;
import com.fluxpay.engine.domain.model.refund.RefundId;
import com.fluxpay.engine.domain.model.refund.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RefundRepository interface contract.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("RefundRepository")
class RefundRepositoryTest {

    @Test
    @DisplayName("should define save method that returns saved Refund")
    void shouldDefineSaveMethod() {
        RefundRepository repository = new MockRefundRepository();
        PaymentId paymentId = PaymentId.generate();
        Refund refund = Refund.create(paymentId, Money.krw(5000), "Test refund");

        StepVerifier.create(repository.save(refund))
            .assertNext(saved -> {
                assertThat(saved).isNotNull();
                assertThat(saved.getId()).isEqualTo(refund.getId());
                assertThat(saved.getPaymentId()).isEqualTo(paymentId);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findById method")
    void shouldDefineFindByIdMethod() {
        MockRefundRepository repository = new MockRefundRepository();
        PaymentId paymentId = PaymentId.generate();
        Refund refund = Refund.create(paymentId, Money.krw(5000), "Test refund");
        repository.save(refund).block();

        StepVerifier.create(repository.findById(refund.getId()))
            .assertNext(found -> assertThat(found.getId()).isEqualTo(refund.getId()))
            .verifyComplete();
    }

    @Test
    @DisplayName("should return empty Mono when refund not found by ID")
    void shouldReturnEmptyMonoWhenRefundNotFound() {
        RefundRepository repository = new MockRefundRepository();
        RefundId nonExistentId = RefundId.generate();

        StepVerifier.create(repository.findById(nonExistentId))
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findByPaymentId method")
    void shouldDefineFindByPaymentIdMethod() {
        MockRefundRepository repository = new MockRefundRepository();
        PaymentId paymentId = PaymentId.generate();
        Refund refund1 = Refund.create(paymentId, Money.krw(3000), "First refund");
        Refund refund2 = Refund.create(paymentId, Money.krw(2000), "Second refund");
        repository.save(refund1).block();
        repository.save(refund2).block();

        StepVerifier.create(repository.findByPaymentId(paymentId).collectList())
            .assertNext(refunds -> assertThat(refunds).hasSize(2))
            .verifyComplete();
    }

    @Test
    @DisplayName("should define findByStatus method")
    void shouldDefineFindByStatusMethod() {
        MockRefundRepository repository = new MockRefundRepository();
        PaymentId paymentId = PaymentId.generate();
        Refund refund = Refund.create(paymentId, Money.krw(5000), "Test refund");
        repository.save(refund).block();

        StepVerifier.create(repository.findByStatus(RefundStatus.REQUESTED).collectList())
            .assertNext(refunds -> {
                assertThat(refunds).hasSize(1);
                assertThat(refunds.get(0).getStatus()).isEqualTo(RefundStatus.REQUESTED);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("should define getTotalRefundedAmount method")
    void shouldDefineGetTotalRefundedAmountMethod() {
        MockRefundRepository repository = new MockRefundRepository();
        PaymentId paymentId = PaymentId.generate();

        // Create two completed refunds
        Refund refund1 = Refund.create(paymentId, Money.krw(3000), "First refund");
        refund1.startProcessing();
        refund1.complete("pg-1");
        repository.save(refund1).block();

        Refund refund2 = Refund.create(paymentId, Money.krw(2000), "Second refund");
        refund2.startProcessing();
        refund2.complete("pg-2");
        repository.save(refund2).block();

        StepVerifier.create(repository.getTotalRefundedAmount(paymentId))
            .assertNext(total -> assertThat(total.amount().longValue()).isEqualTo(5000))
            .verifyComplete();
    }

    /**
     * Mock implementation for testing the interface contract.
     */
    private static class MockRefundRepository implements RefundRepository {
        private final Map<RefundId, Refund> storage = new HashMap<>();

        @Override
        public Mono<Refund> save(Refund refund) {
            storage.put(refund.getId(), refund);
            return Mono.just(refund);
        }

        @Override
        public Mono<Refund> findById(RefundId id) {
            Refund refund = storage.get(id);
            return refund != null ? Mono.just(refund) : Mono.empty();
        }

        @Override
        public Flux<Refund> findByPaymentId(PaymentId paymentId) {
            return Flux.fromIterable(storage.values())
                .filter(r -> r.getPaymentId().equals(paymentId));
        }

        @Override
        public Flux<Refund> findByStatus(RefundStatus status) {
            return Flux.fromIterable(storage.values())
                .filter(r -> r.getStatus() == status);
        }

        @Override
        public Mono<Money> getTotalRefundedAmount(PaymentId paymentId) {
            return findByPaymentId(paymentId)
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                .map(Refund::getAmount)
                .reduce(Money::add)
                .defaultIfEmpty(Money.krw(0));
        }
    }
}
