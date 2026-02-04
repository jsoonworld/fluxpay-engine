# FluxPay Engine - Claude Code System Prompt Additions

> Adapted from Kent Beck's BPlusTree3 development principles for Java/Spring Boot WebFlux environment.
> This document establishes quality gates and development standards that MUST be followed.

---

## Code Quality Standards

### NEVER write production code that contains:

1. **Blocking calls in reactive streams** - NEVER use `.block()` in production code. This defeats the entire purpose of reactive programming and can cause thread starvation.

2. **Raw exceptions without proper handling** - Always return `Mono.error()` or handle exceptions properly. Never let exceptions propagate unhandled.

3. **Memory leaks** - Properly dispose subscriptions, use proper scopes with `Disposable`, and be careful with infinite streams.

4. **Data corruption potential** - All state transitions must be atomic and preserve data integrity. Use proper transaction boundaries with R2DBC `@Transactional`.

5. **Inconsistent error handling patterns** - Establish and follow a single pattern across the entire codebase. Use `GlobalExceptionHandler` with `@ControllerAdvice`.

6. **Subscription leaks** - Never subscribe without proper cleanup. Use `Disposable` tracking or bounded operators.

7. **Thread pool exhaustion** - Never perform blocking I/O on reactive schedulers. Use `Schedulers.boundedElastic()` for unavoidable blocking operations.

---

### ALWAYS:

1. **Write comprehensive tests BEFORE implementing features** (TDD is mandatory)
   - Red -> Green -> Refactor cycle
   - No production code without a failing test first

2. **Include invariant validation in domain objects**
   - Validate in constructors
   - Use Value Objects for identities (PaymentId, TransactionId, etc.)
   - Fail fast on invalid state

3. **Use proper reactive error handling**
   - `onErrorResume()` for recovery
   - `onErrorMap()` for exception translation
   - `doOnError()` for logging only

4. **Document known bugs immediately and fix them before continuing**
   - Create GitHub issues for discovered bugs
   - Never proceed with new features while known bugs exist
   - Technical debt must be tracked and addressed

5. **Implement proper separation of concerns (Hexagonal Architecture)**
   ```
   presentation/  <- Controllers, DTOs, API contracts
   application/   <- Use cases, orchestration, application services
   domain/        <- Entities, Value Objects, Domain Services, Ports
   infrastructure/ <- Adapters, Repository implementations, External services
   ```

6. **Use static analysis tools before considering code complete**
   - SonarQube for code quality
   - SpotBugs for bug detection
   - Checkstyle for code style consistency
   - Run `./gradlew check` and ensure zero warnings

---

## Development Process Guards

### TESTING REQUIREMENTS

#### Unit Testing with StepVerifier
```java
@Test
void shouldProcessPaymentSuccessfully() {
    // Given
    PaymentCommand command = new PaymentCommand(amount, merchantId);
    when(paymentRepository.save(any())).thenReturn(Mono.just(savedPayment));

    // When
    Mono<Payment> result = paymentService.process(command);

    // Then
    StepVerifier.create(result)
        .assertNext(payment -> {
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getAmount()).isEqualTo(amount);
        })
        .verifyComplete();
}

@Test
void shouldHandlePaymentFailure() {
    // Given
    when(externalGateway.charge(any()))
        .thenReturn(Mono.error(new GatewayException("Connection refused")));

    // When & Then
    StepVerifier.create(paymentService.process(command))
        .expectErrorMatches(ex ->
            ex instanceof PaymentProcessingException &&
            ex.getMessage().contains("Gateway communication failed"))
        .verify();
}
```

#### Controller Testing with WebTestClient
```java
@WebFluxTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePayment() {
        // Given
        PaymentRequest request = new PaymentRequest(10000L, "merchant-123");
        PaymentResponse response = new PaymentResponse("pay-001", PaymentStatus.COMPLETED);

        when(paymentService.process(any()))
            .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.post()
            .uri("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.isSuccess").isEqualTo(true)
            .jsonPath("$.result.paymentId").isEqualTo("pay-001")
            .jsonPath("$.result.status").isEqualTo("COMPLETED");
    }

    @Test
    void shouldReturn400ForInvalidRequest() {
        // Given
        PaymentRequest invalidRequest = new PaymentRequest(-100L, null);

        // When & Then
        webTestClient.post()
            .uri("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.isSuccess").isEqualTo(false)
            .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }
}
```

