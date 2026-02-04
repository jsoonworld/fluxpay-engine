# CLAUDE.md - FluxPay Engine

## Critical Development Rules

### MANDATORY: Test-Driven Development
1. **NEVER** write production code without a failing test first
2. **Red → Green → Refactor** cycle is **NON-NEGOTIABLE**
3. If you find yourself writing code without a test, **STOP** and write the test first
4. Tests are not an afterthought—they are the foundation of our development process
5. "Make it work, make it right, make it fast" — but always start with a test

### FORBIDDEN Patterns
These patterns are strictly prohibited in this codebase:

| Pattern | Why It's Forbidden | What To Do Instead |
|---------|-------------------|-------------------|
| `.block()` in production reactive code | Defeats the purpose of reactive programming, causes thread starvation | Use reactive operators, return `Mono`/`Flux` |
| Empty catch blocks | Swallows exceptions silently, hides bugs | Log the error, rethrow, or handle explicitly |
| Swallowing exceptions | Masks root causes, makes debugging impossible | Use `onErrorResume`, `onErrorMap` with proper handling |
| Committing code with failing tests | Breaks CI/CD, affects team productivity | Fix tests before committing, no exceptions |
| Skipping code review | Introduces bugs, reduces code quality | All code must be reviewed before merge |
| Skipping coverage checks | Allows untested code to ship | Maintain minimum 80% coverage |
| `@SuppressWarnings` without justification | Hides potential issues | Document why suppression is necessary |
| Hardcoded secrets/credentials | Security vulnerability | Use environment variables or secret management |

---

## Code Review Checklist

Before approving any PR, verify ALL of the following:

### TDD Compliance
- [ ] Tests written BEFORE implementation? (Check commit history)
- [ ] All tests pass locally and in CI?
- [ ] Test names clearly describe the behavior being tested?
- [ ] Both happy path and error cases covered?

### Quality Gates
- [ ] Code coverage >= 80%? (Domain layer should be 90%+)
- [ ] No decrease in overall coverage?
- [ ] No blocking calls (`.block()`) in reactive code?
- [ ] Proper error handling with meaningful messages?

### Architecture Compliance
- [ ] Follows hexagonal architecture boundaries?
- [ ] Domain layer has no external dependencies?
- [ ] Ports and adapters properly separated?
- [ ] DTOs don't leak into domain layer?

### Code Quality
- [ ] No code smells (long methods, deep nesting, etc.)?
- [ ] Single Responsibility Principle followed?
- [ ] No magic numbers or strings?
- [ ] Proper logging at appropriate levels?

### Documentation
- [ ] Public APIs documented?
- [ ] Complex logic has explanatory comments?
- [ ] CHANGELOG updated if needed?

---

## Reactive Programming Rules

### Operator Selection Guide

#### When to Use `map`
Use `map` for **synchronous transformations** that don't involve I/O:
```java
// CORRECT: Simple transformation, no I/O
Mono<PaymentDto> dto = paymentMono.map(payment -> toDto(payment));

// CORRECT: Extracting a value
Mono<String> paymentId = paymentMono.map(Payment::getId);
```

#### When to Use `flatMap`
Use `flatMap` for **asynchronous operations** that return `Mono`/`Flux`:
```java
// CORRECT: Database call returns Mono
Mono<Payment> payment = paymentIdMono
    .flatMap(id -> paymentRepository.findById(id));

// CORRECT: Chaining multiple async operations
Mono<Receipt> receipt = paymentMono
    .flatMap(payment -> processPayment(payment))
    .flatMap(processed -> generateReceipt(processed));
```

#### Common Mistakes
```java
// WRONG: Using map with Mono-returning method creates Mono<Mono<T>>
Mono<Mono<Payment>> wrong = paymentIdMono.map(id -> paymentRepository.findById(id));

// CORRECT: Use flatMap
Mono<Payment> correct = paymentIdMono.flatMap(id -> paymentRepository.findById(id));
```

### Error Handling Patterns

