# FluxPay Engine Technical Specification

> **Version**: 1.6
> **Last Updated**: 2026-02-04
> **Status**: Draft

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Implementation Phases](#2-implementation-phases)
3. [System Architecture](#3-system-architecture)
4. [Domain Models](#4-domain-models)
5. [Technical Patterns](#5-technical-patterns)
6. [Data Architecture](#6-data-architecture)
7. [Integration Design](#7-integration-design)
8. [Security Architecture](#8-security-architecture)
9. [Observability Strategy](#9-observability-strategy)
10. [Non-Functional Requirements](#10-non-functional-requirements)
11. [Appendix](#11-appendix)

---

## 1. Executive Summary

### 1.1 Vision

FluxPay Engine은 **도메인 독립적인 결제/과금 엔진**으로, 어떤 서비스에도 플러그인처럼 연결하여 결제, 과금, 트래픽 제어를 처리할 수 있는 **재사용 가능한 시스템**이다.

### 1.2 Key Characteristics

| Characteristic | Description |
|----------------|-------------|
| **Reactive & Non-blocking** | Project Reactor 기반 고동시성 처리 |
| **Domain-agnostic** | 특정 비즈니스 도메인에 종속되지 않는 설계 |
| **Distributed Transaction** | Saga 패턴으로 서비스 간 일관성 보장 |
| **Event-driven** | Kafka 기반 이벤트 스트리밍 |

### 1.3 Goals & Success Criteria

| Goal | Success Metric |
|------|----------------|
| Pluggable | 기존 서비스에 1주일 내 연동 가능 |
| Eventual Consistency | 결제 승인 후 30초 내 최종 일관성 |
| Traffic Protection | 트래픽 폭주 시 시스템 정상 동작 |
| High Availability | 99.9% 가용성 |
| Performance | 1,000+ TPS, p99 < 500ms |

### 1.4 Scope

| Domain | Description | Key Capabilities |
|--------|-------------|------------------|
| **Order** | 주문 생성 및 라이프사이클 | 생성, 확인, 취소 |
| **Payment** | PG 연동 결제 처리 | 승인, 확정, 환불 |
| **Credit** | 선불 크레딧 시스템 | 충전, 사용, 환불 |
| **Subscription** | 정기 구독 관리 | 구독, 갱신, 일시정지, 취소 |

### 1.5 Technology Stack

| Layer | Technology | Selection Rationale |
|-------|------------|---------------------|
| Language | Java 21 | Pattern Matching, Records, Improved GC |
| Framework | Spring Boot 3 + WebFlux | Reactive 생태계, 성숙한 커뮤니티 |

> **참고**: WebFlux(Reactor) 기반 아키텍처를 사용하므로 Virtual Threads는 주요 I/O 경로에서 사용하지 않습니다. 단, 배치 작업이나 블로킹이 불가피한 레거시 연동에서 Virtual Threads를 선택적으로 활용할 수 있습니다.
| Build | Gradle (Groovy DSL) | 빌드 성능, 유연한 설정 |
| Database | PostgreSQL 16+ (R2DBC) | Reactive 드라이버, JSONB 지원 |
| Cache | Redis 7.x | 고성능 캐시, Lua Script 지원 |
| Messaging | Apache Kafka 3.x | 이벤트 순서 보장, 높은 처리량 |
| PG | TossPayments | 국내 주요 PG, 안정적 API |

**Observability Stack**:

| Component | Technology | Purpose |
|-----------|------------|---------|
| Metrics | Micrometer + Actuator + Prometheus | Spring Boot 표준, JVM 메트릭 포함 |
| Tracing | OpenTelemetry + Jaeger | 벤더 중립, CNCF 졸업 프로젝트 |
| Logging | Loki | Grafana 스택 통합, 라벨 기반 쿼리 |
| Alerting | Alertmanager + PagerDuty | 알림 그룹핑/라우팅, 온콜 연동 |
| Kafka Monitoring | Kafka Exporter | Consumer Lag/DLQ 모니터링 |
| Dashboard | Grafana | 유연한 시각화, 알림 통합 |
| JVM Metrics (Optional) | [rJMX-Exporter](https://github.com/jsoonworld/rJMX-Exporter) | 프로세스 격리가 필요한 경우 사이드카 방식 |

---

## 2. Implementation Phases

FluxPay Engine의 구현은 4단계로 나뉘며, 각 단계는 이전 단계를 기반으로 점진적으로 기능을 확장합니다.

### 2.1 Phase 1: Core Foundation (핵심 기반)

**목표**: 기본 인프라와 단일 결제 흐름 구현

| 영역 | 구현 내용 |
|------|----------|
| **Architecture** | Hexagonal Architecture 기본 구조, Package 구조 수립 |
| **Order Domain** | 주문 생성, 상태 관리 (PENDING → PAID → COMPLETED) |
| **Payment Domain** | 단일 결제 승인/확정/실패 처리, TossPayments 연동 |
| **Infrastructure** | PostgreSQL (R2DBC), Redis 기본 연동 |
| **API** | REST API 기본 엔드포인트, 요청 검증 |
| **Observability** | Spring Boot Actuator, 기본 로깅 (JSON 포맷) |
| **Testing** | 단위 테스트 프레임워크, TDD 프로세스 수립 |

**Exit Criteria**:
- 단일 결제 E2E 흐름 완성
- 코드 커버리지 80% 이상
- 기본 API 문서화 완료

### 2.2 Phase 2: Essential Features (필수 기능)

**목표**: 프로덕션 운영에 필요한 핵심 기능 구현

| 영역 | 구현 내용 |
|------|----------|
| **Idempotency** | 2-Layer 멱등성 (Redis + PostgreSQL), 충돌 감지 |
| **Saga Pattern** | Orchestration 기반 Saga, 보상 트랜잭션 |
| **Outbox Pattern** | Transactional Outbox, 폴링 기반 이벤트 발행 |
| **Multi-Tenancy** | 테넌트 식별 (X-Tenant-Id), 데이터 격리 (RLS) |
| **Payment Enhancement** | 환불 처리, Webhook 수신/발신 |
| **Resilience** | Circuit Breaker (Resilience4j), Timeout 정책 |
| **Observability** | Micrometer 비즈니스 메트릭, Prometheus 연동 |
| **Testing** | 통합 테스트 (Testcontainers), Contract 테스트 |

**Exit Criteria**:
- 분산 트랜잭션 시나리오 테스트 통과
- 장애 시나리오 (PG 타임아웃, Redis 장애) 대응 확인
- 멀티 테넌트 격리 검증 완료

### 2.3 Phase 3: Advanced Features (고급 기능)

**목표**: 확장성과 운영 효율성을 위한 고급 기능 구현

| 영역 | 구현 내용 |
|------|----------|
| **Credit Domain** | 선불 크레딧 시스템, 2-Phase Deduction, Ledger 패턴 |
| **Subscription Domain** | 정기 구독, 자동 갱신, 상태 머신 (TRIAL → ACTIVE → CANCELLED) |
| **Traffic Control** | Virtual Waiting Room, Rate Limiting (Token Bucket) |
| **Kafka Integration** | 이벤트 스트리밍, DLQ 처리, Consumer Lag 모니터링 |
| **Security** | API Key + HMAC 서명, Audit Logging, GDPR 익명화 |
| **Observability** | 분산 트레이싱 (OpenTelemetry + Jaeger), Grafana 대시보드 (4종) |
| **Alerting** | Alertmanager + PagerDuty, Severity 기반 알림 (P1-P4) |
| **Testing** | 부하 테스트 (k6), 카오스 엔지니어링 (Chaos Mesh) |

**Exit Criteria**:
- 1,000+ TPS 처리 확인 (부하 테스트)
- SLA 99.9% 가용성 달성
- 전체 도메인 (Order, Payment, Credit, Subscription) 기능 완성

### 2.4 Phase 4+: Future Enhancements (향후 확장)

**목표**: 운영 최적화 및 선택적 고급 기능

| 영역 | 구현 내용 | 우선순위 |
|------|----------|----------|
| **rJMX-Exporter** | 프로세스 격리 JMX 모니터링 (Jolokia + Rust 사이드카) | Optional |
| **Blue-Green 배포** | Major 버전 무중단 배포 전략 | Medium |
| **Multi-PG 지원** | 추가 PG 연동 (NicePay, KakaoPay 등) | Medium |
| **Advanced Analytics** | 실시간 결제 분석 대시보드, ML 기반 이상 탐지 | Low |
| **Global Expansion** | 다중 통화 지원 강화, 해외 PG 연동 | Low |
| **API Gateway** | Kong/Envoy 기반 API Gateway 분리 | Optional |
| **Service Mesh** | Istio 기반 서비스 메시 적용 | Optional |

**고려 사항**:
- Phase 4+ 기능은 비즈니스 요구사항에 따라 선택적으로 구현
- 각 기능의 ROI 분석 후 우선순위 결정
- 기존 Phase 1-3 기능의 안정화가 선행 조건

### 2.5 Phase Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        FluxPay Engine Implementation Roadmap                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Phase 1: Core Foundation                                                   │
│  ├── Hexagonal Architecture                                                 │
│  ├── Order & Payment Domain (Basic)                                         │
│  ├── PostgreSQL + Redis                                                     │
│  └── Basic Observability (Actuator)                                         │
│           │                                                                 │
│           ▼                                                                 │
│  Phase 2: Essential Features                                                │
│  ├── Idempotency (2-Layer)                                                  │
│  ├── Saga & Outbox Pattern                                                  │
│  ├── Multi-Tenancy                                                          │
│  └── Resilience (Circuit Breaker)                                           │
│           │                                                                 │
│           ▼                                                                 │
│  Phase 3: Advanced Features                                                 │
│  ├── Credit & Subscription Domain                                           │
│  ├── Traffic Control (Waiting Room, Rate Limit)                             │
│  ├── Kafka Event Streaming                                                  │
│  └── Full Observability (Tracing, Alerting, Dashboards)                     │
│           │                                                                 │
│           ▼                                                                 │
│  Phase 4+: Future Enhancements (Optional)                                   │
│  ├── rJMX-Exporter (Process Isolation)                                      │
│  ├── Multi-PG Support                                                       │
│  ├── Advanced Analytics                                                     │
│  └── Service Mesh / API Gateway                                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. System Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Applications                          │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ REST API (HTTPS)
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          FluxPay Engine                              │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    Presentation Layer                          │  │
│  │              Controllers, DTOs, Request Validation             │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                │                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                      Domain Layer                              │  │
│  │       Entities, Value Objects, Domain Services, Saga          │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                │                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                   Infrastructure Layer                         │  │
│  │      Repositories, PG Client, Kafka, Redis, Outbox Publisher  │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
      ┌───────────────────┼───────────────────┐
      ▼                   ▼                   ▼
┌──────────┐       ┌──────────┐       ┌──────────┐
│PostgreSQL│       │  Redis   │       │  Kafka   │
└──────────┘       └──────────┘       └──────────┘
                                            │
                   ┌────────────────────────┼────────────────────────┐
                   ▼                        ▼                        ▼
             [Service A]              [Service B]              [Service N]
```

### 3.2 Architectural Principles

**Hexagonal (Clean) Architecture** 적용:

| Principle | Description |
|-----------|-------------|
| **Dependency Rule** | 의존성은 항상 안쪽을 향함 (Infrastructure → Domain ← Presentation) |
| **Port & Adapter** | Domain이 Port(인터페이스) 정의, Infrastructure가 Adapter 구현 |
| **Domain Isolation** | 핵심 비즈니스 로직은 외부 의존성 없음 |

```
         ┌─────────────────────────────────────┐
         │           Presentation              │
         │   (Controllers, DTOs)               │
         └──────────────┬──────────────────────┘
                        │ depends on
         ┌──────────────▼──────────────────────┐
         │             Domain                  │
         │  (Entities, Services, Ports)        │
         └──────────────▲──────────────────────┘
                        │ implements
         ┌──────────────┴──────────────────────┐
         │          Infrastructure             │
         │  (Repositories, External Clients)   │
         └─────────────────────────────────────┘
```

### 3.3 Package Structure

```
com.fluxpay.engine/
├── domain/
│   ├── model/          # Entities, Value Objects
│   ├── service/        # Domain Services
│   ├── port/           # Input/Output Ports (Interfaces)
│   ├── event/          # Domain Events
│   └── saga/           # Saga Orchestration
├── infrastructure/
│   ├── persistence/    # R2DBC Repositories
│   ├── messaging/      # Kafka, Outbox
│   ├── cache/          # Redis
│   └── external/       # PG Client (TossPayments)
└── presentation/
    ├── api/            # REST Controllers
    ├── dto/            # Request/Response DTOs
    └── exception/      # Exception Handlers
```

### 3.4 Multi-Tenancy Model

FluxPay Engine은 **멀티 테넌트** 아키텍처를 지원하여 여러 클라이언트 서비스가 독립적으로 사용할 수 있습니다.

**Tenant 식별**:
- 모든 API 요청에 `X-Tenant-Id` 헤더 필수
- `tenantId`는 모든 데이터/키/이벤트의 스코프 기준

**데이터 격리**:

| 영역 | 격리 방식 |
|------|----------|
| Database | 테이블에 `tenant_id` 컬럼, Row-Level Security (RLS) 적용 |
| Cache (Redis) | Key prefix: `{tenantId}:{keyType}:{key}` |
| Idempotency | Key 구성: `{tenantId}:{endpoint}:{idempotencyKey}` |
| Events (Kafka) | 파티션 키: `{tenantId}:{aggregateId}` |
| Rate Limit | 테넌트별 독립 버킷 |

**Tenant 설정**:
```yaml
tenants:
  service-a:
    rate-limit: 1000 req/min
    credit-enabled: true
    subscription-enabled: false
  service-b:
    rate-limit: 500 req/min
    credit-enabled: true
    subscription-enabled: true
```

### 3.5 Reactive Programming Model

| Principle | Description |
|-----------|-------------|
| Non-blocking I/O | 모든 외부 호출은 Reactive (Mono/Flux) |
| No .block() | Production 코드에서 블로킹 호출 금지 |
| Backpressure | limitRate, buffer 등으로 부하 제어 |
| Error Propagation | onErrorResume, onErrorMap으로 에러 전파 |

---

## 4. Domain Models

### 4.1 Order Domain

**Purpose**: 구매 의도와 주문 라이프사이클 관리

**Core Entity**: Order (Aggregate Root)
- 주문 고유 ID, 사용자 ID, 금액
- 상태, 멱등성 키, 메타데이터
- 주문 항목 목록

**State Machine**:

```
              ┌─────────┐
              │ PENDING │
              └────┬────┘
                   │ payment approved
                   ▼
              ┌─────────┐
    ┌─────────│  PAID   │─────────┐
    │         └────┬────┘         │
    │ cancel       │ complete     │ fail
    ▼              ▼              ▼
┌───────────┐ ┌───────────┐ ┌───────────┐
│ CANCELLED │ │ COMPLETED │ │  FAILED   │
└───────────┘ └───────────┘ └───────────┘
```

| Transition | Trigger | Compensation |
|------------|---------|--------------|
| PENDING → PAID | Payment approved | Cancel payment |
| PAID → COMPLETED | Service success | - |
| PAID → FAILED | Service failure | Refund payment |
| PAID → CANCELLED | User cancellation | Refund payment |

### 4.2 Payment Domain

**Purpose**: PG 연동 결제 트랜잭션 처리

**Core Entity**: Payment
- 결제 ID, 주문 ID, 금액
- 결제 수단, 상태
- PG 거래 ID, 재시도 횟수

**State Machine**:

```
       ┌─────────┐
       │  READY  │
       └────┬────┘
            │ request
            ▼
     ┌─────────────┐
     │ PROCESSING  │
     └──────┬──────┘
            │
  ┌─────────┼─────────┐
  │ success │         │ fail
  ▼         │         ▼
┌────────┐  │    ┌────────┐
│APPROVED│  │    │ FAILED │
└───┬────┘  │    └────────┘
    │confirm│
    ▼       │
┌─────────┐ │
│CONFIRMED│ │
└────┬────┘ │
     │refund│
     ▼      │
┌─────────┐ │
│REFUNDED │ │
└─────────┘ │
```

**Business Rules**:
- 결제 승인 후 24시간 내 확정 필요
- 부분 환불 지원
- 재시도 최대 3회

### 4.3 Credit Domain

**Purpose**: 선불 크레딧/포인트 시스템

**Core Entities**:

**1. Credit (Aggregate Root)** - 잔액 스냅샷
- 사용자 ID (PK)
- 잔액, 예약 금액
- 낙관적 락 버전

**2. CreditLedger (Immutable)** - 거래 원장
- ledger_id (PK)
- user_id, tenant_id
- transaction_type: CHARGE, RESERVE, CONFIRM, CANCEL, REFUND, EXPIRE
- amount, balance_after
- reference_id (주문/결제 ID)
- created_at

**Business Rule**: `availableBalance = balance - reservedAmount`

**원장 기반 모델 (Ledger Pattern)**:

```
┌──────────────────────────────────────────────────────────────┐
│  모든 크레딧 변경은 원장(Ledger)에 불변 기록                      │
│                                                              │
│  credit_ledger (immutable, append-only)                      │
│  ┌─────────┬────────┬────────────┬─────────────────────────┐ │
│  │ user_id │ type   │ amount     │ balance_after           │ │
│  ├─────────┼────────┼────────────┼─────────────────────────┤ │
│  │ user1   │ CHARGE │ +10,000    │ 10,000                  │ │
│  │ user1   │ RESERVE│ -3,000     │ 7,000 (available)       │ │
│  │ user1   │ CONFIRM│ 0          │ 7,000 (reserved→deduct) │ │
│  │ user1   │ REFUND │ +1,500     │ 8,500                   │ │
│  └─────────┴────────┴────────────┴─────────────────────────┘ │
│                                                              │
│  credits (mutable, snapshot for fast query)                  │
│  ┌─────────┬─────────┬──────────────┬─────────┐              │
│  │ user_id │ balance │ reserved_amt │ version │              │
│  ├─────────┼─────────┼──────────────┼─────────┤              │
│  │ user1   │ 8,500   │ 0            │ 4       │              │
│  └─────────┴─────────┴──────────────┴─────────┘              │
└──────────────────────────────────────────────────────────────┘
```

> **원장의 장점**: 감사 추적, 분쟁 해결, 정산 대사에 활용 가능. 스냅샷은 빠른 조회용으로만 사용.

**2-Phase Deduction Pattern**:

```
┌──────────────────────────────────────────────────┐
│  Phase 1: Reserve                                 │
│  1. Check available balance                       │
│  2. INSERT ledger (RESERVE)                       │
│  3. UPDATE credits.reserved_amount (atomic)       │
│  4. Return reservation_id                         │
└──────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────┐
│  Phase 2: Confirm or Cancel                       │
│  Confirm: INSERT ledger (CONFIRM),                │
│           balance -= amount, reserved -= amount   │
│  Cancel:  INSERT ledger (CANCEL),                 │
│           reserved -= amount                      │
└──────────────────────────────────────────────────┘
```

### 4.4 Subscription Domain

**Purpose**: 정기 구독 및 자동 갱신 관리

**Core Entity**: Subscription
- 구독 ID, 사용자 ID, 플랜
- 상태, 결제 주기
- 현재 기간 시작/종료, 다음 결제일

**State Machine**:

```
         ┌─────────────┐
         │   CREATED   │
         └──────┬──────┘
                │ start
                ▼
         ┌─────────────┐
 ┌───────│   TRIAL     │───────┐
 │       └──────┬──────┘       │
 │cancel        │ trial ends   │expire
 │              ▼              │
 │       ┌─────────────┐       │
 │  ┌────│   ACTIVE    │────┐  │
 │  │    └──────┬──────┘    │  │
 │  │pause      │cancel     │  │
 │  ▼           ▼           │  ▼
 │ ┌──────┐ ┌───────────┐   │ ┌─────────┐
 │ │PAUSED│ │ CANCELLED │   │ │ EXPIRED │
 │ └──┬───┘ └───────────┘   │ └─────────┘
 │    │resume               │
 │    └─────────────────────┘
 └─────────────────────────────▶ [End]
```

### 4.5 Common Value Objects

| Value Object | Fields | Invariants |
|--------------|--------|------------|
| **Money** | amount, currency | amount >= 0, same currency for operations |
| **PaymentMethod** | type, token, displayName | type is valid enum |
| **Currency** | KRW (0), USD (2), JPY (0), EUR (2) | precision by currency |

---

## 5. Technical Patterns

### 5.1 Saga Pattern (Orchestration)

분산 트랜잭션 관리를 위한 Saga Orchestrator 적용.

**Payment Saga Flow**:

```
[정상 플로우]
Create Order → Process Payment → Execute Service → Complete
     │              │                   │
     └──────────────┴───────────────────┘
                    │
             [실패 시 보상]
                    │
     ┌──────────────┴───────────────────┐
     │              │                   │
Cancel Order ← Refund Payment ← (Failure Point)
```

**Saga Status**:

| Status | Description |
|--------|-------------|
| STARTED | Saga 시작 |
| PROCESSING | 단계 실행 중 |
| COMPLETED | 성공 완료 |
| COMPENSATING | 보상 트랜잭션 실행 중 |
| COMPENSATED | 보상 완료 |
| FAILED | 보상도 실패 (수동 개입 필요) |

### 5.2 Transactional Outbox Pattern

DB 트랜잭션과 이벤트 발행의 원자성 보장.

```
┌──────────────────────────────────────────────────┐
│  Application Transaction                          │
│  1. UPDATE domain state (e.g., orders.status)     │
│  2. INSERT INTO outbox_events                     │
│  3. COMMIT                                        │
└──────────────────────────────────────────────────┘
                        │
                        │ (Async Polling)
                        ▼
┌──────────────────────────────────────────────────┐
│  Outbox Publisher (100ms interval)                │
│  1. SELECT ... FOR UPDATE SKIP LOCKED (batch 100) │
│  2. Publish to Kafka                              │
│  3. UPDATE status = 'PUBLISHED'                   │
│  4. COMMIT                                        │
└──────────────────────────────────────────────────┘
```

**멀티 인스턴스 경쟁 방지**:

```sql
-- FOR UPDATE SKIP LOCKED으로 다른 인스턴스가 처리 중인 행 건너뛰기
SELECT * FROM outbox_events
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

| 전략 | 설명 |
|------|------|
| `FOR UPDATE SKIP LOCKED` | 락 경쟁 없이 각 인스턴스가 다른 이벤트 처리 |
| Batch Processing | 100건씩 처리로 DB 부하 최소화 |
| Idempotent Consumer | 소비자 측에서 `eventId`로 중복 처리 방지 |

**Consumer Idempotency**:
```java
// 소비자는 eventId로 중복 검사 필수
if (processedEventRepository.existsByEventId(event.getId())) {
    log.info("Skipping duplicate event: {}", event.getId());
    return Mono.empty();
}
```

**Guarantees**: At-least-once delivery without distributed transactions

### 5.3 Idempotency Pattern (2-Layer)

**멱등 키 구성 규칙**:

```
Full Key = {tenantId}:{endpoint}:{idempotencyKey}

예시:
- service-a:/api/v1/payments:550e8400-e29b-41d4-a716-446655440000
- service-b:/api/v1/credits/charge:7c9e6679-7425-40de-944b-e07fc1f90ae7
```

| 요소 | 설명 | 예시 |
|------|------|------|
| `tenantId` | 테넌트 식별자 | `service-a` |
| `endpoint` | API 엔드포인트 경로 | `/api/v1/payments` |
| `idempotencyKey` | 클라이언트 제공 UUID v4 | `550e8400-...` |

**Layer 1: Redis (Fast Path)**
- Lua Script로 원자적 Check-and-Set
- TTL: 24시간
- Key: `idempotency:{tenantId}:{endpoint}:{key}`

**Layer 2: PostgreSQL (Fallback)**
- UNIQUE constraint: `(tenant_id, endpoint, idempotency_key)`
- Redis 장애 시 fallback

**Flow**:

```
Request → Redis Check → (miss) → Process → Redis Store + DB Store
                     ↘ (hit) → Return Cached Response
```

**충돌 감지 (Optional)**:

동일 멱등키로 다른 페이로드가 들어온 경우 409 Conflict 반환:
```java
if (cachedPayloadHash != incomingPayloadHash) {
    throw new IdempotencyConflictException("Payload mismatch for idempotency key");
}
```

**Idempotency 처리 흐름**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Request with Idempotency Key                      │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │   Redis GETEX (atomic)   │
                    └─────────────┬───────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │ Cache HIT         │ Cache MISS        │
              ▼                   │                   ▼
    ┌─────────────────┐           │         ┌─────────────────┐
    │ Compare Payload │           │         │ Process Request │
    │      Hash       │           │         │ (Business Logic)│
    └────────┬────────┘           │         └────────┬────────┘
             │                    │                  │
     ┌───────┴───────┐            │                  ▼
     │ Match   │ Mismatch         │         ┌─────────────────┐
     ▼         ▼                  │         │ Store in Redis  │
  ┌──────┐  ┌──────────┐          │         │ + PostgreSQL    │
  │Return│  │409 Conflict│        │         │ (2-layer)       │
  │Cached│  │(VAL_002)  │         │         └────────┬────────┘
  │Result│  └──────────┘          │                  │
  └──────┘                        │                  ▼
                                  │         ┌─────────────────┐
                                  │         │ Return Response │
                                  │         └─────────────────┘
                                  │
                    ┌─────────────▼───────────┐
                    │   Redis Fallback Path   │
                    │   (Redis 장애 시)        │
                    └─────────────┬───────────┘
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │ PostgreSQL Check        │
                    │ UNIQUE constraint       │
                    └─────────────────────────┘
```

### 5.4 Virtual Waiting Room

대규모 트래픽(선착순 이벤트 등) 시 시스템 보호.

```
1. 사용자 요청 → 대기열 등록 (Redis Sorted Set)
2. Admission Controller → 동적 입장률 조절
3. 입장 허용 → Access Token 발급
4. Token 검증 → 실제 요청 처리
```

| Parameter | Default |
|-----------|---------|
| Admission Rate | 100/s |
| Token TTL | 5분 |
| Max Queue Size | 100,000 |

### 5.5 Rate Limiting

Token Bucket 알고리즘 (Redis 기반).

| Endpoint Category | Rate Limit |
|-------------------|------------|
| Payment APIs | 100 req/min per tenant+user |
| Credit APIs | 200 req/min per tenant+user |
| Query APIs | 1,000 req/min per tenant+user |
| Admin APIs | 50 req/min per tenant+user |

### 5.6 Traffic Control 우선순위

Virtual Waiting Room과 Rate Limit의 상호작용:

```
요청 → [1] Waiting Room 체크 → [2] Rate Limit 체크 → [3] 실제 처리
         (대기열 입장)         (버킷 차감)
```

| 단계 | 통과 조건 | 실패 응답 |
|------|----------|----------|
| Waiting Room | 입장 토큰 보유 또는 대기열 미적용 | 429 + 대기 순번 반환 |
| Rate Limit | 버킷에 토큰 있음 | 429 + Retry-After 헤더 |

> Waiting Room 통과 후에도 Rate Limit이 적용됩니다. Waiting Room은 시스템 전체 보호, Rate Limit은 개별 사용자/테넌트 남용 방지 목적입니다.

**시나리오별 동작 예시**:

| 시나리오 | Waiting Room | Rate Limit | 결과 |
|----------|--------------|------------|------|
| 평상시 요청 | 비활성화 | 100 req/min 내 | 정상 처리 |
| 선착순 이벤트 (대기열 활성화) | 대기열 입장 후 토큰 발급 | 토큰 보유자 100 req/min | 순차 처리 |
| 선착순 이벤트 + 과다 요청 | 토큰 보유 | 100 req/min 초과 | 429 (Rate Limit) |
| 악의적 사용자 (토큰 없이 재시도) | 토큰 없음 | - | 429 (Waiting Room) |
| 시스템 과부하 감지 | 자동 활성화, 신규 대기열 등록 | 적용 | Graceful degradation |

**응답 헤더 예시**:

```http
# Waiting Room 대기 중
HTTP/1.1 429 Too Many Requests
X-FluxPay-Queue-Position: 1523
X-FluxPay-Estimated-Wait: 45
Retry-After: 10

# Rate Limit 초과
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1707045060
Retry-After: 30
```

### 5.7 Resilience Patterns

외부 시스템 장애 시 보호를 위한 회복 탄력성 패턴:

**Circuit Breaker (Resilience4j)**:

| 대상 | 설정 | 목적 |
|------|------|------|
| PG (TossPayments) | 50% 실패 시 OPEN, 30초 대기 | PG 장애 시 빠른 실패 |
| Redis | 연결 실패 5회 시 OPEN, 10초 대기 | DB fallback 전환 |
| Kafka Producer | 3회 실패 시 OPEN, 60초 대기 | Outbox 테이블 fallback |

```java
@CircuitBreaker(name = "pg", fallbackMethod = "pgFallback")
@Retry(name = "pg")
@TimeLimiter(name = "pg")
public Mono<PaymentResult> requestPayment(PaymentRequest request) {
    return pgClient.approve(request);
}
```

**Timeout 정책**:

| 대상 | Connect Timeout | Read Timeout | Total Timeout |
|------|-----------------|--------------|---------------|
| PG API | 3s | 10s | 15s |
| Redis | 1s | 3s | 5s |
| Kafka Produce | - | - | 10s |
| DB Query | 1s | 5s | 10s |

**Bulkhead (격리)**:

| 리소스 | 격리 방식 | 설정 |
|--------|----------|------|
| PG 호출 | Semaphore | max 50 concurrent calls |
| DB Connection | R2DBC Pool | max 20 connections |
| Kafka Producer | Thread Pool | max 10 threads |

**Fallback 전략**:

| 장애 상황 | Fallback |
|----------|----------|
| PG 타임아웃 | 결제 상태를 PENDING으로 유지, 비동기 조회 스케줄링 |
| Redis 장애 | PostgreSQL idempotency_keys 테이블로 fallback |
| Kafka 장애 | Outbox 테이블에 저장, 복구 후 재발행 |

---

## 6. Data Architecture

### 6.1 Database Design

| Table | Purpose | Key Constraints |
|-------|---------|-----------------|
| orders | 주문 데이터 | UNIQUE: idempotency_key |
| order_line_items | 주문 항목 | FK: order_id |
| payments | 결제 데이터 | INDEX: pg_transaction_id |
| refunds | 환불 데이터 | FK: payment_id |
| credits | 크레딧 계정 | CHECK: balance >= reserved |
| credit_transactions | 크레딧 거래 | INDEX: user_id, created_at |
| subscriptions | 구독 | INDEX: next_billing_at |
| subscription_plans | 구독 플랜 | - |
| outbox_events | 이벤트 저장소 | INDEX: status, created_at |
| saga_instances | Saga 상태 | INDEX: correlation_id |
| idempotency_keys | 멱등성 저장 | TTL 기반 자동 삭제 |

*상세 스키마는 DB 마이그레이션 스크립트 참조*

### 6.2 Event Architecture

**Event Format**: CloudEvents v1.0 표준

**Kafka Topics**:

| Topic | Purpose | Partition Key |
|-------|---------|---------------|
| fluxpay.order.events | 주문 이벤트 | `{tenantId}:{orderId}` |
| fluxpay.payment.events | 결제 이벤트 | `{tenantId}:{paymentId}` |
| fluxpay.credit.events | 크레딧 이벤트 | `{tenantId}:{userId}` |
| fluxpay.subscription.events | 구독 이벤트 | `{tenantId}:{subscriptionId}` |
| fluxpay.dlq.* | Dead Letter Queue | 원본 키 유지 |

> **파티션 키 설계 원칙**: `userId` 단일 키는 핫 파티션 위험이 있으므로, `tenantId + aggregateId` 조합으로 부하를 분산합니다. 동일 Aggregate 내 이벤트 순서만 보장하면 충분합니다.

**Event Types**:

| Domain | Events |
|--------|--------|
| Order | order.created, order.completed, order.cancelled, order.failed |
| Payment | payment.approved, payment.confirmed, payment.failed, payment.refunded |
| Credit | credit.charged, credit.deducted, credit.refunded |
| Subscription | subscription.activated, subscription.renewed, subscription.cancelled, subscription.expired |

**Event Guarantees**:

| Guarantee | Implementation |
|-----------|----------------|
| Delivery | At-least-once |
| Ordering | Per aggregate (partition by tenantId:aggregateId) |
| Retry | 3회 후 DLQ |
| Backoff | Exponential (1s, 2s, 4s) |

---

## 7. Integration Design

### 7.1 External Integration Points

| Direction | System | Protocol | Purpose |
|-----------|--------|----------|---------|
| Outbound | TossPayments | REST/HTTPS | 결제 승인/취소/환불 |
| Inbound | Client Services | REST API | 주문/결제/크레딧 요청 |
| Outbound | Client Services | Kafka | 상태 변경 이벤트 알림 |

### 7.2 API Design Principles

| Principle | Implementation |
|-----------|----------------|
| RESTful | Resource-oriented endpoints |
| Idempotency | X-Idempotency-Key header (UUID v4) |
| Versioning | URL path (/api/v1/) |
| Pagination | Cursor-based for large collections |
| Error Format | Consistent error response structure |

**Error Code Convention**: `{DOMAIN}_{NUMBER}`
- PAY_001 ~ PAY_099: Payment errors
- ORD_001 ~ ORD_099: Order errors
- CRD_001 ~ CRD_099: Credit errors
- SUB_001 ~ SUB_099: Subscription errors
- SYS_001 ~ SYS_099: System errors
- VAL_001 ~ VAL_099: Validation errors

**Error Response Schema**:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PAY_001",
    "message": "Payment not found",
    "details": {
      "paymentId": "pay_123456",
      "hint": "Check if the payment ID is correct"
    }
  },
  "metadata": {
    "timestamp": "2026-02-04T10:30:00Z",
    "traceId": "abc-123-def",
    "requestId": "req-789"
  }
}
```

**Error Code → HTTP Status Mapping**:

| Code | HTTP Status | Description |
|------|-------------|-------------|
| PAY_001 | 404 Not Found | 결제를 찾을 수 없음 |
| PAY_002 | 400 Bad Request | 잘못된 결제 금액 |
| PAY_003 | 409 Conflict | 이미 처리된 결제 |
| PAY_004 | 422 Unprocessable | 지원하지 않는 결제 수단 |
| PAY_005 | 502 Bad Gateway | PG 연동 오류 |
| PAY_006 | 400 Bad Request | 잘못된 결제 상태 전이 |
| ORD_001 | 404 Not Found | 주문을 찾을 수 없음 |
| ORD_002 | 409 Conflict | 이미 완료된 주문 |
| CRD_001 | 404 Not Found | 크레딧 계정 없음 |
| CRD_002 | 422 Unprocessable | 잔액 부족 |
| SYS_001 | 500 Internal Error | 내부 서버 오류 |
| SYS_002 | 503 Service Unavailable | 서비스 일시 불가 |
| SYS_003 | 504 Gateway Timeout | 외부 서비스 타임아웃 |
| SYS_004 | 429 Too Many Requests | 요청 한도 초과 |
| VAL_001 | 400 Bad Request | 요청 유효성 검증 실패 |
| VAL_002 | 409 Conflict | 멱등키 페이로드 불일치 |

*상세 API 스펙은 OpenAPI/Swagger 문서 참조*

### 7.3 PG Payment Flow (Async/Redirect)

대부분의 PG 결제는 비동기/리디렉션 기반입니다. FluxPay는 다음 시나리오를 모두 지원합니다.

**결제 승인 흐름 (3DS/간편결제 포함)**:

```
[클라이언트 앱]                [FluxPay]                    [PG: 토스페이먼츠]
      │                           │                              │
      │ POST /payments/request    │                              │
      │──────────────────────────▶│                              │
      │                           │ POST /payments (승인요청)     │
      │                           │─────────────────────────────▶│
      │                           │                              │
      │  ◀────────────────────────│ 302 Redirect (3DS/인증 페이지) │
      │                           │◀─────────────────────────────│
      │                           │                              │
      │ [사용자가 인증 완료]        │                              │
      │                           │                              │
      │                           │ Callback: /payments/callback  │
      │                           │◀─────────────────────────────│
      │                           │                              │
      │                           │ POST /payments/confirm       │
      │                           │─────────────────────────────▶│
      │                           │                              │
      │                           │ 200 OK (결제 확정)            │
      │                           │◀─────────────────────────────│
      │                           │                              │
      │ Event: payment.confirmed   │                              │
      │◀──────────────────────────│                              │
```

**승인-확정 분리 (2-Phase Commit)**:

| Phase | 설명 | 유효 시간 |
|-------|------|----------|
| 승인 (Approve) | PG에서 결제 수단 유효성 확인, 금액 홀드 | 24시간 |
| 확정 (Confirm) | 실제 청구 확정, 서비스 실행 후 호출 | 승인 후 24시간 내 |

> 서비스 실행 실패 시 승인만 취소하여 고객 청구 방지

### 7.4 Webhook 수신 (PG → FluxPay)

PG에서 FluxPay로 보내는 웹훅 처리 정책:

| 정책 | 구현 |
|------|------|
| **멱등성** | `pgTransactionId`로 중복 처리 방지 |
| **순서 보장** | 이벤트 타임스탬프 기준 out-of-order 처리 |
| **서명 검증** | HMAC-SHA256으로 요청 위변조 검증 |
| **재시도 대응** | PG 재시도 시 동일 응답 반환 (idempotent) |

**Out-of-Order 처리**:
```
// 이미 CONFIRMED 상태인데 APPROVED 웹훅이 늦게 도착한 경우
if (currentStatus.ordinal() > incomingStatus.ordinal()) {
    log.info("Ignoring outdated webhook: current={}, incoming={}", currentStatus, incomingStatus);
    return Mono.just(ResponseEntity.ok().build()); // 200 OK로 재시도 방지
}
```

### 7.5 Webhook 발신 (FluxPay → Client)

| Event | Trigger | Retry Policy |
|-------|---------|--------------|
| Payment Completed | 결제 확정 후 | 3회, exponential backoff (1s, 2s, 4s) |
| Payment Failed | 결제 실패 후 | 3회, exponential backoff |
| Refund Completed | 환불 완료 후 | 3회, exponential backoff |
| Service Callback | 서비스 실행 결과 | 클라이언트 → FluxPay |

**웹훅 실패 처리**:
- 3회 실패 시 DLQ 저장
- 클라이언트는 `/webhooks/retry` API로 수동 재시도 가능
- 웹훅 이력 조회 API 제공

---

## 8. Security Architecture

### 8.1 Authentication & Authorization

| Layer | Method |
|-------|--------|
| API Authentication | API Key + HMAC-SHA256 Signature |
| Internal Services | mTLS (Mutual TLS) |
| Admin Access | OAuth 2.0 + RBAC |

**Signature Algorithm**:
```
StringToSign = METHOD + "\n" +
               CONTENT_TYPE + "\n" +
               TIMESTAMP + "\n" +
               NONCE + "\n" +
               CanonicalURI + "\n" +
               CanonicalQueryString + "\n" +
               SHA256(RequestBody)

Signature = HMAC-SHA256(SecretKey, StringToSign)
```

**서명 규칙**:
- `TIMESTAMP`: ISO 8601 형식 (e.g., `2026-02-04T10:30:00Z`)
- `NONCE`: UUID v4, 요청당 고유값 (replay attack 방지)
- `CanonicalQueryString`: 파라미터를 key 알파벳 순 정렬 후 URL 인코딩
- `Timestamp 허용 오차`: ±5분 (서버 시간 기준)
- `NONCE 유효 시간`: 5분 내 중복 불가 (Redis로 검증)

### 8.2 Data Protection

| Data Type | Protection |
|-----------|------------|
| Card Information | **저장 안 함** (PG 토큰화) |
| PII | AES-256-GCM 암호화 |
| Encryption Keys | AWS KMS / HashiCorp Vault |
| API Secrets | Environment Variables / Secret Manager |

### 8.3 Network Security

| Layer | Implementation |
|-------|----------------|
| External Traffic | TLS 1.3 필수 |
| Internal Traffic | mTLS with auto-rotation |
| Actuator Endpoints | 내부 네트워크만 허용 |

### 8.4 Audit & Compliance

| Requirement | Implementation |
|-------------|----------------|
| Audit Logging | 모든 결제/환불 요청 기록 |
| Log Retention | 7년 (전자금융거래법) |
| PCI-DSS | SAQ-A (카드 정보 미저장) |
| GDPR | 개인정보 처리 및 삭제 지원 (아래 익명화 전략 참조) |

**GDPR 삭제 vs 7년 보관 충돌 해결**:

전자금융거래법 7년 보관 의무와 GDPR 삭제권(Right to Erasure) 충돌을 **익명화/가명처리**로 해결합니다.

```
GDPR 삭제 요청 시:
1. PII 필드 익명화 (이름, 이메일, 전화번호 등)
   - name: "김철수" → "USER_DELETED_abc123"
   - email: "kim@example.com" → "deleted_abc123@anonymized.local"
   - phone: "010-1234-5678" → null

2. 거래 기록 보존 (법적 의무)
   - paymentId, amount, timestamp 등 유지
   - 감사 로그에 "GDPR 삭제 처리됨" 기록

3. 연관 데이터 정리
   - 캐시에서 사용자 데이터 삭제
   - 개인 식별 불가한 집계 데이터만 유지
```

| 데이터 유형 | GDPR 삭제 시 처리 |
|-------------|------------------|
| 결제/거래 기록 | 익명화 후 7년 보관 |
| 사용자 프로필 | 완전 삭제 |
| 로그 내 PII | 마스킹 처리 |
| 분석용 데이터 | 비식별화 후 유지 |

---

## 9. Observability Strategy

### 9.1 Monitoring Architecture

FluxPay Engine은 **Spring Boot 표준 모니터링**을 기본으로 사용합니다. 추가적인 모니터링 도구는 선택적으로 연동할 수 있습니다.

**기본 구성 (Phase 1-3)**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Monitoring Stack                                 │
│                                                                         │
│  ┌─────────────┐     ┌──────────────┐     ┌─────────────────────────┐  │
│  │   Grafana   │◀────│  Prometheus  │────▶│  Alertmanager/PagerDuty │  │
│  │ (Dashboard) │     │  (Time-Series│     │  (알림 라우팅)            │  │
│  └─────────────┘     │    DB)       │     └─────────────────────────┘  │
│                      └──────┬───────┘                                   │
│                             │ scrape                                    │
│              ┌──────────────┴──────────────┐                           │
│              ▼                             ▼                           │
│      ┌─────────────────────┐       ┌─────────────┐                    │
│      │   /actuator/        │       │   Kafka    │                    │
│      │   prometheus        │       │  Exporter  │                    │
│      │ (Micrometer 기본)    │       │            │                    │
│      │ - 비즈니스 메트릭    │       └─────────────┘                    │
│      │ - JVM 메트릭        │                                          │
│      │ - R2DBC 메트릭      │                                          │
│      └──────────┬──────────┘                                          │
│                 │                                                      │
│  ┌──────────────▼───────────────────────────────────────────────────┐ │
│  │                     FluxPay Engine (JVM)                          │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐                    │ │
│  │  │Micrometer│  │ OpenTele │  │Structured Log│                    │ │
│  │  │ Registry │  │ metry    │  │ (JSON→Loki)  │                    │ │
│  │  └──────────┘  └─────┬────┘  └──────────────┘                    │ │
│  └──────────────────────┼───────────────────────────────────────────┘ │
│                         ▼                                             │
│                ┌──────────────┐                                       │
│                │    Jaeger    │                                       │
│                │  (Tracing)   │                                       │
│                └──────────────┘                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

**메트릭 수집 전략**:

| 수집기 | 역할 | 수집 대상 |
|--------|------|----------|
| Micrometer (기본) | 비즈니스 + JVM 메트릭 | 결제/주문/크레딧 처리량, 힙/GC/쓰레드 |
| Kafka Exporter | 메시징 메트릭 | Consumer Lag, DLQ 메시지 |

> **Spring Boot Actuator**는 Micrometer를 통해 JVM 메트릭(힙, GC, 쓰레드 등)을 기본 제공합니다. 별도의 JMX Exporter 없이도 `/actuator/prometheus` 엔드포인트에서 모든 메트릭을 수집할 수 있습니다.

### 9.2 JVM Metrics (Micrometer 기본 제공)

Spring Boot Actuator + Micrometer가 기본 제공하는 JVM 메트릭:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, info, metrics
  metrics:
    tags:
      application: fluxpay-engine
    export:
      prometheus:
        enabled: true
```

**기본 제공 JVM 메트릭**:
- `jvm_memory_used_bytes` / `jvm_memory_max_bytes` (힙/논힙)
- `jvm_gc_pause_seconds` (GC 일시정지 시간)
- `jvm_threads_live_threads` (활성 쓰레드 수)
- `jvm_classes_loaded_classes` (로드된 클래스 수)
- `r2dbc_pool_*` (R2DBC 커넥션 풀 상태)

### 9.3 Optional: rJMX-Exporter 연동

> **Phase 4+ 선택 사항**: 프로세스 격리가 필요하거나 세밀한 JMX MBean 수집이 필요한 경우에만 적용

[rJMX-Exporter](https://github.com/jsoonworld/rJMX-Exporter)는 Rust 기반 경량 JMX 메트릭 수집기로, Jolokia HTTP/JSON 프로토콜을 통해 JVM 메트릭을 수집하여 Prometheus 형식으로 노출합니다.

**rJMX-Exporter vs 기본 Micrometer**:

| 항목 | Micrometer (기본) | rJMX-Exporter (선택) |
|------|-------------------|---------------------|
| 설정 난이도 | 매우 쉬움 (의존성 추가만) | 중간 (Jolokia + 사이드카 배포) |
| 추가 의존성 | 없음 | Jolokia Agent 필요 |
| 프로세스 격리 | 없음 (동일 JVM) | **있음 (별도 프로세스)** |
| 메모리 오버헤드 | ~0 (기본 포함) | <10MB (사이드카) |
| 세밀한 MBean 접근 | 제한적 | **가능** |

**언제 rJMX-Exporter를 사용하나요?**
- 모니터링 장애가 애플리케이션에 영향을 주면 안 되는 경우
- 기본 Micrometer가 제공하지 않는 커스텀 MBean 수집이 필요한 경우
- 사이드카 패턴으로 모니터링을 완전히 분리하고 싶은 경우

**rJMX-Exporter 연동 시 추가 설정**:

1. Jolokia Agent 추가 (build.gradle):
```groovy
dependencies {
    runtimeOnly 'org.jolokia:jolokia-core:1.7.2'
}
```

2. Jolokia 보안 설정:

| 환경 | 바인딩 주소 | 접근 제어 |
|------|------------|----------|
| 로컬 개발 | `127.0.0.1:8778` | localhost만 접근 가능 |
| K8s | `0.0.0.0:8778` + NetworkPolicy | rJMX-Exporter Pod만 접근 |

3. Kubernetes NetworkPolicy:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: jolokia-access
spec:
  podSelector:
    matchLabels:
      app: fluxpay-engine
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: rjmx-exporter
      ports:
        - port: 8778
```

### 9.4 Metrics

#### 9.4.1 Business Metrics (Micrometer)

| 메트릭명 | 타입 | 설명 | 알림 임계값 |
|----------|------|------|-------------|
| `fluxpay_payment_total` | Counter | 결제 시도 수 (status 라벨) | - |
| `fluxpay_payment_success_rate` | Gauge | 결제 성공률 | < 99.9% (P2), < 99% (P1) |
| `fluxpay_payment_latency_seconds` | Histogram | 결제 처리 시간 | p99 > 500ms (P3) |
| `fluxpay_order_created_total` | Counter | 주문 생성 수 | - |
| `fluxpay_credit_balance_total` | Gauge | 전체 크레딧 잔액 합계 | - |
| `fluxpay_credit_transactions_total` | Counter | 크레딧 거래 수 (type: charge/use/refund) | - |
| `fluxpay_saga_compensation_total` | Counter | Saga 보상 트랜잭션 수 | > 10건/min (P2) |
| `fluxpay_outbox_pending_count` | Gauge | 미발행 Outbox 이벤트 | > 100 (P3) |
| `fluxpay_waiting_room_queue_size` | Gauge | 대기열 크기 | > 10,000 |
| `fluxpay_idempotent_hit_total` | Counter | 멱등 키 캐시 히트 | - |

#### 9.4.2 JVM Metrics (Micrometer 기본 제공)

| 메트릭명 | 설명 | 알림 임계값 |
|----------|------|-------------|
| `jvm_memory_used_bytes{area="heap"}` | 힙 메모리 사용량 | > 80% of max |
| `jvm_memory_max_bytes{area="heap"}` | 힙 메모리 최대값 | - |
| `jvm_memory_used_bytes{area="nonheap"}` | Non-Heap 메모리 사용량 | - |
| `jvm_gc_pause_seconds_count` | GC 일시정지 횟수 | - |
| `jvm_gc_pause_seconds_sum` | GC 일시정지 총 시간 | > 5s/min |
| `jvm_threads_live_threads` | 현재 활성 쓰레드 수 | > 500 |
| `jvm_threads_states_threads{state="blocked"}` | 블록된 쓰레드 | > 10 (P2) |
| `jvm_classes_loaded_classes` | 로드된 클래스 수 | - |
| `r2dbc_pool_acquired` | R2DBC 사용 중인 커넥션 | > 80% of max |
| `r2dbc_pool_pending` | R2DBC 대기 중인 요청 | > 10 (P3) |

#### 9.4.3 Infrastructure Metrics

| 대상 | 메트릭 | 알림 임계값 |
|------|--------|-------------|
| Kafka | Consumer Lag | > 10,000 |
| Kafka | DLQ 메시지 수 | > 0 (P2) |
| Redis | 메모리 사용률 | > 80% |
| Redis | 연결 수 | > 1,000 |
| PostgreSQL | Active Connections | > 80% of max |
| PostgreSQL | Replication Lag | > 1s |

### 9.5 Distributed Tracing

| Component | Technology |
|-----------|------------|
| SDK | OpenTelemetry |
| Backend | Jaeger |
| Propagation | W3C Trace Context (HTTP), Message Headers (Kafka) |
| Correlation | traceId, spanId in all logs |

**추적 ID 전파**:
- X-Request-Id 헤더를 모든 로그/이벤트에 전파
- Kafka 메시지 헤더에 trace context 포함

### 9.6 Logging

| Aspect | Standard |
|--------|----------|
| Format | Structured JSON |
| Aggregation | Loki (Grafana 스택 통합) |
| Correlation | traceId, spanId in all logs |
| Query | 라벨 기반 쿼리 (LogQL) |

### 9.7 Alerting

#### 9.7.1 Severity Classification

| Severity | Response Time | Example |
|----------|---------------|---------|
| P1 (Critical) | 5분 | 결제 전면 장애, 성공률 99% 미만, 데이터 유실 위험 |
| P2 (High) | 30분 | 성공률 99.9% 미만, Saga 보상 급증 (10건/분 초과) |
| P3 (Medium) | 4시간 | 응답 지연 (p99 > 500ms), 메모리 경고 |
| P4 (Low) | 다음 업무일 | 경미한 성능 저하 |

#### 9.7.2 Alert Rules (Prometheus)

```yaml
groups:
  - name: fluxpay-critical
    rules:
      # P1: 결제 시스템 다운
      - alert: PaymentSystemDown
        expr: up{job="fluxpay"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "FluxPay Engine is down"
          description: "Payment system has been unreachable for more than 1 minute"

      # P1: 결제 성공률 급락
      - alert: PaymentSuccessRateCritical
        expr: fluxpay_payment_success_rate < 0.99
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Payment success rate critical"
          description: "Payment success rate is below 99% for 2 minutes"

      # P1: JVM 데드락
      - alert: JvmDeadlockDetected
        expr: jvm_threads_deadlocked > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "JVM deadlock detected"

  - name: fluxpay-high
    rules:
      # P2: 결제 성공률 저하
      - alert: PaymentSuccessRateLow
        expr: fluxpay_payment_success_rate < 0.999
        for: 5m
        labels:
          severity: high
        annotations:
          summary: "Payment success rate below SLO"
          description: "Payment success rate is below 99.9% for 5 minutes"

      # P2: Saga 보상 급증 (10건/분 초과)
      - alert: SagaCompensationSpike
        expr: increase(fluxpay_saga_compensation_total[1m]) > 10
        for: 2m
        labels:
          severity: high
        annotations:
          summary: "Saga compensation rate spike"

      # P2: DLQ 메시지 발생
      - alert: DlqMessagesDetected
        expr: kafka_dlq_messages_total > 0
        for: 1m
        labels:
          severity: high

  - name: fluxpay-medium
    rules:
      # P3: Outbox 적체
      - alert: OutboxBacklog
        expr: fluxpay_outbox_pending_count > 100
        for: 10m
        labels:
          severity: medium
        annotations:
          summary: "Outbox event backlog"

      # P3: JVM 힙 메모리 경고
      - alert: JvmHeapMemoryHigh
        expr: jvm_memory_heap_used_bytes / jvm_memory_heap_max_bytes > 0.8
        for: 5m
        labels:
          severity: medium
        annotations:
          summary: "JVM heap memory usage high"

      # P3: API 응답 지연
      - alert: ApiLatencyHigh
        expr: histogram_quantile(0.99, rate(fluxpay_payment_latency_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: medium
        annotations:
          summary: "API p99 latency exceeds 500ms"

      # P3: GC 시간 과다
      - alert: JvmGcTimeHigh
        expr: rate(jvm_gc_collection_seconds_sum[1m]) > 5
        for: 5m
        labels:
          severity: medium
```

### 9.8 Dashboards

#### 9.8.1 Executive Dashboard (경영진용)
- 일별 결제 처리량 및 금액
- 결제 성공률 추이 (일/주/월)
- 주요 장애 타임라인
- 월간 SLA 달성률

#### 9.8.2 Operations Dashboard (운영팀용)
- 실시간 TPS 및 응답 시간
- 에러율 및 에러 유형 분포
- 대기열 상태 (Virtual Waiting Room)
- Kafka Consumer Lag
- DLQ 메시지 현황
- Outbox 발행 지연

#### 9.8.3 Engineering Dashboard (개발팀용)
- JVM 힙/Non-Heap/GC 상태 (Micrometer 기본 제공)
- 쓰레드 풀 상태 (WebFlux Event Loop)
- DB 커넥션 풀 상태 (R2DBC)
- Saga 상태 전이 흐름
- Redis 연결/메모리 상태

#### 9.8.4 SLA Dashboard
- 월간 가용성 (목표: 99.9%)
- API 응답 시간 SLO 달성률 (p95 < 200ms, p99 < 500ms)
- 중복 결제 발생 건수 (목표: 0건)
- 데이터 일관성 지연 (목표: 30초 이내)

---

## 10. Non-Functional Requirements

### 10.1 Performance

| Metric | Target |
|--------|--------|
| API Response (p95) | < 200ms |
| API Response (p99) | < 500ms |
| Throughput | > 1,000 TPS |
| Payment E2E | < 3s |

### 10.2 Availability & Resilience

| Metric | Target |
|--------|--------|
| System Availability | 99.9% uptime |
| Recovery Time (RTO) | < 15 minutes |
| Recovery Point (RPO) | < 1 minute |
| Planned Downtime | < 4 hours/month |

### 10.3 Scalability

| Dimension | Strategy |
|-----------|----------|
| Horizontal | Stateless design, K8s auto-scaling |
| Database | Read replicas, Connection pooling (R2DBC) |
| Cache | Redis Cluster |
| Messaging | Kafka partition 증설 |

### 10.4 Data Management

| Requirement | Policy |
|-------------|--------|
| Retention | 결제 데이터 7년 |
| Backup | Daily full, Hourly incremental |
| Consistency | 30초 내 최종 일관성 |
| Encryption | AES-256 at rest |

### 10.5 Consistency Model

작업 유형에 따른 일관성 수준:

| 작업 유형 | 일관성 수준 | 최대 지연 | 설명 |
|----------|------------|----------|------|
| 결제 승인/확정 | **Strong** | 즉시 | 동기 트랜잭션, 단일 DB 작업 |
| 크레딧 차감 | **Strong** | 즉시 | 낙관적 락으로 원자성 보장 |
| 크레딧 잔액 조회 | **Strong** | 즉시 | Primary DB 직접 조회 |
| 주문 상태 변경 | **Eventual** | 30초 | Saga 보상 완료 시간 기준 |
| 이벤트 발행 | **Eventual** | 30초 | Outbox polling interval 포함 |
| 구독 갱신 알림 | **Eventual** | 1분 | 스케줄러 주기 포함 |
| 대시보드 집계 | **Eventual** | 5분 | 배치 집계 주기 |

> **Strong Consistency가 필요한 작업**: 금액 관련 모든 변경(결제, 환불, 크레딧 차감/충전)은 즉시 일관성 보장
> **Eventual Consistency 허용 작업**: 알림, 집계, 조회용 캐시 갱신

---

## 11. Appendix

### A. Glossary

| Term | Definition |
|------|------------|
| Aggregate | 하나의 단위로 취급되는 도메인 객체 클러스터 |
| Saga | 분산 트랜잭션을 관리하는 패턴 |
| Outbox | 트랜잭션 내에서 이벤트를 저장하는 패턴 |
| Idempotency | 동일 요청을 여러 번 실행해도 결과가 같은 속성 |
| PG | Payment Gateway (결제 대행사) |
| DLQ | Dead Letter Queue (처리 실패 메시지 큐) |

### B. External Dependencies

| Dependency | Purpose | Fallback |
|------------|---------|----------|
| TossPayments | 결제 처리 | N/A (Primary) |
| PostgreSQL | 데이터 저장 | Read replica |
| Redis | 캐시, 멱등성 | DB fallback |
| Kafka | 이벤트 발행 | Outbox retry |

### C. Related Documents

| Document | Purpose |
|----------|---------|
| API Specification (OpenAPI) | 상세 API 엔드포인트 정의 |
| Database Schema | 테이블 DDL 및 마이그레이션 |
| Event Schema | 이벤트 페이로드 상세 정의 |
| Runbook | 운영 가이드 및 장애 대응 |
| [CLAUDE.md](../CLAUDE.md) | TDD 규칙, 코딩 컨벤션, 개발 가이드 |

### C.1 Operational Runbook (장애 대응)

**P1 장애: 결제 시스템 다운**

```
증상: PaymentSystemDown 알림, 결제 API 응답 없음
1. kubectl get pods -l app=fluxpay-engine  # Pod 상태 확인
2. kubectl logs -l app=fluxpay-engine --tail=100  # 최근 로그 확인
3. 원인별 대응:
   - OOM: Pod 재시작 확인 → 메모리 limit 증가 또는 GC 튜닝
   - DB 연결 실패: PostgreSQL 상태 확인, R2DBC pool 설정 점검
   - 네트워크 장애: NetworkPolicy 및 Service 확인
4. 복구 후: 진행 중 Saga 상태 확인 (COMPENSATING 상태 처리)
```

**P1 장애: 결제 성공률 급락 (<99%)**

```
증상: PaymentSuccessRateCritical 알림
1. Grafana에서 에러 유형별 분포 확인
2. 원인별 대응:
   - PG 장애 (PAY_005 급증): PG 상태 페이지 확인, Circuit Breaker 상태 확인
   - 잘못된 배포: 직전 배포 롤백 (아래 롤백 절차 참조)
   - DB 타임아웃: slow query 확인, connection pool 상태 점검
3. PG 장애 시:
   - Circuit Breaker OPEN 확인 → 빠른 실패로 사용자 경험 보호
   - PG 복구 시 자동 HALF_OPEN → CLOSED 전이 확인
4. 고객 안내 필요 시 상태 페이지 업데이트
```

**P2 장애: Saga 보상 급증**

```
증상: SagaCompensationSpike 알림 (10건/분 초과)
1. saga_instances 테이블에서 COMPENSATING 상태 조회
   SELECT * FROM saga_instances WHERE status = 'COMPENSATING' ORDER BY created_at DESC;
2. 공통 실패 원인 파악 (order_id, payment_id 패턴 분석)
3. 원인별 대응:
   - 외부 서비스 장애: 해당 서비스 팀 연락
   - 데이터 정합성 이슈: 수동 보상 트랜잭션 실행
4. FAILED 상태 Saga 수동 처리 필요 (재무팀 협의)
```

**P2 장애: DLQ 메시지 발생**

```
증상: DlqMessagesDetected 알림
1. Kafka DLQ 토픽에서 실패 메시지 확인
   kafka-console-consumer --topic fluxpay.dlq.payment.events --from-beginning
2. 실패 원인 파악 (deserialization 오류, 처리 로직 버그 등)
3. 대응:
   - 일시적 오류: DLQ 메시지 재처리 API 호출
   - 영구적 오류: 메시지 아카이브 후 수동 처리
4. 동일 패턴 재발 방지 위한 로직 수정 및 배포
```

**P3 장애: Outbox 적체**

```
증상: OutboxBacklog 알림 (>100건)
1. outbox_events 테이블 상태 확인
   SELECT status, COUNT(*) FROM outbox_events GROUP BY status;
2. 원인별 대응:
   - Kafka 연결 실패: Kafka 클러스터 상태 확인
   - 처리 지연: Outbox polling 인스턴스 수 확인
   - DB 락 경합: FOR UPDATE SKIP LOCKED 적용 확인
3. 긴급 시 수동 이벤트 발행 스크립트 실행
```

### C.2 Deployment & Rollback Strategy

**배포 전략: Rolling Update (기본)**

```yaml
# Kubernetes Deployment 설정
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # 한 번에 1개 Pod 추가
      maxUnavailable: 0  # 항상 최소 replicas 유지
  replicas: 3
```

| 단계 | 설명 | 검증 |
|------|------|------|
| 1. Pre-deploy | Feature flag OFF 확인, DB 마이그레이션 완료 | CI/CD 파이프라인 |
| 2. Canary (선택) | 1개 Pod만 신규 버전 배포 | 5분간 에러율 모니터링 |
| 3. Rolling | 점진적 교체 (1 pod씩) | 각 Pod 준비 확인 |
| 4. Post-deploy | 전체 Pod 신규 버전 확인 | Smoke test 실행 |

**롤백 절차**:

```bash
# 방법 1: kubectl rollout (권장)
kubectl rollout undo deployment/fluxpay-engine -n fluxpay

# 방법 2: 특정 리비전으로 롤백
kubectl rollout history deployment/fluxpay-engine -n fluxpay
kubectl rollout undo deployment/fluxpay-engine -n fluxpay --to-revision=<N>

# 롤백 후 확인
kubectl rollout status deployment/fluxpay-engine -n fluxpay
```

**롤백 트리거 조건**:

| 조건 | 자동/수동 | 설명 |
|------|----------|------|
| Pod 시작 실패 (CrashLoopBackOff) | 자동 | Liveness probe 3회 실패 |
| 결제 성공률 <99% (배포 후 5분 내) | 수동 | 모니터링 기반 판단 |
| P1 알림 발생 | 수동 | 온콜 엔지니어 판단 |
| E2E 테스트 실패 | 자동 | CD 파이프라인 게이트 |

**Blue-Green 배포 (Major 버전)**:

```
┌─────────────────┐     ┌─────────────────┐
│   Blue (v1.x)   │     │  Green (v2.0)   │
│   (현재 Live)    │     │  (대기)         │
└────────┬────────┘     └────────┬────────┘
         │                       │
         └───────┬───────────────┘
                 │
         ┌───────▼───────┐
         │   Ingress     │
         │ (Traffic 전환) │
         └───────────────┘

1. Green 환경에 v2.0 배포 및 검증
2. Ingress에서 트래픽 전환 (Blue → Green)
3. Blue 환경 1시간 유지 (즉시 롤백 대비)
4. 안정화 후 Blue 환경 제거
```

**DB 마이그레이션 전략**:

| 변경 유형 | 전략 | 롤백 |
|----------|------|------|
| 컬럼 추가 (nullable) | 배포 전 마이그레이션 | 컬럼 삭제 (다음 배포) |
| 컬럼 제거 | 3단계: 미사용 → 배포 → 제거 | 컬럼 재생성 |
| 인덱스 추가 | CONCURRENTLY 옵션 사용 | 인덱스 삭제 |
| 테이블 변경 | Expand-Contract 패턴 | 이전 스키마 복원 |

### D. Configuration Parameters

| Category | Key Parameters |
|----------|----------------|
| Payment | retry.max-attempts, timeout |
| Idempotency | ttl (default: 24h) |
| Outbox | polling-interval, batch-size |
| Waiting Room | admission-rate, token-ttl |
| Rate Limit | requests-per-minute by endpoint category |

**Observability Configuration**:

| Component | Parameter | Default |
|-----------|-----------|---------|
| Prometheus Scrape | /actuator/prometheus | 15s interval |
| Alertmanager | group_wait | 30s |
| Alertmanager | group_interval | 5m |
| Alertmanager | repeat_interval | 4h |
| rJMX-Exporter (Optional) | port | 9090 |
| Jolokia Agent (Optional) | port | 8778 |
| Jolokia Agent (Optional) | host binding | 127.0.0.1 (localhost only) |

*상세 설정값은 application.yml 참조*

### E. Test Strategy

> **참고**: TDD 필수 규칙 및 상세 테스트 패턴은 [CLAUDE.md](../CLAUDE.md) 참조

**테스트 피라미드**:

| 테스트 유형 | 커버리지 목표 | 도구 | 실행 시점 |
|------------|--------------|------|----------|
| Unit Tests | 90%+ (Domain), 80%+ (전체) | JUnit 5, AssertJ, StepVerifier | 모든 커밋 |
| Integration Tests | 80%+ | Testcontainers, WebTestClient | PR 머지 전 |
| Contract Tests | API 변경 시 | Spring Cloud Contract | PR 머지 전 |
| E2E Tests | Critical paths | Playwright/Cypress | 배포 전 |

**Reactive 테스트 패턴 (StepVerifier)**:

```java
// 성공 케이스 검증
@Test
void shouldProcessPaymentSuccessfully() {
    // Given
    Payment payment = createTestPayment(PaymentStatus.READY);

    // When
    Mono<Payment> result = paymentService.process(payment);

    // Then
    StepVerifier.create(result)
        .assertNext(processed -> {
            assertThat(processed.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(processed.getApprovedAt()).isNotNull();
        })
        .verifyComplete();
}

// 에러 케이스 검증
@Test
void shouldFailWhenBalanceInsufficient() {
    // Given
    CreditDeductionRequest request = new CreditDeductionRequest(
        "user-123", Money.of(100000, "KRW")
    );

    // When
    Mono<CreditTransaction> result = creditService.deduct(request);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(ex ->
            ex instanceof InsufficientBalanceException &&
            ex.getMessage().contains("Insufficient balance"))
        .verify();
}

// 시간 기반 테스트 (Virtual Time)
@Test
void shouldTimeoutWhenPgResponseDelayed() {
    StepVerifier.withVirtualTime(() -> pgClient.approve(request))
        .expectSubscription()
        .thenAwait(Duration.ofSeconds(15))
        .expectError(TimeoutException.class)
        .verify();
}
```

**WebTestClient 테스트 패턴**:

```java
@WebFluxTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentWithIdempotencyKey() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        CreatePaymentRequest request = new CreatePaymentRequest(
            "order-123", Money.of(10000, "KRW"), PaymentMethod.CARD
        );
        Payment expected = Payment.builder()
            .id("pay-456")
            .status(PaymentStatus.APPROVED)
            .build();

        when(paymentService.create(any(), eq(idempotencyKey)))
            .thenReturn(Mono.just(expected));

        // When & Then
        webTestClient.post()
            .uri("/api/v1/payments")
            .header("X-Tenant-Id", "service-a")
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.paymentId").isEqualTo("pay-456")
            .jsonPath("$.data.status").isEqualTo("APPROVED");
    }

    @Test
    void shouldReturn409OnIdempotencyConflict() {
        // Given
        String idempotencyKey = "duplicate-key";
        when(paymentService.create(any(), eq(idempotencyKey)))
            .thenReturn(Mono.error(new IdempotencyConflictException("Payload mismatch")));

        // When & Then
        webTestClient.post()
            .uri("/api/v1/payments")
            .header("X-Tenant-Id", "service-a")
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPaymentRequest())
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error.code").isEqualTo("VAL_002");
    }
}
```

**Testcontainers 설정 (통합 테스트)**:

```java
@SpringBootTest
@Testcontainers
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("fluxpay_test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" +
            postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

**부하 테스트 (Load Testing)**:

| 시나리오 | 목표 | 도구 |
|----------|------|------|
| 정상 부하 | 1,000 TPS 처리 확인 | k6, Gatling |
| 피크 부하 | 3,000 TPS에서 graceful degradation | k6 |
| 스파이크 | 0 → 5,000 TPS 급증 대응 | k6 |
| Soak Test | 24시간 지속 부하에서 메모리 누수 없음 | k6 |

**카오스 엔지니어링 (Chaos Engineering)**:

| 실험 | 기대 결과 | 도구 |
|------|----------|------|
| PG 타임아웃 | Circuit breaker OPEN, 빠른 실패 | Chaos Mesh |
| Redis 장애 | DB fallback으로 멱등성 유지 | Chaos Mesh |
| Kafka 파티션 장애 | Outbox 재발행으로 이벤트 무손실 | Chaos Mesh |
| Pod 강제 종료 | 진행 중 Saga 보상 트랜잭션 완료 | kubectl delete |

**재해 복구 (DR) 테스트**:

| 항목 | 주기 | 목표 |
|------|------|------|
| DB Failover | 분기 | RTO < 5분 |
| 전체 복구 | 반기 | RTO < 15분, RPO < 1분 |
| Backup 복원 | 월간 | 백업 무결성 검증 |

### F. Monitoring Checklist

**Phase 1-3: 기본 모니터링 (필수)**:

- [ ] Spring Boot Actuator 활성화 (`/actuator/prometheus`)
- [ ] Micrometer 비즈니스 메트릭 구현
- [ ] Prometheus 스크래핑 설정 (Actuator + Kafka Exporter)
- [ ] Grafana 대시보드 구성 (4종: Executive, Operations, Engineering, SLA)
- [ ] Alertmanager 알림 규칙 정의 + PagerDuty 연동
- [ ] Loki 로그 수집 파이프라인 구성
- [ ] OpenTelemetry + Jaeger 분산 트레이싱 연동
- [ ] SLA 리포트 자동화

**Phase 4+: 선택적 모니터링 강화 (Optional)**:

- [ ] rJMX-Exporter 사이드카 배포 (프로세스 격리 필요 시)
- [ ] Jolokia Agent 설정 (rJMX-Exporter 연동 시 필요)
- [ ] 커스텀 JMX MBean 메트릭 수집 (필요 시)

---

*Document Version: 1.5*
*Last Updated: 2026-02-04*