#### Repository Testing with @DataR2dbcTest
```java
@DataR2dbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PaymentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("fluxpay_test");

    @Autowired
    private PaymentRepository paymentRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" +
            postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Test
    void shouldSaveAndFindPayment() {
        // Given
        Payment payment = Payment.create(10000L, "merchant-123");

        // When & Then
        StepVerifier.create(
            paymentRepository.save(payment)
                .flatMap(saved -> paymentRepository.findById(saved.getId()))
        )
        .assertNext(found -> {
            assertThat(found.getAmount()).isEqualTo(10000L);
            assertThat(found.getMerchantId()).isEqualTo("merchant-123");
        })
        .verifyComplete();
    }
}
```

#### Test Coverage Requirements
- **Minimum 80% code coverage** (enforced by JaCoCo)
- **100% coverage for domain logic**
- **All error paths must be tested**
- **Edge cases and boundary conditions must be covered**

---

### ARCHITECTURE REQUIREMENTS

#### Hexagonal Architecture Enforcement

```
src/main/java/com/fluxpay/engine/
├── domain/                          # Pure business logic - NO external dependencies
│   ├── model/                       # Entities and Value Objects
│   │   ├── Payment.java
│   │   ├── PaymentId.java          # Value Object
│   │   └── Money.java              # Value Object
│   ├── service/                     # Domain Services
│   │   └── PaymentDomainService.java
│   ├── event/                       # Domain Events
│   │   └── PaymentCompletedEvent.java
│   └── port/                        # Ports (interfaces)
│       ├── inbound/
│       │   └── PaymentUseCase.java
│       └── outbound/
│           ├── PaymentRepository.java
│           └── PaymentGateway.java
│
├── application/                     # Application layer - orchestration
│   ├── service/                     # Application Services (Use Case implementations)
│   │   └── PaymentApplicationService.java
│   └── dto/                         # Application DTOs
│       └── PaymentCommand.java
│
├── infrastructure/                  # External adapters
│   ├── persistence/                 # Database adapters
│   │   ├── entity/
│   │   │   └── PaymentEntity.java
│   │   ├── repository/
│   │   │   └── PaymentR2dbcRepository.java
│   │   └── adapter/
│   │       └── PaymentRepositoryAdapter.java
│   ├── external/                    # External service adapters
│   │   └── PaymentGatewayAdapter.java
│   └── config/                      # Infrastructure configuration
│       └── R2dbcConfig.java
│
└── presentation/                    # API layer
    ├── api/                         # REST Controllers
    │   └── PaymentController.java
    ├── dto/                         # Request/Response DTOs
    │   ├── PaymentRequest.java
    │   └── PaymentResponse.java
    └── handler/                     # Exception handlers
        └── GlobalExceptionHandler.java
```

#### Domain Layer Rules
- **NO Spring annotations** in domain layer (except validation annotations)
- **NO infrastructure imports** (no R2DBC, no WebFlux)
- Domain objects must be **immutable** where possible
- All validation in constructors - **fail fast**

---

### REVIEW CHECKPOINTS

Before marking ANY code complete, verify ALL of the following:

- [ ] **No compilation warnings** - `./gradlew compileJava` produces zero warnings
- [ ] **All tests pass** - `./gradlew test` passes 100%
- [ ] **Code coverage >= 80%** - `./gradlew jacocoTestCoverageVerification` passes
- [ ] **No reactive anti-patterns** - No `.block()`, no subscription leaks
- [ ] **Error handling is comprehensive** - All error paths handled and tested
- [ ] **Code follows package structure** - Hexagonal architecture maintained
- [ ] **Documentation matches implementation** - Javadoc is accurate
- [ ] **Static analysis passes** - `./gradlew check` with zero issues

---

## Java/Spring WebFlux-Specific Quality Standards

### ERROR HANDLING