#### Pattern 1: Transform to Domain Exception
```java
public Mono<Payment> findPayment(String id) {
    return paymentRepository.findById(id)
        .switchIfEmpty(Mono.error(new PaymentNotFoundException(id)))
        .onErrorMap(DataAccessException.class,
            ex -> new PaymentRepositoryException("Failed to fetch payment", ex));
}
```

#### Pattern 2: Fallback with Recovery
```java
public Mono<PaymentStatus> getStatus(String id) {
    return primaryService.getStatus(id)
        .onErrorResume(ServiceUnavailableException.class,
            ex -> {
                log.warn("Primary service unavailable, falling back", ex);
                return fallbackService.getStatus(id);
            });
}
```

#### Pattern 3: Side Effects for Logging
```java
public Mono<Payment> processPayment(Payment payment) {
    return executePayment(payment)
        .doOnSuccess(p -> log.info("Payment {} completed successfully", p.getId()))
        .doOnError(ex -> log.error("Payment {} failed: {}", payment.getId(), ex.getMessage()));
}
```

### Backpressure Considerations

#### Use Appropriate Operators
```java
// For bounded streams with backpressure
Flux<Payment> payments = paymentRepository.findAll()
    .limitRate(100)  // Request 100 items at a time
    .buffer(50);     // Process in batches of 50

// For unbounded streams, use onBackpressure* operators
Flux<Event> events = eventStream
    .onBackpressureBuffer(1000)  // Buffer up to 1000 events
    .onBackpressureDrop(dropped -> log.warn("Dropped event: {}", dropped));
```

### Never Block Rules

#### Absolutely NEVER do this:
```java
// FORBIDDEN: Blocks the reactive thread pool
public Payment getPaymentBlocking(String id) {
    return paymentRepository.findById(id).block();  // NEVER!
}

// FORBIDDEN: Blocking in a reactive chain
public Mono<Payment> process(String id) {
    Payment payment = paymentRepository.findById(id).block();  // NEVER!
    return Mono.just(processSync(payment));
}
```

#### Instead, do this:
```java
// CORRECT: Stay reactive end-to-end
public Mono<Payment> process(String id) {
    return paymentRepository.findById(id)
        .flatMap(payment -> processAsync(payment));
}
```

#### Testing Exception
`.block()` is ONLY acceptable in tests:
```java
@Test
void shouldProcessPayment() {
    Payment result = paymentService.process(testPayment).block();
    assertThat(result.getStatus()).isEqualTo(COMPLETED);
}
```

---

## Domain Modeling Guidelines

### Value Objects vs Entities

#### Value Objects
- Immutable
- No identity (compared by attributes)
- Represent descriptive aspects of the domain

```java
// Value Object: Defined by its attributes, immutable
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        Objects.requireNonNull(currency, "Currency is required");
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException();
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

// Value Object: Payment Method
public record PaymentMethod(PaymentMethodType type, String token) {
    public PaymentMethod {
        Objects.requireNonNull(type);
        Objects.requireNonNull(token);
    }
}
```

#### Entities
- Have identity (compared by ID)
- Mutable state with controlled transitions
- Lifecycle tracked over time

```java
// Entity: Has unique identity, state changes over time
public class Payment {
    private final PaymentId id;           // Identity
    private PaymentStatus status;          // Mutable state
    private final Money amount;            // Value Object
    private final PaymentMethod method;    // Value Object
    private Instant processedAt;

    // State transitions through explicit methods
    public void markAsProcessing() {
        if (this.status != PaymentStatus.INITIATED) {
            throw new InvalidPaymentStateException(this.status, PaymentStatus.PROCESSING);
        }
        this.status = PaymentStatus.PROCESSING;
    }

    public void complete(Instant completedAt) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStateException(this.status, PaymentStatus.COMPLETED);
        }
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = completedAt;
    }
}
```

### Aggregate Root Pattern

An Aggregate is a cluster of domain objects that can be treated as a single unit:

