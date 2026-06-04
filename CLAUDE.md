# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Building

```bash
./gradlew build                          # Build all modules
./gradlew :<service-name>:build          # Build a specific service
./gradlew :<service-name>:bootJar        # Create executable jar
```

### Testing

```bash
./gradlew :<service-name>:test                                   # All tests in a service
./gradlew :<service-name>:test --tests "ClassName*"              # Single test class
```

### Running the Full Stack

```bash
cp .env.example .env                     # First-time setup — fill in credentials
docker compose up --build                # Full stack from source
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d  # Dev mode (local jars)
docker compose -f docker-compose.yml -f docker-compose.observability.yml up  # With Grafana/Prometheus
```

Access points once running:
- Gateway / Swagger UI: http://localhost:8080 / http://localhost:8080/swagger-ui.html
- Keycloak admin console: http://localhost:8081 (admin/admin)
- Eureka dashboard: http://localhost:8761
- Grafana: http://localhost:3000 (observability overlay only)

### Frontend

```bash
cd frontend
npm install
npm run dev      # Dev server
npm run build    # Production build
npm run lint     # ESLint check
```

## Architecture Overview

### Three-Plane Design

The system is a family fintech application ("Maqsat / Halyk Oiy") built around three planes:

1. **Transactional plane** — microservices that simulate a banking backend (transactions, budget, goals, family limits).
2. **Analytics & AI plane** — event-driven pipelines that compute user metrics, detect drift, generate multilingual summaries, and trigger LLM-based budget re-planning.
3. **Ecosystem plane** — partner commerce integration (Alser electronics, Halyk Travel mock services + recommendation engine).

### Service Catalog

| Service | Port | Purpose |
|---|---|---|
| `gateway` | 8080 | JWT validation + routing to all services via Eureka |
| `eureka-server` | 8761 | Service discovery |
| `auth-service` | 8089 | Onboarding & family invite flow (provisions Keycloak accounts) |
| `transaction-service` | 8083 | Transaction recording + MCC-based categorization; Kafka producer |
| `budget-service` | 8082 | Budget plans, category limits, spend tracking; Kafka consumer |
| `goals-service` | 8084 | Goals & virtual account balances |
| `family-service` | 8085 | Groups, adult/child roles, daily child limits, SOS approval flow |
| `notification-service` | 8087 | Push notifications and SOS alerts via Kafka |
| `analytics-service` | 8090 | Per-user metric computation + drift detection (scheduled) |
| `ai-assistant-service` | 8086 | LLM orchestration proxy → parse-budget-plan-service |
| `parse-budget-plan-service` | 8094 | Budget plan validation & persistence |
| `financial-agent-service` | 8091 | OpenAI-powered budget re-planning |
| `summary-llm-service` | 8092 | Multilingual spending summaries (OpenAI) |
| `recommendation-service` | 8093 | Partner targeting signals (OpenAI) |
| `integration-service` | 8088 | Partner offers/bonuses stub |
| `alser-mock-service` | 8095 | Electronics partner catalogue mock |
| `halyk-travel-mock-service` | 8096 | Travel partner catalogue mock |
| `frontend` | 8090/8080 | React 19 SPA (proxies `/api` to gateway) |

> `api-gateway/` at root is legacy — use `gateway/` instead.

### Key Architectural Patterns

**Virtual account overlay** — Money stays on the main bank account. Family and goal "accounts" are virtual masks computed through the API layer; no actual fund transfers occur.

**Event-driven fan-out + synchronous reads** — Kafka drives real-time side-effects (categorization, limit checks, analytics, notifications). REST via Eureka load balancing handles reads and synchronous commands.

**Database-per-service** — Single PostgreSQL 16 instance with one database per service. Flyway manages schema migrations (`baseline-on-migrate = true`). No cross-service direct DB access.

**JWT at the gateway only** — The `gateway` service validates OAuth2/JWT tokens from Keycloak. Downstream services trust forwarded headers; they do not re-validate tokens.

**Family roles are local** — `adult`/`child` roles live in `family-service` membership records, not in Keycloak. Keycloak only handles authentication ("who are you").

### Technology Stack

- **Java 21**, Spring Boot 3.4.1, Spring Cloud 2024.0.0
- **Spring Cloud Gateway** (reactive/WebFlux) for routing
- **Spring Data JPA + Hibernate** for persistence
- **Apache Kafka 3.9** (KRaft, no ZooKeeper) — topics: `transaction.categorized`, `transaction.limit-exceeded`, `family.limit-override-approved`, analytics/AI events
- **Keycloak 26** (OIDC) — realm config auto-imported at startup
- **OpenAI API** (`gpt-4o-mini`) and **Anthropic API** for AI services
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS 4, TanStack Query 5, Keycloak.js, Radix UI
- **Observability**: Micrometer + OTel tracing (→ Tempo) + Prometheus + Grafana + Loki

### Gradle Module Layout

All services are Gradle subprojects. Shared dependencies (Micrometer, Lombok, OpenTelemetry) are declared in the root `build.gradle.kts` via Spring BOM. Version catalog lives in `gradle/libs.versions.toml`.

### Environment Variables

Copy `.env.example` to `.env` and set at minimum:
- `DATABASE_USERNAME` / `DATABASE_PASSWORD`
- `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`
- `OPENAI_API_KEY` (for financial-agent, summary-llm, recommendation services)
- `ANTHROPIC_API_KEY` (for ai-assistant-service)

### Observability

Every service exposes `/actuator/health`, `/actuator/info`, and `/actuator/prometheus`. Traces are correlated across services via `traceId` in ECS-format JSON logs (Alloy → Loki pipeline). The Grafana dashboards are stored in `observability/` and auto-provisioned under the "Maqsat" folder.