#### Custom Domain Exceptions
```java
// Base domain exception
public abstract class DomainException extends RuntimeException {
    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

// Specific domain exceptions
public class PaymentNotFoundException extends DomainException {
    public PaymentNotFoundException(String paymentId) {
        super("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId);
    }
}

public class InsufficientBalanceException extends DomainException {
    public InsufficientBalanceException(Money requested, Money available) {
        super("INSUFFICIENT_BALANCE",
            String.format("Insufficient balance. Requested: %s, Available: %s",
                requested, available));
    }
}

public class PaymentProcessingException extends DomainException {
    public PaymentProcessingException(String message, Throwable cause) {
        super("PAYMENT_PROCESSING_ERROR", message, cause);
    }
}
```

#### Global Exception Handler
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleDomainException(
            DomainException ex, ServerWebExchange exchange) {

        log.warn("Domain exception occurred: code={}, message={}, path={}",
            ex.getErrorCode(), ex.getMessage(), exchange.getRequest().getPath());

        HttpStatus status = resolveStatus(ex);
        return Mono.just(ResponseEntity
            .status(status)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage())));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidationException(
            WebExchangeBindException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);

        return Mono.just(ResponseEntity
            .badRequest()
            .body(ApiResponse.error("VALIDATION_ERROR", message)));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleUnexpectedException(
            Exception ex, ServerWebExchange exchange) {

        log.error("Unexpected error occurred: path={}",
            exchange.getRequest().getPath(), ex);

        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.")));
    }

    private HttpStatus resolveStatus(DomainException ex) {
        return switch (ex) {
            case PaymentNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case InsufficientBalanceException ignored -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PaymentProcessingException ignored -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

#### MDC Context Propagation for Logging
```java
@Component
public class MdcContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders()
            .getFirst("X-Request-ID");

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        final String finalRequestId = requestId;
        exchange.getResponse().getHeaders().add("X-Request-ID", finalRequestId);

        return chain.filter(exchange)
            .contextWrite(Context.of("requestId", finalRequestId))
            .doOnEach(signal -> {
                if (!signal.isOnComplete()) {
                    signal.getContextView().getOrEmpty("requestId")
                        .ifPresent(id -> MDC.put("requestId", id.toString()));
                }
            })
            .doFinally(signalType -> MDC.clear());
    }
}
```

---

### REACTIVE PROGRAMMING STANDARDS

#### Operator Selection Guide

| Operation | Use | NOT |
|-----------|-----|-----|
| Async transformation | `flatMap()` | `map()` with nested subscribe |
| Sync transformation | `map()` | `flatMap()` for sync operations |
| Error recovery | `onErrorResume()` | try-catch |
| Error translation | `onErrorMap()` | catch and rethrow |
| Side effects | `doOnNext()`, `doOnError()` | Subscribe inside chain |
| Conditional | `filter()`, `switchIfEmpty()` | if-else with block |
| Combine results | `zip()`, `zipWith()` | Multiple subscribes |
| First result | `firstWithValue()` | Manual tracking |

#### Scheduler Usage
```java
// DO: Use appropriate scheduler for blocking operations
return Mono.fromCallable(() -> blockingLegacyService.call())
    .subscribeOn(Schedulers.boundedElastic())
    .flatMap(result -> processResult(result));

// DO: Keep reactive operations on default scheduler
return paymentRepository.findById(id)
    .flatMap(payment -> gateway.charge(payment))
    .map(PaymentResponse::from);

// NEVER: Block on reactive scheduler
return paymentRepository.findById(id)
    .map(payment -> blockingService.process(payment).block()); // FORBIDDEN
```

#### Backpressure Handling
```java
// For high-volume streams, always consider backpressure
return eventSource.stream()
    .onBackpressureBuffer(1000,
        dropped -> log.warn("Event dropped due to backpressure: {}", dropped),
        BufferOverflowStrategy.DROP_OLDEST)
    .flatMap(this::processEvent, 10) // Limit concurrency
    .doOnNext(result -> metrics.recordProcessed());
```

---

### DATA INTEGRITY

#### Transaction Management
```java
@Service
@RequiredArgsConstructor
public class PaymentApplicationService implements PaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Payment> processPayment(PaymentCommand command) {
        return Mono.defer(() -> {
            Payment payment = Payment.create(command.amount(), command.merchantId());
            Transaction transaction = Transaction.forPayment(payment);

            return paymentRepository.save(payment)
                .flatMap(saved -> transactionRepository.save(transaction)
                    .thenReturn(saved));
        })
        .as(transactionalOperator::transactional)
        .doOnError(ex -> log.error("Payment processing failed", ex));
    }
}
```

#### Optimistic Locking
```java
@Table("payments")
public class PaymentEntity {

    @Id
    private Long id;

    @Version
    private Long version;

    private String paymentId;
    private Long amount;
    private String status;

    // Update operations check version automatically
}
```

#### Value Objects for Identity
```java
public record PaymentId(String value) {

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PaymentId cannot be blank");
        }
        if (!value.matches("^pay_[a-zA-Z0-9]{16}$")) {
            throw new IllegalArgumentException("Invalid PaymentId format: " + value);
        }
    }

    public static PaymentId generate() {
        return new PaymentId("pay_" + generateRandomString(16));
    }

    private static String generateRandomString(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

---

## Critical Patterns to Avoid

### DANGEROUS PATTERNS - NEVER DO THESE

```java
// 1. NEVER: Blocking in reactive pipeline
return repository.findById(id)
    .map(entity -> blockingService.process(entity).block()); // Thread starvation!

// 2. NEVER: Swallowing errors silently
return service.process()
    .onErrorResume(e -> Mono.empty()); // Lost error information!

// 3. NEVER: No error handling on external calls
return webClient.get()
    .retrieve()
    .bodyToMono(Response.class); // What happens on 4xx/5xx?

// 4. NEVER: Subscribe inside reactive chain
return repository.findById(id)
    .doOnNext(entity -> {
        auditService.log(entity).subscribe(); // Subscription leak!
    });

// 5. NEVER: Mixing blocking and reactive
public Mono<Payment> getPayment(String id) {
    Payment payment = repository.findById(id).block(); // Defeats reactive!
    return Mono.just(payment);
}

// 6. NEVER: Ignoring subscription result
service.process().subscribe(); // Fire and forget = lost errors!

// 7. NEVER: Mutable state in reactive chains
AtomicReference<Payment> payment = new AtomicReference<>();
return repository.findById(id)
    .doOnNext(payment::set) // Race condition risk!
    .flatMap(p -> externalService.call());
```

### PREFERRED PATTERNS - ALWAYS DO THESE

```java
// 1. DO: Proper reactive chain
return repository.findById(id)
    .flatMap(entity -> asyncService.process(entity));

// 2. DO: Proper error handling with context
return service.process()
    .onErrorResume(PaymentException.class, e -> {
        log.error("Payment processing failed: {}", e.getMessage());
        return Mono.error(new BusinessException("Payment failed", e));
    });

// 3. DO: Comprehensive HTTP error handling
return webClient.get()
    .uri("/payments/{id}", paymentId)
    .retrieve()
    .onStatus(HttpStatusCode::is4xxClientError, response ->
        response.bodyToMono(ErrorResponse.class)
            .flatMap(err -> Mono.error(new ClientException(err.message()))))
    .onStatus(HttpStatusCode::is5xxServerError, response ->
        Mono.error(new ExternalServiceException("Gateway unavailable")))
    .bodyToMono(PaymentResponse.class)
    .timeout(Duration.ofSeconds(5))
    .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
        .filter(ex -> ex instanceof ExternalServiceException));

// 4. DO: Chain side effects properly
return repository.findById(id)
    .flatMap(entity ->
        auditService.log(entity)
            .thenReturn(entity));

// 5. DO: Full reactive flow
public Mono<Payment> getPayment(String id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new PaymentNotFoundException(id)));
}

