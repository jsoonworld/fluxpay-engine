# CLAUDE.md - FluxPay Engine

## Project Overview

FluxPay Engine is a **domain-independent payment and billing engine** designed to handle high-throughput financial transactions with reliability and scalability. Built on reactive principles, it provides a flexible foundation for various payment scenarios including one-time payments, subscriptions, credits, and complex billing workflows.

Key characteristics:
- **Reactive & Non-blocking**: Built entirely on Project Reactor for high concurrency
- **Domain-agnostic**: Decoupled from specific business domains, can be integrated into any system requiring payment capabilities
- **Distributed Transaction Support**: Implements Saga pattern for cross-service consistency
- **Event-driven**: Uses Kafka for reliable event streaming and eventual consistency

---

## Tech Stack & Architecture

### Core Technologies
| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3 + WebFlux |
| Language | Java 21 |
| Build Tool | Gradle (Groovy DSL) |
| Database | PostgreSQL (R2DBC - reactive) |
| Cache | Redis |
| Messaging | Apache Kafka |

### Architecture Style: Hexagonal / Clean Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│                 (Controllers, DTOs, APIs)                    │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│            (Entities, Use Cases, Domain Services)            │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                       │
│         (Repositories, External Services, Adapters)          │
└─────────────────────────────────────────────────────────────┘
```

The architecture enforces:
- **Dependency Rule**: Dependencies point inward (infrastructure → domain ← presentation)
- **Port & Adapter Pattern**: Domain defines ports (interfaces), infrastructure implements adapters
- **Domain Isolation**: Core business logic has no external dependencies

---

## Code Conventions

### Package Structure
```
src/main/java/com/fluxpay/engine/
├── domain/                    # Core business logic
│   ├── model/                 # Domain entities and value objects
│   ├── service/               # Domain services
│   ├── port/                  # Ports (interfaces for adapters)
│   └── event/                 # Domain events
├── infrastructure/            # External concerns
│   ├── persistence/           # R2DBC repositories, entities
│   ├── messaging/             # Kafka producers/consumers
│   ├── cache/                 # Redis operations
│   └── external/              # External service clients
└── presentation/              # API layer
    ├── api/                   # REST controllers
    └── dto/                   # Request/Response DTOs
