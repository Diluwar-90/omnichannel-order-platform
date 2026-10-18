# Omnichannel Order Processing Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Microservices-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-4.1.0%20(KRaft)-black.svg?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?logo=docker&logoColor=white)](https://www.docker.com/)
[![AWS ECS Fargate](https://img.shields.io/badge/AWS-ECS%20Fargate-FF9900.svg?logo=amazonaws&logoColor=white)](docs/AWS_COPILOT_ECS_DEPLOYMENT.md)
[![AWS Copilot](https://img.shields.io/badge/AWS-Copilot%20CLI-blueviolet.svg?logo=amazonaws&logoColor=white)](docs/AWS_COPILOT_ECS_DEPLOYMENT.md)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF.svg?logo=githubactions&logoColor=white)](.github/workflows/ci-cd.yml)
[![AWS OIDC](https://img.shields.io/badge/AWS-ECR%20%7C%20OIDC-FF9900.svg?logo=amazonaws&logoColor=white)](docs/AWS_CICD_SETUP.md)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

An enterprise-grade, distributed **Omnichannel Order Processing Platform** built with **Java 21**, **Spring Boot**, **Spring Cloud Gateway**, and **Apache Kafka**. Designed for high availability, fault tolerance, and eventual consistency across physical retail and digital storefront channels.

---

## Table of Contents

- [Architectural Highlights](#architectural-highlights)
- [System Architecture](#system-architecture)
- [Microservice Ecosystem](#microservice-ecosystem)
- [Event-Driven Saga Pattern (Order Lifecycle)](#event-driven-saga-pattern-order-lifecycle)
- [Fault Tolerance & Resilience (Resilience4j & Idempotency)](#fault-tolerance--resilience-resilience4j--idempotency)
- [API Gateway Routing & Endpoints](#api-gateway-routing--endpoints)
- [Security & RBAC](#security--rbac)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Starting Infrastructure with Docker Compose](#starting-infrastructure-with-docker-compose)
  - [Running Microservices Locally](#running-microservices-locally)
- [Testing](#testing)
- [CI/CD & Cloud Deployment](#cicd--cloud-deployment)
- [Repository Structure](#repository-structure)
- [Observability & Health Checks](#observability--health-checks)
- [License](#license)

---

## Architectural Highlights

- **Microservices Architecture**: Decoupled domain boundaries with independent build lifecycles and dedicated persistence layers (Database-per-Service pattern).
- **Choreographed Event-Driven Saga**: Asynchronous distributed transaction management using **Apache Kafka** to maintain eventual consistency without distributed locking bottlenecks.
- **Unified Edge Routing**: **Spring Cloud Gateway (WebFlux)** serving as the non-blocking API Gateway reverse proxy for unified traffic routing, CORS policies, and rate-limiting readiness.
- **Enterprise Security**: Zero-trust authentication via stateless **JWT tokens**, BCrypt password hashing, and granular **Role-Based Access Control (RBAC)**.
- **Lean, Hardened Production Containers**: Built on `eclipse-temurin:21-jre-alpine` (~95MB image footprint), running with least-privilege `appuser` non-root security and JVM container ergonomics (`-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`) to eliminate container OOM kills.
- **Reliable Data Migrations**: Automated, version-controlled relational schema migrations using **Flyway**.
- **Hermetic Integration Testing**: 100% self-contained test suites powered by **Spring Boot Testcontainers** (ephemeral PostgreSQL 17 and Apache Kafka Native) requiring zero pre-running host dependencies in CI.
- **Secure Cloud Native CI/CD**: Automated GitHub Actions pipelines featuring parallel test matrices, automated dependency container builds, and **keyless AWS OIDC authentication** to Amazon ECR and Amazon ECS Fargate.

---

## System Architecture

```mermaid
flowchart TB
    Client[Web / Mobile / POS Clients] -->|HTTP / REST| Gateway[API Gateway :8080]

    subgraph Edge Layer
        Gateway
    end

    subgraph Security Layer
        Gateway -->|/api/v1/auth/**| Auth[Auth Service :8081]
        Auth --> AuthDB[(PostgreSQL: auth_db)]
    end

    subgraph Domain Microservices
        Gateway -->|/api/products/**| Product[Product Service :8082]
        Gateway -->|/api/v1/inventory/**| Inventory[Inventory Service :8083]
        Gateway -->|/api/orders/**| Order[Order Service :8084]
        Gateway -->|/api/v1/payments/**| Payment[Payment Service :8085]
        Gateway -->|/api/notifications/**| Notification[Notification Service :8086]

        Product --> ProductDB[(PostgreSQL: product_db)]
        Inventory --> InventoryDB[(PostgreSQL: inventory_db)]
        Order --> OrderDB[(PostgreSQL: order_db)]
        Payment --> PaymentDB[(PostgreSQL: payment_db)]
        Notification --> NotificationDB[(PostgreSQL: notification_db)]
    end

    subgraph Synchronous Inter-Service
        Order -.->|HTTP: Reserve Stock| Inventory
    end

    subgraph Event Backbone [Apache Kafka KRaft Cluster]
        Topic1([order.created])
        Topic2([payment.captured])
        Topic3([payment.failed])
        Topic4([payment.refunded])
    end

    Order -->|Publishes| Topic1
    Topic1 -->|Consumes| Payment
    Topic1 -->|Consumes| Notification

    Payment -->|Publishes| Topic2
    Payment -->|Publishes| Topic3
    Payment -->|Publishes| Topic4

    Topic2 -->|Consumes| Order
    Topic3 -->|Consumes| Order
    Topic2 -->|Consumes| Notification
    Topic3 -->|Consumes| Notification
    Topic4 -->|Consumes| Notification
```

---

## Microservice Ecosystem

| Service | Port | Database / Schema | Responsibilities |
| :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | *None* | Central reverse proxy, request routing, WebFlux non-blocking load handling. |
| **Auth Service** | `8081` | `auth_db` | User registration, authentication, JWT generation/validation, role authorization. |
| **Product Service** | `8082` | `product_db` (`product_schema`) | Product catalog lifecycle (CRUD), pricing, categories, and metadata. |
| **Inventory Service** | `8083` | `inventory_db` (`inventory_schema`) | Multi-channel stock management, stock reservations, releases, and stock audits. |
| **Order Service** | `8084` | `order_db` (`order_schema`) | Order placement, state machine transitions, stock reservation sync, and Saga coordination. |
| **Payment Service** | `8085` | `payment_db` (`payment_schema`) | Payment processing, transaction capture, refunds, and Kafka event publishing. |
| **Notification Service**| `8086` | `notification_db` (`notification_schema`)| Audit trails, event consumption, customer communications (email/SMS simulators). |

---

## Event-Driven Saga Pattern (Order Lifecycle)

The platform employs a **choreographed saga** pattern to fulfill orders reliably without 2-phase commits:

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant Gateway as API Gateway
    participant Order as Order Service
    participant Inventory as Inventory Service
    participant Kafka as Apache Kafka
    participant Payment as Payment Service
    participant Notify as Notification Service

    Customer->>Gateway: POST /api/orders
    Gateway->>Order: Forward Request
    Order->>Inventory: POST /api/v1/inventory/{id}/reserve (Sync)
    Inventory-->>Order: Stock Reserved (HTTP 200)
    Order->>Order: Save Order (Status: PENDING)
    Order->>Kafka: Publish "order.created"
    Order-->>Customer: Return OrderResponse (Status: PENDING)

    par Async Payment Processing
        Kafka->>Payment: Consume "order.created"
        Payment->>Payment: Process Payment Transaction
        alt Payment Succeeded
            Payment->>Kafka: Publish "payment.captured"
            Kafka->>Order: Consume "payment.captured"
            Order->>Order: Transition Order Status to CONFIRMED
        else Payment Failed
            Payment->>Kafka: Publish "payment.failed"
            Kafka->>Order: Consume "payment.failed"
            Order->>Order: Transition Order Status to CANCELLED
            Order->>Inventory: POST /api/v1/inventory/{id}/release (Compensate)
        end
    and Async Customer Notification
        Kafka->>Notify: Consume "order.created"
        Notify->>Notify: Log & Dispatch Order Creation Alert
        Kafka->>Notify: Consume "payment.captured" / "payment.failed"
        Notify->>Notify: Log & Dispatch Payment Receipt / Failure Alert
    end
```

---

## Fault Tolerance & Resilience (Resilience4j & Idempotency)

The platform implements industry-standard fault tolerance patterns powered by **Resilience4j**, client-side timeouts, and distributed idempotency guarantees:

```mermaid
flowchart TD
    OS[Order Service] -->|Idempotency-Key Header| R4J[Resilience4j Interceptor]
    
    subgraph R4J_Stack [Resilience4j Protection Stack]
        TL[TimeLimiter: 3s Timeout]
        RE[Retry: 3 Attempts with Exponential Backoff]
        CB[Circuit Breaker: Sliding Window & 50% Threshold]
    end
    
    R4J --> TL --> RE --> CB --> PS[Payment Service]
    
    subgraph Payment_Flow [Payment Service Idempotent Processing]
        PS --> ID_CHK{Check Idempotency}
        ID_CHK -->|Existing Payment| RET_EXIST[Return Cached Response]
        ID_CHK -->|New Payment| EXT_PROV[External Payment Provider]
        EXT_PROV -->|Success| SAVE_PAY[Save Payment: CAPTURED]
        EXT_PROV -->|Failure| FAIL_PAY[Save Payment: FAILED]
        SAVE_PAY --> RET_RES[Publish Event & Return Result]
        FAIL_PAY --> RET_RES
    end
```

### Core Resilience Mechanisms

1. **Circuit Breakers (`@CircuitBreaker`)**:
   - **Order Service**: Inter-service REST clients (`PaymentClient` and `InventoryClient`) are guarded by Resilience4j circuit breakers with sliding window evaluation (10 calls), 50% failure threshold, and 50% slow-call threshold (>2s). When open, requests automatically redirect to local fallback handlers, returning descriptive service degradation responses rather than tying up container threads.
   - **Payment Service**: Third-party payment provider integration (`PaymentProvider` / `DefaultPaymentProvider`) is protected by a dedicated circuit breaker (`paymentProvider`) to isolate external latency spikes and bank gateway outages.
   - **API Gateway**: Edge routing utilizes `spring-cloud-starter-circuitbreaker-reactor-resilience4j` with reactive timeout filters that immediately redirect failing route traffic to edge fallback endpoints.
   - **Self-Healing Dynamics**: Automatically transitions from `CLOSED` &rarr; `OPEN` &rarr; `HALF_OPEN` &rarr; `CLOSED` upon downstream recovery.

2. **Exponential Backoff Retries (`@Retry`)**:
   - Configured with 3 max attempts, 500ms initial wait duration, and a 2x backoff multiplier.
   - Selectively retries transient network interruptions (`ResourceAccessException`, `IOException`, `5xx` server errors) while bypassing client errors (`4xx`).

3. **Time Limiters & Socket Timeouts (`@TimeLimiter`)**:
   - HTTP clients leverage `JdkClientHttpRequestFactory` with strict socket-level timeouts (3-second connection timeout, 5-second read timeout), preventing hung connections from exhausting connection pools.

4. **Distributed Idempotency (`Idempotency-Key`)**:
   - **Header Propagation**: Callers and clients provide standard `Idempotency-Key` headers on mutations.
   - **Deduplication Logic**: `PaymentService` queries existing records by `idempotencyKey` and `orderId`. Duplicate submissions immediately return the existing processed payment entity without re-executing credit card captures or generating duplicate bank charges.
   - **Automated Client Propagation**: `order-service` automatically injects a deterministic `Idempotency-Key: order-<id>` or UUID header when executing synchronous payment calls.

5. **Edge Gateway Fallbacks**:
   - The API Gateway routes incoming traffic through reactive circuit breakers. In the event of downstream service degradation, callers receive clean, standardized JSON errors (`/fallback/orders`, `/fallback/payments`, `/fallback/inventory`, `/fallback/products`, `/fallback/auth`, `/fallback/notifications`) rather than raw connection timeouts.

---

## API Gateway Routing & Endpoints

All external traffic routes through the **API Gateway** on port `8080`.

### 1. Authentication (`/api/v1/auth/**` &rarr; `:8081`)

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","password":"SecurePassword123!"}'

# Authenticate & obtain JWT
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"SecurePassword123!"}'

# Verify authentication (Protected Profile)
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

### 2. Products (`/api/products/**` &rarr; `:8082`)

```bash
# Create Product (Requires stockQuantity)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Wireless Noise-Canceling Headphones","description":"Premium over-ear headphones","price":199.99,"stockQuantity":50}'

# List Products
curl -X GET http://localhost:8080/api/products

# Get Product By ID
curl -X GET http://localhost:8080/api/products/1
```

### 3. Inventory (`/api/v1/inventory/**` &rarr; `:8083`)

```bash
# Initialize inventory for product
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":100}'

# Check stock availability
curl -X GET "http://localhost:8080/api/v1/inventory/1/availability?quantity=2"

# Reserve stock
curl -X POST http://localhost:8080/api/v1/inventory/1/reserve \
  -H "Content-Type: application/json" \
  -d '{"quantity":2}'

# Release stock
curl -X POST http://localhost:8080/api/v1/inventory/1/release \
  -H "Content-Type: application/json" \
  -d '{"quantity":2}'
```

### 4. Orders (`/api/orders/**` &rarr; `:8084`)

```bash
# Place new order (triggers synchronous inventory reservation & Kafka Saga)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 101,
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "unitPrice": 199.99
      }
    ]
  }'

# Get order details
curl -X GET http://localhost:8080/api/orders/1

# Get order payment details (via Order Service Client)
curl -X GET http://localhost:8080/api/orders/1/payment
```

### 5. Payments (`/api/v1/payments/**` &rarr; `:8085`)

```bash
# Process direct payment with Idempotency Key
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d" \
  -d '{"orderId":1,"amount":399.98,"currency":"USD"}'

# Duplicate call with same Idempotency-Key returns cached payment without double charge:
# Status: 201 Created (Cached Entity with exact same ID and timestamp)

# Query payment by order ID
curl -X GET http://localhost:8080/api/v1/payments/order/1
```

### 6. Notifications (`/api/notifications/**` &rarr; `:8086`)

```bash
# View dispatched notifications audit log
curl -X GET http://localhost:8080/api/notifications
```

### 7. Edge Gateway Fallbacks & Circuit Breakers (`/fallback/**` &rarr; `:8080`)

When a backend microservice becomes unavailable or latency exceeds timeout thresholds, Spring Cloud Gateway routes gracefully degrade to fallback endpoints:

```bash
# Test fallback response directly or during downstream service outage:
curl -X GET http://localhost:8080/fallback/orders
curl -X GET http://localhost:8080/fallback/payments
curl -X GET http://localhost:8080/fallback/inventory
curl -X GET http://localhost:8080/fallback/products
curl -X GET http://localhost:8080/fallback/auth
curl -X GET http://localhost:8080/fallback/notifications

# Standardized HTTP 503 JSON Output:
# {
#   "error": "Service Unavailable",
#   "message": "Order Service is currently unavailable. Circuit breaker triggered.",
#   "status": 503,
#   "timestamp": "2026-10-18T18:50:00Z"
# }
```

---

## Security & RBAC

The platform implements **Spring Security 6** with **stateless JWT tokens**:
- **Token Generation**: Claims include user ID, username, and assigned roles (`ROLE_USER`, `ROLE_ADMIN`).
- **Signature & Expiration**: HMAC-SHA signed with configurable secret keys and automated token expiration.
- **Method-Level Security**: Controllers leverage `@PreAuthorize("hasRole('ADMIN')")` for sensitive operations.
- **Data Protection**: User passwords are encrypted using BCrypt before persisting to `auth_db`.

---

## Technology Stack

| Domain | Technologies |
| :--- | :--- |
| **Language & Runtimes** | Java 21 (OpenJDK / Eclipse Temurin Alpine JRE) |
| **Frameworks** | Spring Boot 4.x / 3.x, Spring Cloud Gateway (WebFlux), Spring Security, Spring Data JPA |
| **Resilience & Fault Tolerance** | Resilience4j 2.2.0 (Circuit Breakers, Retries, TimeLimiters), Spring Cloud CircuitBreaker (Reactor Resilience4j) |
| **Messaging & Streaming** | Apache Kafka 4.1.0 (KRaft mode, zero ZooKeeper dependency) |
| **Databases & Migrations**| PostgreSQL 17, Flyway Database Migrations |
| **Build & Tooling** | Gradle 8.x / 9.x Wrapper, Project Lombok, Jackson |
| **Testing & Quality** | JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL 17, Kafka Native) |
| **Containerization** | Docker (Alpine JRE, Non-root `appuser`, JVM Container Ergonomics), Docker Compose |
| **DevOps & Cloud** | GitHub Actions CI/CD, AWS OIDC (IAM AssumeRoleWithWebIdentity), Amazon ECR, Amazon ECS Fargate |

---

## Getting Started

### Prerequisites

Ensure you have installed:
- **Java 21 JDK** (`java -version`)
- **Docker & Docker Compose v2+** (`docker compose version`)
- **Git**

### Starting Infrastructure with Docker Compose

The platform provides a unified [`docker-compose.yml`](docker-compose.yml) orchestrating all 7 microservices, **PostgreSQL 17** (with pre-initialized schemas), and **Apache Kafka** (KRaft mode).

```bash
# Clone the repository
git clone https://github.com/Diluwar-90/omnichannel-order-platform.git
cd omnichannel-order-platform

# Option A: Start complete platform (All 7 microservices + PostgreSQL + Kafka)
docker compose up -d

# Option B: Start infrastructure only (PostgreSQL 17 and Apache Kafka KRaft)
docker compose up -d postgres kafka

# Verify container health
docker compose ps
```

PostgreSQL is exposed at `localhost:5433` (internal port `5432`):
- **User**: `app_user`
- **Password**: `app_password`
- **Databases**: `product_db`, `inventory_db`, `order_db`, `payment_db`, `notification_db`, `auth_db`

Apache Kafka is accessible at:
- **Internal (containers)**: `kafka:9092`
- **External (host)**: `localhost:9094`

### Running Microservices Locally

Each service can be compiled and run individually using its Gradle wrapper:

```bash
# In separate terminal windows:
./services/api-gateway/gradlew -p services/api-gateway bootRun
./services/auth-service/gradlew -p services/auth-service bootRun
./services/product-service/gradlew -p services/product-service bootRun
./services/inventory-service/gradlew -p services/inventory-service bootRun
./services/order-service/gradlew -p services/order-service bootRun
./services/payment-service/gradlew -p services/payment-service bootRun
./services/notification-service/gradlew -p services/notification-service bootRun
```

Or build container images and run via Docker Compose:

```bash
# Build all bootable JARs
for svc in api-gateway auth-service product-service inventory-service order-service payment-service notification-service; do
  ./services/$svc/gradlew -p services/$svc bootJar
done

# Run the complete environment
docker compose up -d --build
```

---

## Testing

The project incorporates comprehensive unit and integration testing designed for **100% hermetic isolation** using **Testcontainers**. Tests do not rely on pre-running host databases or Kafka brokers; ephemeral instances spin up dynamically during test execution.

- **Unit Tests**: Fast, isolated tests mocking downstream boundaries with Mockito.
- **Integration Tests**: Full Spring Boot test contexts with `@ServiceConnection` dynamically injecting connection parameters for PostgreSQL 17 and Apache Kafka Native.
- **Cross-Service Testing**: End-to-end integration tests (e.g. `OrderIntegrationTest`) validate distributed communication with containerized downstream dependencies (`inventory-service:test`, `payment-service:test`).

```bash
# Run tests across all microservices in parallel
for svc in api-gateway auth-service product-service inventory-service order-service payment-service notification-service; do
  echo "Testing $svc..."
  ./services/$svc/gradlew -p services/$svc test --no-daemon
done

# Run tests for a specific service (e.g. order-service)
./services/order-service/gradlew -p services/order-service test
```

Test reports are generated in:
```
services/<service-name>/build/reports/tests/test/index.html
```

---

## CI/CD & Cloud Deployment

Continuous Integration and Continuous Deployment are managed via **GitHub Actions** ([`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml)):

```
┌─────────────────┐       ┌────────────────────────┐       ┌────────────────────────┐       ┌─────────────────────────┐
│ Job 1: Test     │  ──▶  │ Job 2: Build & Package │  ──▶  │ Job 3: Amazon ECR Push │  ──▶  │ Job 4: Amazon ECS Deploy│
│ (7 Services in  │       │ (Compile JAR & Build   │       │ (Keyless AWS OIDC Auth │       │ (Zero-Downtime Rolling  │
│  Parallel)      │       │  Docker Containers)    │       │  -> Push ECR Images)   │       │  Fargate Deployment)    │
└─────────────────┘       └────────────────────────┘       └────────────────────────┘       └─────────────────────────┘
```

1. **Test Matrix**: Automatically triggers on pull requests and pushes to `main`, validating unit and integration suites in parallel.
2. **Build Matrix**: Compiles executable Spring Boot artifacts (`bootJar`) and builds Docker images tagged with Git commit SHAs and `latest`.
3. **AWS OIDC Deployment**: Authenticates seamlessly with AWS STS using OpenID Connect (**zero stored long-lived AWS keys**) and pushes images to Amazon ECR.
4. **Automated ECS Fargate Deployment**: Injects new container image digests into task definitions using `aws-actions/amazon-ecs-render-task-definition` and executes zero-downtime rolling service deployments with `aws-actions/amazon-ecs-deploy-task-definition`.

> - For initial environment and cluster provisioning (VPC, Aurora Serverless v2, MSK Serverless), see the [AWS Copilot & ECS Fargate Deployment Guide](docs/AWS_COPILOT_ECS_DEPLOYMENT.md).
> - For step-by-step instructions on configuring AWS IAM OIDC roles and ECR repositories, see the [AWS CI/CD Setup Guide](docs/AWS_CICD_SETUP.md).

---

## Repository Structure

```
omnichannel-order-platform/
├── .github/
│   └── workflows/
│       └── ci-cd.yml                 # GitHub Actions CI/CD with ECR & ECS rolling deploy
├── copilot/                          # AWS Copilot manifests (Cost-Optimized Dev)
│   ├── .workspace                    # Application configuration
│   ├── environments/dev/             # VPC (1 NAT GW), Aurora v2 & MSK Serverless addons
│   └── <service>/manifest.yml        # Service manifests (Load Balanced & Backend)
├── docker/
│   └── postgres/init/                # PostgreSQL local initialization scripts
├── docs/
│   ├── AWS_CICD_SETUP.md             # Keyless AWS OIDC & ECR deployment documentation
│   └── AWS_COPILOT_ECS_DEPLOYMENT.md # AWS Copilot & ECS Fargate deployment guide
├── infrastructure/
│   └── ecs/task-definitions/         # Fargate task definition JSON templates (7 services)
├── services/
│   ├── api-gateway/                  # Spring Cloud Gateway edge router (Port 8080)
│   ├── auth-service/                 # JWT Authentication & RBAC (Port 8081)
│   ├── product-service/              # Product catalog microservice (Port 8082)
│   ├── inventory-service/            # Stock reservation & tracking (Port 8083)
│   ├── order-service/                # Order management & Saga orchestrator (Port 8084)
│   ├── payment-service/              # Payment processing & Kafka producer (Port 8085)
│   └── notification-service/         # Async notification subscriber (Port 8086)
├── docker-compose.yml                # Unified multi-container local orchestration
└── README.md                         # Project documentation
```

---

## Observability & Health Checks

Each microservice exposes standard Spring Boot Actuator endpoints for container health probes, circuit breaker state tracking, and metrics collection:

### 1. Health Checks (Liveness / Readiness)

```bash
# Verify health across all services
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Product Service
curl http://localhost:8083/actuator/health  # Inventory Service
curl http://localhost:8084/actuator/health  # Order Service
curl http://localhost:8085/actuator/health  # Payment Service
curl http://localhost:8086/actuator/health  # Notification Service
```

### 2. Resilience4j Circuit Breakers & Retries Monitoring

Actuator exposes runtime circuit breaker and retry telemetry for real-time observability:

```bash
# Inspect Order Service Circuit Breakers (PaymentClient & InventoryClient)
curl http://localhost:8084/actuator/circuitbreakers

# Sample Output:
# {
#   "circuitBreakers": {
#     "inventoryService": {
#       "bufferedCalls": 1,
#       "failedCalls": 0,
#       "failureRate": "-1.0%",
#       "failureRateThreshold": "50.0%",
#       "notPermittedCalls": 0,
#       "state": "CLOSED"
#     },
#     "paymentService": {
#       "bufferedCalls": 2,
#       "failedCalls": 0,
#       "failureRate": "-1.0%",
#       "failureRateThreshold": "50.0%",
#       "notPermittedCalls": 0,
#       "state": "CLOSED"
#     }
#   }
# }

# Inspect Payment Service Circuit Breakers (PaymentProvider)
curl http://localhost:8085/actuator/circuitbreakers

# Inspect Configured Retries
curl http://localhost:8084/actuator/retries
# Output: {"retries":["inventoryService","paymentService"]}
```

### 3. API Gateway Route Inspection

```bash
# Query active edge routes, predicates, and filter definitions
curl http://localhost:8080/actuator/gateway/routes
```

---

## License

This project is licensed under the [MIT License](LICENSE).