```java
// Order is the Aggregate Root
public class Order {
    private final OrderId id;
    private final List<OrderLineItem> lineItems;  // Only accessible through Order
    private OrderStatus status;
    private Money totalAmount;

    // All modifications go through the Aggregate Root
    public void addLineItem(Product product, int quantity) {
        validateCanModify();
        OrderLineItem item = new OrderLineItem(product, quantity);
        this.lineItems.add(item);
        recalculateTotal();
    }

    public void removeLineItem(OrderLineItemId itemId) {
        validateCanModify();
        this.lineItems.removeIf(item -> item.getId().equals(itemId));
        recalculateTotal();
    }

    // Invariants are enforced by the Aggregate Root
    private void validateCanModify() {
        if (status != OrderStatus.DRAFT) {
            throw new OrderNotModifiableException(id, status);
        }
    }
}
```

#### Aggregate Rules
1. Reference other Aggregates by ID only, not by object reference
2. One transaction = one Aggregate modification
3. Aggregate Root is the only entry point for modifications
4. Keep Aggregates small for better concurrency

### Domain Events

Domain Events capture something important that happened in the domain:

```java
// Domain Event: Immutable record of what happened
public record PaymentCompletedEvent(
    String eventId,
    PaymentId paymentId,
    Money amount,
    Instant occurredAt
) implements DomainEvent {
    public PaymentCompletedEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(occurredAt);
    }

    public static PaymentCompletedEvent from(Payment payment) {
        return new PaymentCompletedEvent(
            UUID.randomUUID().toString(),
            payment.getId(),
            payment.getAmount(),
            Instant.now()
        );
    }
}

// Publishing events from domain service
public class PaymentDomainService {
    private final DomainEventPublisher eventPublisher;

    public Mono<Payment> completePayment(Payment payment) {
        payment.complete(Instant.now());
        return paymentRepository.save(payment)
            .doOnSuccess(saved ->
                eventPublisher.publish(PaymentCompletedEvent.from(saved)));
    }
}
```

### Repository Pattern

Repositories provide collection-like interface for Aggregates:

```java
// Port (Domain layer) - Defines the contract
public interface PaymentRepository {
    Mono<Payment> findById(PaymentId id);
    Mono<Payment> save(Payment payment);
    Flux<Payment> findByStatus(PaymentStatus status);
    Mono<Void> delete(PaymentId id);
}

// Adapter (Infrastructure layer) - Implements the contract
@Repository
public class R2dbcPaymentRepository implements PaymentRepository {
    private final PaymentR2dbcRepository r2dbcRepository;
    private final PaymentMapper mapper;

    @Override
    public Mono<Payment> findById(PaymentId id) {
        return r2dbcRepository.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Payment> save(Payment payment) {
        return r2dbcRepository.save(mapper.toEntity(payment))
            .map(mapper::toDomain);
    }
}
```

---

## API Design Standards

### Response Wrapper Format (ApiResponse)

All API responses must be wrapped in a consistent format:

```java
// Standard Response Wrapper
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorInfo error,
    ResponseMetadata metadata
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, ResponseMetadata.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message), ResponseMetadata.now());
    }
}

public record ErrorInfo(String code, String message, List<FieldError> fieldErrors) {
    public ErrorInfo(String code, String message) {
        this(code, message, List.of());
    }
}

public record ResponseMetadata(Instant timestamp, String traceId) {
    public static ResponseMetadata now() {
        return new ResponseMetadata(Instant.now(), MDC.get("traceId"));
    }
}
```

#### Success Response Example
```json
{
  "success": true,
  "data": {
    "paymentId": "pay_123456",
    "status": "COMPLETED",
    "amount": {
      "value": 10000,
      "currency": "KRW"
    }
  },
  "error": null,
  "metadata": {
    "timestamp": "2026-02-04T10:30:00Z",
    "traceId": "abc-123-def"
  }
}
```