```

### Naming Conventions
- **Classes**: PascalCase (e.g., `PaymentService`, `OrderRepository`)
- **Methods/Variables**: camelCase (e.g., `processPayment`, `orderId`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Packages**: lowercase (e.g., `com.fluxpay.engine.domain`)

### Reactive Programming Guidelines
- Always use `Mono<T>` for single-value async operations
- Always use `Flux<T>` for multi-value async streams
- Never block reactive streams (no `.block()` in production code)
- Use operators like `flatMap`, `map`, `switchIfEmpty` appropriately
- Handle errors with `onErrorResume`, `onErrorMap`, `doOnError`

```java
// Good example
public Mono<Payment> processPayment(PaymentRequest request) {
    return paymentRepository.findById(request.getPaymentId())
        .switchIfEmpty(Mono.error(new PaymentNotFoundException()))
        .flatMap(payment -> validatePayment(payment))
        .flatMap(payment -> executePayment(payment))
        .doOnSuccess(payment -> log.info("Payment processed: {}", payment.getId()))
        .doOnError(error -> log.error("Payment failed", error));
}
```

### DTO Conventions
- Request DTOs: `*Request.java` (e.g., `CreatePaymentRequest`)
- Response DTOs: `*Response.java` (e.g., `PaymentResponse`)
- Use records for immutable DTOs (Java 21 feature)
- Validate with Jakarta Bean Validation annotations

---

## TDD Requirements (CRITICAL)

### Test-Driven Development is MANDATORY

All new features MUST follow the **Red-Green-Refactor** cycle:

1. **RED**: Write a failing test first
2. **GREEN**: Write minimal code to make the test pass
3. **REFACTOR**: Clean up the code while keeping tests green

### Test File Naming
| Test Type | Naming Pattern | Location |
|-----------|----------------|----------|
| Unit Tests | `*Test.java` | `src/test/java/.../` |
| Integration Tests | `*IntegrationTest.java` | `src/test/java/.../` |

### Testing Tools & Patterns

#### WebTestClient for Controller Tests
```java
@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePayment() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest(...);
        when(paymentService.create(any())).thenReturn(Mono.just(payment));

        // When & Then
        webTestClient.post()
            .uri("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .value(response -> assertThat(response.getId()).isNotNull());
    }
}
```

#### StepVerifier for Reactive Tests
```java
@Test
void shouldProcessPaymentReactively() {
    // Given
    Payment payment = createTestPayment();

    // When
    Mono<Payment> result = paymentService.process(payment);

    // Then
    StepVerifier.create(result)
        .assertNext(processed -> {
            assertThat(processed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(processed.getProcessedAt()).isNotNull();
        })
        .verifyComplete();
}

@Test
void shouldHandlePaymentError() {
    // Given
    Payment invalidPayment = createInvalidPayment();

    // When
    Mono<Payment> result = paymentService.process(invalidPayment);

    // Then
    StepVerifier.create(result)
        .expectError(PaymentValidationException.class)
        .verify();
}
```

### Code Coverage
- **Target**: Minimum 80% code coverage
- **Domain layer**: Aim for 90%+ coverage
- **Integration tests**: Cover all critical paths

### Pre-Commit Checklist
```bash
# ALWAYS run before committing
./gradlew test

# Check coverage report
./gradlew jacocoTestReport
```

---

## Common Commands

### Build & Test
```bash
# Full build with tests
./gradlew build

# Run tests only
./gradlew test

# Run specific test class
./gradlew test --tests "PaymentServiceTest"

# Run with test coverage
./gradlew test jacocoTestReport

# Clean build
./gradlew clean build
```

### Run Application
```bash
# Start the application
./gradlew bootRun

# Start with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Infrastructure
```bash
# Start all dependencies (PostgreSQL, Redis, Kafka)
docker-compose up -d

# Stop all dependencies
docker-compose down

# View logs
docker-compose logs -f
```

### Code Quality
```bash
# Run checkstyle
./gradlew checkstyleMain

# Run all checks
./gradlew check
```

---

## Domain Knowledge

### Core Domains

#### 1. Order Domain
- Represents purchase intent and order lifecycle
- States: CREATED → PENDING → CONFIRMED → COMPLETED / CANCELLED
- Aggregates line items, pricing, and customer information

#### 2. Payment Domain
- Handles monetary transactions
- Supports multiple payment methods (card, bank transfer, wallet)
- Implements idempotency for safe retries
- States: INITIATED → PROCESSING → COMPLETED / FAILED / REFUNDED

#### 3. Credit Domain
- Manages virtual currency, points, or prepaid balances
- Supports credit allocation, consumption, and expiration
- Atomic operations for concurrent access safety

#### 4. Subscription Domain
- Handles recurring billing cycles
- Manages subscription lifecycle (trial, active, paused, cancelled)
- Integrates with payment domain for automatic renewals

### Distributed Transaction Patterns

#### Saga Pattern
Used for long-running transactions spanning multiple services:
```
Order Service → Payment Service → Inventory Service → Notification Service
     │               │                  │                    │
     └─── Compensating transactions on failure ────────────────┘
```

- **Choreography**: Services react to events (preferred for loose coupling)
- **Orchestration**: Central coordinator manages the flow

#### Transactional Outbox Pattern
Ensures reliable event publishing with database transactions:
```
1. Begin Transaction
2. Update domain state in database
3. Insert event into outbox table
4. Commit Transaction
5. (Async) Outbox relay publishes to Kafka
```

This guarantees at-least-once delivery without distributed transactions.

### Idempotency Implementation
Redis Lua scripts ensure atomic idempotency checks:
```lua
-- Check and set idempotency key
local key = KEYS[1]
local value = ARGV[1]
local ttl = ARGV[2]

if redis.call('EXISTS', key) == 1 then
    return redis.call('GET', key)
else
    redis.call('SET', key, value, 'EX', ttl)
    return nil
end
```

Every payment request must include an idempotency key in the header:
```
X-Idempotency-Key: <uuid>
```

---

## Git Workflow

### Branch Strategy
```
main (production)
  └── develop (integration)
        ├── feature/payment-refund
        ├── feature/subscription-pause
        └── bugfix/credit-calculation
```

### Branch Naming
- Features: `feature/<description>` (e.g., `feature/payment-webhook`)
- Bug fixes: `bugfix/<description>` (e.g., `bugfix/duplicate-charge`)
- Hotfixes: `hotfix/<description>` (e.g., `hotfix/security-patch`)

### Commit Message Format
```
<type>: <subject>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring (no functional change)
- `test`: Adding or updating tests
- `docs`: Documentation changes
- `chore`: Build, CI, or tooling changes

**Examples**:
```
feat: Add payment refund endpoint

- Implement partial and full refund support
- Add refund reason tracking
- Update payment status on refund

Closes #123
```

```
fix: Prevent duplicate payment processing

Add idempotency check before initiating payment transaction.
```

### Pull Request Guidelines
1. All code and comments MUST be in English
2. PRs require at least one approval
3. All tests must pass
4. Code coverage must not decrease
5. Link related issues in PR description

---

## Quick Reference

| Task | Command |
|------|---------|
| Build | `./gradlew build` |
| Test | `./gradlew test` |
| Run | `./gradlew bootRun` |
| Start infra | `docker-compose up -d` |
| Coverage report | `./gradlew jacocoTestReport` |

---

*Last updated: 2026-02-04*
