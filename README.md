# FluxPay Engine

> 도메인 독립적인 결제/과금 엔진 - 분산 환경에서 데이터 무결성을 보장하는 플러그인형 결제 플랫폼

## Overview

어떤 서비스 앞에도 붙여서 결제, 과금, 트래픽 제어를 처리할 수 있는 재사용 가능한 엔진입니다.
특정 도메인에 종속되지 않고, 설정 기반으로 다양한 비즈니스 모델(크레딧, 구독, 종량제)을 지원합니다.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3 + WebFlux |
| Language | Kotlin + Coroutines |
| Database | PostgreSQL (R2DBC) |
| Message Queue | Kafka |
| Cache | Redis |
| PG | 토스페이먼츠 |

## Quick Start

### Prerequisites

- JDK 21
- Docker & Docker Compose
- Gradle 8.x

### Run Infrastructure

```bash
docker-compose up -d
```

### Run Application

```bash
./gradlew bootRun
```

### API Documentation

서버 실행 후: http://localhost:8080/swagger-ui.html

## Project Structure

```
fluxpay-engine/
├── src/main/kotlin/com/fluxpay/engine/
│   ├── FluxPayApplication.kt
│   ├── domain/
│   │   ├── order/          # 주문 도메인
│   │   ├── payment/        # 결제 도메인
│   │   ├── credit/         # 크레딧 도메인
│   │   └── subscription/   # 구독 도메인
│   ├── infrastructure/
│   │   ├── config/         # 설정
│   │   ├── persistence/    # DB 연동
│   │   ├── messaging/      # Kafka
│   │   └── external/       # 외부 API (PG사)
│   └── presentation/
│       ├── api/            # REST API
│       └── dto/            # 요청/응답 DTO
├── src/main/resources/
│   └── application.yml
├── docs/
│   └── 1-pager.md          # 프로젝트 문서
└── docker-compose.yml
```

## Core Features

- **Order**: 주문 생성 및 상태 관리
- **Payment**: PG사 연동 결제 처리
- **Credit**: 선불 크레딧 충전/사용
- **Subscription**: 구독 모델 지원

## Key Technical Challenges

- **Saga Pattern**: 분산 트랜잭션 보상 처리
- **Transactional Outbox**: 이벤트 유실 방지
- **Idempotency**: 중복 요청 방지 (Redis Lua Script)
- **Virtual Waiting Room**: 트래픽 제어

## Documentation

- [1-Pager](docs/1-pager.md) - 프로젝트 상세 문서

## License

MIT License
