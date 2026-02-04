# FluxPay Engine

> A domain-agnostic payment and billing engine ensuring data integrity in distributed environments

## Overview

FluxPay Engine is a **pluggable payment/billing system** that can be integrated with any service to handle payments, billing, and traffic control. It supports various business models (credits, subscriptions, pay-per-use) through configuration without domain-specific dependencies.

### Key Characteristics

| Characteristic | Description |
|----------------|-------------|
| **Reactive & Non-blocking** | Built on Project Reactor for high concurrency |
| **Domain-agnostic** | Not tied to any specific business domain |
| **Distributed Transaction** | Saga pattern for cross-service consistency |
| **Event-driven** | Kafka-based event streaming |

### Problems We Solve

1. **Repetitive payment system development** - PG integration, idempotency, refund logic from scratch every time
2. **Data consistency in distributed environments** - Manual reconciliation when payment succeeds but service fails
3. **Traffic surge protection** - System crashes during flash sales or large promotions

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3 + WebFlux |
| Build | Gradle (Groovy DSL) |
| Database | PostgreSQL 16+ (R2DBC) |
| Cache | Redis 7.x |
| Messaging | Apache Kafka 3.x |
| PG | TossPayments |

## Quick Start

### Prerequisites

- JDK 21
- Docker & Docker Compose
- Gradle 8.x

### Setup

```bash
# Start infrastructure (PostgreSQL, Redis, Kafka)
docker-compose up -d

# Setup Git hooks
./.githooks/setup.sh

# Run application
./gradlew bootRun
```

### API Documentation

After starting the server: http://localhost:8080/swagger-ui.html

## Core Domains

| Domain | Description | Key Capabilities |
|--------|-------------|------------------|
| **Order** | Purchase intent and lifecycle | Create, confirm, cancel |
| **Payment** | PG-integrated payment processing | Approve, confirm, refund |
| **Credit** | Prepaid credit system | Charge, consume, refund |
| **Subscription** | Recurring billing management | Subscribe, renew, pause, cancel |

## Architecture

FluxPay Engine follows **Hexagonal Architecture** (Ports & Adapters):

```text
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

### Project Structure

```text
src/main/java/com/fluxpay/engine/
├── domain/                    # Core business logic
│   ├── model/                 # Entities and value objects
│   ├── service/               # Domain services
│   ├── port/                  # Ports (interfaces)
│   └── event/                 # Domain events
├── infrastructure/            # External concerns
│   ├── persistence/           # R2DBC repositories
│   ├── messaging/             # Kafka producers/consumers
│   └── external/              # PG clients
└── presentation/              # API layer
    ├── api/                   # REST controllers
    └── dto/                   # Request/Response DTOs
```

## Technical Patterns

| Pattern | Purpose |
|---------|---------|
| **Saga Pattern** | Distributed transaction compensation |
| **Transactional Outbox** | Reliable event publishing |
| **Idempotency (2-Layer)** | Duplicate request prevention (Redis + PostgreSQL) |
| **Virtual Waiting Room** | Traffic control during peak loads |

## Success Criteria

| Metric | Target |
|--------|--------|
| Payment Success Rate | 99.9%+ |
| Duplicate Payments | 0/month |
| Consistency Recovery | < 30 seconds |
| API Response (p95) | < 200ms |
| Throughput | 1,000+ TPS |

## Development

### TDD is Mandatory

All development follows **Red-Green-Refactor** cycle. See [CLAUDE.md](CLAUDE.md) for detailed guidelines.

```bash
# Run tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Full verification
./gradlew check
```

### Code Coverage

- Minimum: 80%
- Domain layer: 90%+

## Documentation

| Document | Description |
|----------|-------------|
| [1-Pager](docs/1-pager.md) | Executive summary |
| [Product Brief](docs/product-brief.md) | Business goals, use cases, roadmap |
| [Tech Spec](docs/tech-spec.md) | Technical architecture and specifications |
| [CLAUDE.md](CLAUDE.md) | Development conventions and guidelines |

## Roadmap

| Phase | Goal | Key Deliverables |
|-------|------|------------------|
| **1** | Core Payment | Order/Payment, PG integration, Basic API |
| **2** | Reliability | Event architecture, Saga, Multi-tenancy |
| **3** | Scalability | Traffic control, Subscription, Load testing |
| **4** | Operations | Dashboards, Alerting, Distributed tracing |

## License

MIT License