// 6. DO: Handle subscription properly
return service.process()
    .doOnSuccess(result -> log.info("Processed: {}", result))
    .doOnError(ex -> log.error("Processing failed", ex));

// 7. DO: Immutable transformations
return repository.findById(id)
    .map(payment -> payment.withStatus(PaymentStatus.COMPLETED))
    .flatMap(repository::save);
```

---

## Testing Standards

### StepVerifier Patterns

```java
// Testing successful completion
@Test
void shouldEmitSingleValue() {
    Mono<String> result = service.getValue();

    StepVerifier.create(result)
        .expectNext("expected-value")
        .verifyComplete();
}

// Testing error scenarios
@Test
void shouldEmitError() {
    Mono<String> result = service.getValueThatFails();

    StepVerifier.create(result)
        .expectErrorSatisfies(ex -> {
            assertThat(ex).isInstanceOf(DomainException.class);
            assertThat(ex.getMessage()).contains("expected message");
        })
        .verify();
}

// Testing Flux with multiple values
@Test
void shouldEmitMultipleValues() {
    Flux<Integer> result = service.getValues();

    StepVerifier.create(result)
        .expectNext(1, 2, 3)
        .expectNextCount(7)
        .verifyComplete();
}

// Testing with virtual time for delays
@Test
void shouldHandleTimeout() {
    StepVerifier.withVirtualTime(() ->
            service.callWithTimeout()
                .timeout(Duration.ofSeconds(5)))
        .expectSubscription()
        .thenAwait(Duration.ofSeconds(6))
        .expectError(TimeoutException.class)
        .verify();
}