#### Error Response Example
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PAY_001",
    "message": "Payment not found",
    "fieldErrors": []
  },
  "metadata": {
    "timestamp": "2026-02-04T10:30:00Z",
    "traceId": "abc-123-def"
  }
}
```

### Error Code Conventions

Error codes follow the pattern: `{DOMAIN}_{NUMBER}`

| Domain Prefix | Domain | Example Codes |
|--------------|--------|---------------|
| `PAY` | Payment | PAY_001 - PAY_099 |
| `ORD` | Order | ORD_001 - ORD_099 |
| `CRD` | Credit | CRD_001 - CRD_099 |
| `SUB` | Subscription | SUB_001 - SUB_099 |
| `SYS` | System | SYS_001 - SYS_099 |
| `VAL` | Validation | VAL_001 - VAL_099 |

#### Payment Error Codes
| Code | HTTP Status | Description |
|------|-------------|-------------|
| PAY_001 | 404 | Payment not found |
| PAY_002 | 400 | Invalid payment amount |
| PAY_003 | 409 | Payment already processed |
| PAY_004 | 422 | Payment method not supported |
| PAY_005 | 502 | Payment gateway error |
| PAY_006 | 400 | Invalid payment state transition |

#### System Error Codes
| Code | HTTP Status | Description |
|------|-------------|-------------|
| SYS_001 | 500 | Internal server error |
| SYS_002 | 503 | Service temporarily unavailable |
| SYS_003 | 504 | Gateway timeout |
| SYS_004 | 429 | Rate limit exceeded |

### Idempotency Key Handling

All mutating operations must support idempotency:

```java
// Controller: Extract idempotency key from header
@PostMapping("/payments")
public Mono<ApiResponse<PaymentResponse>> createPayment(
    @RequestHeader("X-Idempotency-Key") String idempotencyKey,
    @Valid @RequestBody CreatePaymentRequest request
) {
    return paymentService.createPayment(request, idempotencyKey)
        .map(payment -> ApiResponse.success(toResponse(payment)));
}

// Service: Check idempotency before processing
public Mono<Payment> createPayment(CreatePaymentRequest request, String idempotencyKey) {
    return idempotencyService.checkOrExecute(
        idempotencyKey,
        () -> doCreatePayment(request),
        Duration.ofHours(24)
    );
}

// Idempotency Service: Redis-backed implementation
public class RedisIdempotencyService {
    public <T> Mono<T> checkOrExecute(
        String key,
        Supplier<Mono<T>> operation,
        Duration ttl
    ) {
        return checkIdempotencyKey(key)
            .flatMap(existing -> {
                if (existing != null) {
                    return Mono.just(deserialize(existing));
                }
                return operation.get()
                    .flatMap(result -> storeResult(key, result, ttl));
            });
    }
}
```

#### Idempotency Key Requirements
- **Format**: UUID v4 recommended (e.g., `550e8400-e29b-41d4-a716-446655440000`)
- **TTL**: 24 hours default, configurable per endpoint
- **Scope**: Per-endpoint, not global

### Request Validation Patterns

Use Jakarta Bean Validation with custom validators:

```java
// Request DTO with validation
public record CreatePaymentRequest(
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Currency is required")
    @ValidCurrency  // Custom validator
    String currency,

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(CARD|BANK_TRANSFER|WALLET)$",
             message = "Invalid payment method")
    String paymentMethod,

    @NotBlank(message = "Order ID is required")
    @Size(max = 36, message = "Order ID too long")
    String orderId,

    @Valid  // Nested validation
    @NotNull(message = "Customer info is required")
    CustomerInfo customer
) {}

// Custom Validator
@Constraint(validatedBy = CurrencyValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {
    String message() default "Invalid currency code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {
    private static final Set<String> VALID_CURRENCIES = Set.of("KRW", "USD", "JPY", "EUR");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && VALID_CURRENCIES.contains(value.toUpperCase());
    }
}
```

#### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidationError(
        WebExchangeBindException ex
    ) {
        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
            .toList();

        return Mono.just(ResponseEntity
            .badRequest()
            .body(ApiResponse.validationError("VAL_001", "Validation failed", fieldErrors)));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handlePaymentNotFound(
        PaymentNotFoundException ex
    ) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("PAY_001", ex.getMessage())));
    }
}
```

---

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
