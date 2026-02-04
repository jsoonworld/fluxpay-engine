---
description: TDD Red Phase - Write a failing test first
---

# RED Phase - Write Failing Test

You are in the RED phase of TDD. Your task:

1. **Understand the requirement** - What behavior needs to be implemented?

2. **Write a test that fails** - Create a test that:
   - Describes the expected behavior clearly
   - Uses proper test naming: `should_[expectedBehavior]_when_[condition]`
   - Uses StepVerifier for reactive code
   - Uses WebTestClient for controller tests

3. **Run the test to confirm it fails**:
```bash
./gradlew test --tests "*[TestClassName]*"
```

4. **The test MUST fail** - If it passes, the test is wrong or the feature already exists

Example test structure:
```java
@Test
void should_return_payment_when_valid_id_provided() {
    // Given
    String paymentId = "pay_123";

    // When
    Mono<Payment> result = paymentService.findById(paymentId);

    // Then
    StepVerifier.create(result)
        .assertNext(payment -> {
            assertThat(payment.getId()).isEqualTo(paymentId);
        })
        .verifyComplete();
}
```

After confirming the test fails, proceed to GREEN phase with /green command.