// Testing context propagation
@Test
void shouldPropagateContext() {
    Mono<String> result = service.getValueWithContext()
        .contextWrite(Context.of("userId", "user-123"));

    StepVerifier.create(result)
        .expectNext("user-123")
        .verifyComplete();
}
```

### WebTestClient Patterns

```java
@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentUseCase paymentUseCase;

    // Testing successful POST
    @Test
    void shouldCreatePaymentSuccessfully() {
        PaymentRequest request = new PaymentRequest(10000L, "merchant-001");
        Payment payment = Payment.create(request.amount(), request.merchantId());

        when(paymentUseCase.process(any())).thenReturn(Mono.just(payment));

        webTestClient.post()
            .uri("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().exists("Location")
            .expectBody()
            .jsonPath("$.isSuccess").isEqualTo(true)
            .jsonPath("$.result.id").isNotEmpty();
    }

    // Testing error response
    @Test
    void shouldReturnNotFoundForMissingPayment() {
        when(paymentUseCase.findById(any()))
            .thenReturn(Mono.error(new PaymentNotFoundException("pay-123")));

        webTestClient.get()
            .uri("/api/v1/payments/pay-123")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.isSuccess").isEqualTo(false)
            .jsonPath("$.code").isEqualTo("PAYMENT_NOT_FOUND");
    }

    // Testing with authentication
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminAccess() {
        webTestClient.get()
            .uri("/api/v1/admin/payments")
            .exchange()
            .expectStatus().isOk();
    }
}
```

### Integration Test Patterns

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void shouldProcessPaymentEndToEnd() {
        // Given
        PaymentRequest request = new PaymentRequest(50000L, "merchant-001");

        // When & Then
        String paymentId = webTestClient.post()
            .uri("/api/v1/payments")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.result.id").value(id -> assertThat(id).isNotNull())
            .returnResult()
            .getResponseBody();

        // Verify payment was persisted
        webTestClient.get()
            .uri("/api/v1/payments/{id}", extractPaymentId(paymentId))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.result.amount").isEqualTo(50000);
    }
}
```

---

## Pre-Commit Checklist

Before every commit, run these commands and ensure all pass:

```bash
# 1. Compile without warnings
./gradlew compileJava compileTestJava

# 2. Run all tests
./gradlew test

# 3. Check code coverage
./gradlew jacocoTestCoverageVerification

# 4. Full check (includes all above + style checks)
./gradlew check

# 5. Verify no .block() in production code
grep -r "\.block()" --include="*.java" src/main/java && echo "FAIL: Found .block() in production code" || echo "PASS: No .block() found"
```

---

## Remember

> "First make it work, then make it right, then make it fast."
> - Kent Beck

> "Any fool can write code that a computer can understand. Good programmers write code that humans can understand."
> - Martin Fowler

> "The best code is no code at all."
> - Jeff Atwood

**In reactive programming:**
- Every `.block()` is a code smell
- Every swallowed error is a debugging nightmare waiting to happen
- Every untested path is a production incident in disguise

**Follow TDD religiously. Write the test first. Always.**
