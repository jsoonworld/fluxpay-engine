# Payment Engine 1-Pager

## 프로젝트 정보

| 항목 | 값 |
|------|-----|
| 프로젝트명 | FluxPay |
| GitHub Repository | fluxpay-engine |
| 현재 적용 서비스 | 모아 (피벗 예정) |
| 목적 | 도메인 독립적인 결제/과금 엔진 |
| 포지셔닝 | 분산 환경에서 최종 일관성을 보장하는 플러그인형 결제 플랫폼 |

---

## 프로젝트 개요

어떤 서비스 앞에도 붙여서 결제, 과금, 트래픽 제어를 처리할 수 있는 재사용 가능한 엔진.
특정 도메인에 종속되지 않고, 설정 기반으로 다양한 비즈니스 모델(크레딧, 구독, 종량제)을 지원한다.

---

## 비즈니스 컨텍스트

### 해결하려는 문제

- 유료 기능 도입 시마다 결제 시스템을 새로 구축하는 비효율
- 분산 환경에서 결제-서비스 간 데이터 정합성 보장 어려움
- 트래픽 폭주(선착순 이벤트) 시 시스템 보호 메커니즘 부재

### 목표

- 플러그인처럼 기존 서비스에 연결 가능한 결제 엔진
- 결제 승인 후 30초 내 최종 일관성 보장 (Saga 보상 트랜잭션 포함)
- 트래픽 제어를 통한 하위 시스템 보호

---

## 기술 스택

| 레이어 | 기술 | 선택 이유 |
|--------|------|----------|
| Framework | Spring Boot 3 + WebFlux | 비동기/논블로킹, 생태계 |
| Language | Java 21 + Reactor | 안정성, 광범위한 생태계 |
| Database | PostgreSQL | 트랜잭션 안정성 |
| Message Queue | Kafka | 이벤트 전달, 내구성 |
| Cache | Redis | Lua Script 기반 원자적 연산 |
| PG 연동 | 토스페이먼츠 | 국내 표준, 문서화 우수 |
| Metrics | Micrometer + Prometheus | Spring 생태계 표준, 시계열 DB |
| JVM Metrics | [rJMX-Exporter](https://github.com/jsoonworld/rJMX-Exporter) | Rust 기반 경량 사이드카, Jolokia 연동 |
| Tracing | OpenTelemetry + Jaeger | 벤더 중립, CNCF 졸업 프로젝트 |
| Alerting | Alertmanager + PagerDuty | 알림 그룹핑/라우팅, 온콜 연동 |
| Log Aggregation | Loki | Grafana 스택 통합, 라벨 기반 쿼리 |
| Kafka Monitoring | Kafka Exporter | Consumer Lag/DLQ 모니터링 |
| Dashboard | Grafana | 유연한 시각화, 알림 통합 |

---

## 시스템 아키텍처

```
[Client App]
    │ REST API
    ▼
┌─────────────────────────────────────────────────────────────┐
│                      FluxPay Engine                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Gateway   │  │   Payment   │  │   Infrastructure    │  │
│  │ (대기열/제어)│  │  (결제/과금) │  │                     │  │
│  └─────────────┘  └─────────────┘  │  ┌───────┐ ┌─────┐  │  │
│                                     │  │ Redis │ │ DB  │  │  │
│  ┌─────────────┐  ┌─────────────┐  │  │(Cache)│ │(PG) │  │  │
│  │   Outbox    │  │    Kafka    │  │  └───────┘ └─────┘  │  │
│  │  (이벤트)   │◀─│  Publisher  │  └─────────────────────┘  │
│  └─────────────┘  └─────────────┘                           │
└────────────────────────┬────────────────────────────────────┘
                         │ Kafka Event
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
   [Service A]     [Service B]     [Service N]
   (any backend)   (any backend)   (any backend)
```

### 연동 방식

- **Inbound**: REST API (결제 요청, 잔액 조회, 주문 생성)
- **Outbound**: Kafka 이벤트 (결제 완료, 크레딧 차감 알림)
- **Client → FluxPay**: REST API 호출 (이벤트 발행 아님)
- **설정**: YAML 기반 서비스별 과금 정책 정의

### 핵심 연동 플로우 (결제 승인 예시)

```
1. Client → FluxPay: POST /api/v1/orders (REST)
2. FluxPay: Order 생성 + Outbox 저장 (단일 트랜잭션)
3. FluxPay → PG: 결제 승인 요청
4. FluxPay: Payment 상태 갱신 + Outbox 저장
5. Outbox Publisher → Kafka: payment.approved 발행
6. Kafka → Client: 이벤트 수신 및 후속 처리
```

---

## 핵심 도메인

### 데이터 모델 요약

| 도메인 | 핵심 필드 | 상태 전이 |
|--------|----------|----------|
| Order | orderId, userId, amount, metadata | PENDING → PAID → COMPLETED / CANCELLED |
| Payment | paymentId, orderId, method, pgTxId | READY → APPROVED → CONFIRMED / FAILED |
| Credit | userId, balance, reservedAmount | 충전 / 차감 / 환불 (잔액 기반) |
| Subscription | userId, planId, period, nextBillingAt | ACTIVE → EXPIRED / CANCELLED |

### 1. 주문 (Order)

- 유료 기능 사용 요청 → 주문 생성
- 상태: `PENDING` → `PAID` → `COMPLETED` / `CANCELLED`
- 서비스 독립적: 주문 메타데이터(JSON)로 도메인 정보 전달
- 멱등 키: `idempotencyKey` (클라이언트 제공, 24시간 유효)

### 2. 결제 (Payment)

- PG사 연동 (토스페이먼츠, 확장 가능)
- 결제 수단: 카드, 간편결제
- 상태: `READY` → `APPROVED` → `CONFIRMED` / `FAILED`
- PG 응답 실패 시 자동 재시도 (최대 3회, exponential backoff)

### 3. 크레딧 (Credit)

- 선불 충전 방식 (포인트, 토큰)
- 사용량 차감 및 환불 처리
- 서비스별 차등 과금 정책 지원
- 동시성 제어: DB 낙관적 락 + Redis 예약 차감

### 4. 구독 (Subscription)

- 월정액/연정액 구독 모델
- 자동 갱신, 해지, 다운그레이드
- 플랜별 기능 제한 설정
- 상태: `ACTIVE` → `EXPIRED` / `CANCELLED`

---

## 기술적 도전 과제

### 1. Saga 패턴 (Orchestration)

**문제**: 서비스 호출 성공 → 결제 실패 시 정합성 불일치

**해결**: 보상 트랜잭션으로 자동 롤백

```
[정상 플로우]
주문생성 → 결제승인 → 서비스실행 → 크레딧차감 → 완료

[실패 시 보상]
서비스실패 → 결제취소 → 주문취소 → 크레딧복구
```

### 2. Transactional Outbox

**문제**: DB 커밋 성공 + Kafka 발행 실패 → 이벤트 유실

**해결**: Outbox 테이블에 이벤트 저장 → Polling Publisher가 Kafka로 발행

### 3. 멱등성 보장

**문제**: 네트워크 재시도로 중복 결제 발생

**해결**: 2단계 멱등성 보장

```
[1차 방어] Redis Lua Script - 요청 ID 기반 원자적 중복 체크 (TTL: 24h)
[2차 방어] DB Unique Constraint - idempotency_key 컬럼 (Redis 장애 시 fallback)
```

- Redis 장애/네트워크 분할 시에도 DB 레벨에서 중복 방지
- 멱등 키 충돌 시 기존 응답 캐시 반환

### 4. 트래픽 제어 (Virtual Waiting Room)

**문제**: 선착순 이벤트, 대규모 프로모션 시 서버 과부하

**해결**: Redis Sorted Set 기반 대기열 + 동적 입장 속도 조절

---

## API 설계 원칙

### 요청/응답 형식

- **Request**: camelCase JSON
- **Response**: 표준 응답 래퍼 (`isSuccess`, `code`, `message`, `result`)

### 에러 코드 체계

| 코드 | 설명 |
|------|------|
| PAY_001 | 잔액 부족 |
| PAY_002 | 결제 승인 실패 |
| PAY_003 | 이미 처리된 요청 |
| PAY_004 | 주문 상태 불일치 |

### 주요 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/orders` | 주문 생성 |
| POST | `/api/v1/payments/confirm` | 결제 승인 |
| GET | `/api/v1/credits/balance` | 잔액 조회 |
| POST | `/api/v1/credits/use` | 크레딧 사용 |

---

## 클라이언트 서비스 연동

### 연동 방식 요약

| 방향 | 프로토콜 | 용도 |
|------|----------|------|
| Client → FluxPay | REST API | 주문/결제/크레딧 요청 |
| FluxPay → Client | Kafka Event | 상태 변경 알림 |

### REST API (Client → FluxPay)

| 엔드포인트 | 용도 |
|------------|------|
| `POST /api/v1/orders` | 주문 생성 |
| `POST /api/v1/payments/confirm` | 결제 승인 |
| `POST /api/v1/credits/use` | 크레딧 사용 |
| `POST /api/v1/service-callbacks/completed` | 서비스 실행 완료 콜백 |
| `POST /api/v1/service-callbacks/failed` | 서비스 실행 실패 콜백 |

### Kafka 이벤트 (FluxPay → Client)

| 이벤트 | 설명 |
|--------|------|
| `payment.approved` | 결제 승인됨 |
| `payment.failed` | 결제 실패 |
| `credit.deducted` | 크레딧 차감됨 |
| `credit.refunded` | 크레딧 환불됨 |
| `subscription.activated` | 구독 활성화 |
| `subscription.expired` | 구독 만료 |

### 이벤트 계약

| 항목 | 정의 |
|------|------|
| 스키마 버전 | CloudEvents v1.0 + 자체 확장 |
| 파티션 키 | `userId` (사용자별 순서 보장) |
| 멱등 키 | `eventId` (UUID, 중복 수신 허용) |
| 재시도 정책 | Consumer 측 최소 1회 보장, 멱등 처리 필수 |
| DLQ | `fluxpay.dlq.{event-type}` (3회 실패 시 이동) |

### SDK 제공 (예정)

- Java/Kotlin SDK
- Node.js SDK
- Rust SDK

---

## 구현 우선순위

### Phase 1: Core Payment

- [x] 프로젝트 셋업 (Spring Boot 3, Java, WebFlux)
- [ ] 주문/결제 도메인 구현
- [ ] 토스페이먼츠 연동
- [ ] 기본 크레딧 시스템
- [ ] REST API 및 Swagger 문서화

### Phase 2: Event-Driven Architecture

- [ ] Kafka 연동 및 이벤트 스키마 정의
- [ ] Transactional Outbox 패턴 구현
- [ ] Saga Orchestrator 구현
- [ ] 클라이언트 서비스 연동 테스트

### Phase 3: Traffic Control & Subscription

- [ ] Redis 대기열 시스템 (Virtual Waiting Room)
- [ ] Rate Limiting 미들웨어
- [ ] 구독 도메인 구현
- [ ] 부하 테스트 (k6/Locust)

### Phase 4: Monitoring & Observability

- [ ] Micrometer 비즈니스 메트릭 구현
- [ ] Jolokia Agent 설정 (JVM 내 HTTP/JSON 엔드포인트)
- [ ] [rJMX-Exporter](https://github.com/jsoonworld/rJMX-Exporter) 사이드카 배포 (JVM 메트릭 → Prometheus)
- [ ] Prometheus 스크래핑 설정 (Actuator + rJMX-Exporter + Kafka Exporter)
- [ ] Grafana 대시보드 구성 (4종)
- [ ] Alertmanager 알림 규칙 정의 + PagerDuty 연동
- [ ] Loki 로그 수집 파이프라인 구성
- [ ] OpenTelemetry + Jaeger 분산 트레이싱 연동
- [ ] SLA 리포트 자동화

---

## 성공 기준

| 카테고리 | 지표 | 목표 | 알림 임계값 |
|----------|------|------|-------------|
| 정확성 | 결제 성공률 | 99.9% 이상 | < 99.9% (P2), < 99% (P1) |
| 정확성 | 중복 결제 | 0건 | > 0건 (P1) |
| 정합성 | 데이터 일관성 | 30초 내 최종 일관성 | Outbox 적체 > 100 (P3) |
| 복원력 | Saga 보상 발생 | 최소화 | > 10건/분 (P2) |
| 처리량 | 동시 처리 | 1,000 TPS 이상 | - |
| 지연 | API 응답 시간 | p95 < 200ms, p99 < 500ms | p99 > 500ms (P3) |

---

## 보안 설계

| 항목 | 정책 |
|------|------|
| 카드 정보 저장 | 저장 안 함 (PG 토큰화 사용) |
| 민감 데이터 암호화 | AES-256-GCM (전화번호, 이메일 등) |
| 암호화 키 관리 | AWS KMS 또는 Vault 연동 |
| 접근 통제 | API Key + HMAC 서명 검증 |
| 관리 엔드포인트 | /actuator/*, Jolokia는 내부망 전용 (NetworkPolicy, 127.0.0.1 바인딩) |
| 감사 로그 | 모든 결제/환불 요청 기록 (7년 보관) |
| 컴플라이언스 | PCI-DSS SAQ-A 준수 (카드 정보 미저장) |

---

## 운영 및 관측성

| 항목 | 구현 방식 |
|------|----------|
| 추적 ID | X-Request-Id 헤더, 모든 로그/이벤트에 전파 |
| 분산 트레이싱 | OpenTelemetry + Jaeger |
| 메트릭 | Micrometer + Prometheus + rJMX-Exporter |
| 로그 수집 | Loki (Grafana 스택 통합) |
| 알림 | 결제 성공률 < 99.9% 시 PagerDuty 연동 |
| DLQ 모니터링 | DLQ 메시지 1건 이상 시 알림 |
| 리플레이 정책 | Outbox 이벤트 수동 재발행 API 제공 |

---

## 모니터링 아키텍처

### 전체 구성

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
│              ┌──────────────┼──────────────┐                           │
│              ▼              ▼              ▼                           │
│      ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                  │
│      │ /actuator/  │ │ rJMX-Exporter│ │   Kafka    │                  │
│      │ prometheus  │ │  :9090      │ │  Exporter  │                  │
│      │ (Micrometer)│ │  (JVM 메트릭)│ │            │                  │
│      └──────┬──────┘ └──────┬──────┘ └─────────────┘                  │
│             │               │ Jolokia (HTTP/JSON)                      │
│             └───────────────┼──────────────────────                    │
│                             ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │                     FluxPay Engine (JVM)                          │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │ │
│  │  │ Jolokia  │  │Micrometer│  │ OpenTele │  │  Structured Log  │  │ │
│  │  │ Agent    │  │ Registry │  │ metry    │  │  (JSON → Loki)   │  │ │
│  │  └──────────┘  └──────────┘  └─────┬────┘  └──────────────────┘  │ │
│  └────────────────────────────────────┼─────────────────────────────┘ │
│                                       ▼                               │
│                              ┌──────────────┐                         │
│                              │    Jaeger    │                         │
│                              │  (Tracing)   │                         │
│                              └──────────────┘                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### rJMX-Exporter 선택 이유

> [rJMX-Exporter](https://github.com/jsoonworld/rJMX-Exporter): Rust 기반 경량 JMX 메트릭 수집기. Jolokia HTTP/JSON 프로토콜을 통해 JVM 메트릭을 수집하여 Prometheus 형식으로 노출.

| 항목 | 기존 jmx_exporter (Java Agent) | rJMX-Exporter (Rust Sidecar) |
|------|-------------------------------|------------------------------|
| 메모리 사용량 | ~50MB+ | **<10MB** |
| 시작 시간 | 2-5초 | **<100ms** |
| 런타임 의존성 | JVM 힙 공유 | **네이티브 바이너리** |
| 애플리케이션 영향 | 있음 (GC, 쓰레드 공유) | **없음 (프로세스 격리)** |
| JMX 접근 방식 | 직접 MBean 접근 | Jolokia HTTP/JSON |

- **경량화**: 사이드카 패턴으로 배포 시 리소스 효율 극대화
- **격리**: 모니터링 장애가 결제 시스템에 영향을 주지 않음
- **빠른 복구**: 100ms 이내 재시작으로 메트릭 수집 갭 최소화

> **사전 요구사항**: 애플리케이션에 Jolokia Agent가 설치되어 있어야 합니다.

### 메트릭 수집 전략

```
[Micrometer]                    [rJMX-Exporter]
비즈니스 메트릭                  JVM 인프라 메트릭
├─ 결제 성공/실패 수              ├─ Heap/Non-Heap 메모리
├─ 주문 처리량 (TPS)             ├─ GC 횟수/시간
├─ 크레딧 차감/환불              ├─ Thread 상태
├─ Saga 보상 트랜잭션            ├─ Class Loading
├─ 대기열 크기                   └─ JIT 컴파일
└─ API 응답 시간
```

---

## 핵심 모니터링 메트릭

### 비즈니스 메트릭 (Micrometer)

| 메트릭명 | 타입 | 설명 | 알림 임계값 |
|----------|------|------|-------------|
| `fluxpay_payment_total` | Counter | 결제 시도 수 (status 라벨) | - |
| `fluxpay_payment_success_rate` | Gauge | 결제 성공률 | < 99.9% (P2), < 99% (P1) |
| `fluxpay_payment_latency_seconds` | Histogram | 결제 처리 시간 | p99 > 500ms |
| `fluxpay_order_created_total` | Counter | 주문 생성 수 | - |
| `fluxpay_credit_balance_total` | Gauge | 전체 크레딧 잔액 합계 | - |
| `fluxpay_credit_transactions_total` | Counter | 크레딧 거래 수 (type 라벨: charge/use/refund) | - |
| `fluxpay_saga_compensation_total` | Counter | Saga 보상 트랜잭션 수 | > 10건/min |
| `fluxpay_outbox_pending_count` | Gauge | 미발행 Outbox 이벤트 | > 100 |
| `fluxpay_waiting_room_queue_size` | Gauge | 대기열 크기 | > 10,000 |
| `fluxpay_idempotent_hit_total` | Counter | 멱등 키 캐시 히트 | - |

### JVM 메트릭 (rJMX-Exporter)

| 메트릭명 | 설명 | 알림 임계값 |
|----------|------|-------------|
| `jvm_memory_heap_used_bytes` | 힙 메모리 사용량 | > 80% of max |
| `jvm_memory_heap_max_bytes` | 힙 메모리 최대값 | - |
| `jvm_gc_collection_seconds_count` | GC 횟수 | - |
| `jvm_gc_collection_seconds_sum` | GC 총 소요 시간 | > 5s/min |
| `jvm_threads_current` | 현재 쓰레드 수 | > 500 |
| `jvm_threads_deadlocked` | 데드락 쓰레드 | > 0 |

### 인프라 메트릭

| 대상 | 메트릭 | 알림 임계값 |
|------|--------|-------------|
| Kafka | Consumer Lag | > 10,000 |
| Kafka | DLQ 메시지 수 | > 0 |
| Redis | 메모리 사용률 | > 80% |
| Redis | 연결 수 | > 1,000 |
| PostgreSQL | Active Connections | > 80% of max |
| PostgreSQL | Replication Lag | > 1s |

---

## 알림 전략

### 심각도 분류

| 레벨 | 응답 시간 | 예시 |
|------|----------|------|
| P1 (Critical) | 5분 이내 | 결제 전면 장애, 결제 성공률 99% 미만, 데이터 유실 위험 |
| P2 (High) | 30분 이내 | 결제 성공률 99.9% 미만, Saga 보상 급증 (10건/분 초과) |
| P3 (Medium) | 4시간 이내 | 응답 지연 (p99 > 500ms), 메모리 경고 |
| P4 (Low) | 다음 업무일 | 경미한 성능 저하 |

### 핵심 알림 규칙

```yaml
# P1: 결제 시스템 다운
- alert: PaymentSystemDown
  expr: up{job="fluxpay"} == 0
  for: 1m
  labels:
    severity: critical

# P2: 결제 성공률 저하
- alert: PaymentSuccessRateLow
  expr: fluxpay_payment_success_rate < 0.999
  for: 5m
  labels:
    severity: high

# P1: 결제 성공률 급락
- alert: PaymentSuccessRateCritical
  expr: fluxpay_payment_success_rate < 0.99
  for: 2m
  labels:
    severity: critical

# P2: Saga 보상 급증 (10건/분 초과)
- alert: SagaCompensationSpike
  expr: increase(fluxpay_saga_compensation_total[1m]) > 10
  for: 2m
  labels:
    severity: high

# P3: Outbox 적체
- alert: OutboxBacklog
  expr: fluxpay_outbox_pending_count > 100
  for: 10m
  labels:
    severity: medium

# P3: JVM 힙 메모리 경고
- alert: JvmHeapMemoryHigh
  expr: jvm_memory_heap_used_bytes / jvm_memory_heap_max_bytes > 0.8
  for: 5m
  labels:
    severity: medium
```

---

## 대시보드 구성

### 1. Executive Dashboard (경영진용)

- 일별 결제 처리량 및 금액
- 결제 성공률 추이
- 주요 장애 타임라인

### 2. Operations Dashboard (운영팀용)

- 실시간 TPS 및 응답 시간
- 에러율 및 에러 유형 분포
- 대기열 상태 (Virtual Waiting Room)
- Kafka Consumer Lag
- DLQ 메시지 현황

### 3. Engineering Dashboard (개발팀용)

- JVM 힙/GC 상태 (rJMX-Exporter)
- 쓰레드 풀 상태 (WebFlux Event Loop)
- DB 커넥션 풀 상태
- Saga 상태 전이 흐름
- Outbox 발행 지연

### 4. SLA Dashboard

- 월간 가용성 (목표: 99.9%)
- API 응답 시간 SLO 달성률
- 중복 결제 발생 건수 (목표: 0건)

---

## 제약 조건

- 클라이언트 서비스 코드 수정 최소화 (REST + 이벤트 기반 연동)
- Kafka를 통한 느슨한 결합 유지
- 카드 정보 직접 저장 금지 (PG 토큰화 필수)
- 도메인 로직은 FluxPay 내부에 캡슐화

---

## 기술 키워드

`Spring WebFlux` `Project Reactor` `Kafka` `Redis Lua Script`
`Saga Pattern` `Transactional Outbox` `Idempotent API`
`Virtual Waiting Room` `Rate Limiting` `Event-Driven Architecture`
`Prometheus` `Grafana` `rJMX-Exporter` `Jolokia` `Micrometer`
`OpenTelemetry` `Jaeger` `Loki` `Alertmanager` `SLA Monitoring`
