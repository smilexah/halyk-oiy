# Analytics & AI Plane Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the analytics & AI plane described in `Maqsat_Family_Technical_Context.md` §4: a new `analytics-service` (Spring + Python worker) that derives user metrics, detects budget drift, and triggers three GPU-stand-in AI services (`financial-agent`, `summary-llm`, `recommendation-engine`) that re-plan budgets, generate multilingual summaries, and produce ecosystem-targeting signals.

**Architecture:** Java Spring Boot microservice (`analytics-service`) owns the Metrics DB, REST API, and Kafka producer/consumer surface. Heavy aggregations (recurring-debit detection, volatility, savings rate) run in a Python FastAPI sidecar (`analytics-worker`) called from the Spring service over HTTP — this mirrors the vision's GPU plane split. The existing `ai-assistant-service` is split into three single-responsibility modules: `financial-agent-service` (re-plans on drift), `summary-llm-service` (Kazakh/Russian/English summaries → notifications), `recommendation-service` (targeting signals → integration-service → partners). New Kafka topics (`analytics.metrics-computed`, `analytics.plan-drift-detected`, `ai.summary-generated`, `ai.recommendation-ready`) fan work out asynchronously; synchronous calls reuse the existing Eureka `lb://` pattern.

**Tech Stack:** Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0, Gradle Kotlin DSL, PostgreSQL 16, Flyway, Apache Kafka 3.9 (KRaft), Spring Kafka, Spring Cloud Gateway, Eureka, Keycloak 26, Micrometer + OTel, Lombok, springdoc-openapi. Python 3.11, FastAPI, pandas, numpy (Python worker only). Anthropic SDK (existing; documented as on-prem GPU stand-in).

---

## Phase Map

| Phase | Adds | Independently shippable? |
|---|---|---|
| A | `analytics-service` module skeleton (gradle, ports, security, eureka, flyway, gateway route) | Yes — `/api/analytics/health` works |
| B | Kafka ingest + scheduled batch ETL | Yes — txns land in `txn_fact` |
| C | Python worker (`analytics-worker`) + metrics compute | Yes — metrics show via REST |
| D | Drift detector + event emission | Yes — drift events visible in Kafka UI |
| E | Event contracts in `common` | Yes — compile-only |
| F | Split `ai-assistant-service` → `financial-agent-service`, `summary-llm-service`, `recommendation-service` (keep existing as deprecated shim until F.5) | Yes |
| G | Wire AI services as consumers of analytics events | Yes |
| H | `integration-service` consumes targeting signals → stub partner call | Yes |
| I | Observability: metrics, dashboard, OTel | Yes |

**Stop after Phase E** = MVP for analytics integration (metrics + drift visible to other services). Phases F–I add the AI services.

---

## File Structure

### New module: `analytics-service/` (Java)
- `build.gradle.kts` — Spring Boot module
- `src/main/java/kz/halyk/maqsat/analytics/AnalyticsServiceApplication.java`
- `src/main/java/kz/halyk/maqsat/analytics/config/SecurityConfig.java` — JWT resource server (copy from budget)
- `src/main/java/kz/halyk/maqsat/analytics/config/KafkaConfig.java` — `ByteArrayJsonMessageConverter` (multi-type later) + producer
- `src/main/java/kz/halyk/maqsat/analytics/config/WorkerClientConfig.java` — `WebClient` for Python worker (NOT load-balanced; direct URL via env)
- `src/main/java/kz/halyk/maqsat/analytics/config/WebClientConfig.java` — `@LoadBalanced` `WebClient` for budget-service lookups
- `src/main/java/kz/halyk/maqsat/analytics/domain/TxnFact.java` — staging row (one per `TransactionCategorized`)
- `src/main/java/kz/halyk/maqsat/analytics/domain/UserMetrics.java` — period snapshot
- `src/main/java/kz/halyk/maqsat/analytics/domain/CategoryStat.java`
- `src/main/java/kz/halyk/maqsat/analytics/domain/RecurringDebit.java`
- `src/main/java/kz/halyk/maqsat/analytics/domain/DriftReport.java`
- `src/main/java/kz/halyk/maqsat/analytics/repository/TxnFactRepository.java`
- `src/main/java/kz/halyk/maqsat/analytics/repository/UserMetricsRepository.java`
- `src/main/java/kz/halyk/maqsat/analytics/repository/CategoryStatRepository.java`
- `src/main/java/kz/halyk/maqsat/analytics/repository/RecurringDebitRepository.java`
- `src/main/java/kz/halyk/maqsat/analytics/repository/DriftReportRepository.java`
- `src/main/java/kz/halyk/maqsat/analytics/listener/TransactionCategorizedListener.java`
- `src/main/java/kz/halyk/maqsat/analytics/service/IngestService.java`
- `src/main/java/kz/halyk/maqsat/analytics/service/MetricsService.java` — orchestrates the Python worker
- `src/main/java/kz/halyk/maqsat/analytics/service/DriftService.java`
- `src/main/java/kz/halyk/maqsat/analytics/service/BatchEtlJob.java` — `@Scheduled` cron
- `src/main/java/kz/halyk/maqsat/analytics/client/AnalyticsWorkerClient.java`
- `src/main/java/kz/halyk/maqsat/analytics/client/BudgetClient.java`
- `src/main/java/kz/halyk/maqsat/analytics/controller/MetricsController.java`
- `src/main/java/kz/halyk/maqsat/analytics/controller/DriftController.java`
- `src/main/java/kz/halyk/maqsat/analytics/dto/UserMetricsResponse.java`
- `src/main/java/kz/halyk/maqsat/analytics/dto/DriftReportResponse.java`
- `src/main/java/kz/halyk/maqsat/analytics/dto/WorkerComputeRequest.java`
- `src/main/java/kz/halyk/maqsat/analytics/dto/WorkerComputeResponse.java`
- `src/main/java/kz/halyk/maqsat/analytics/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V1__init.sql`
- `src/test/java/kz/halyk/maqsat/analytics/service/IngestServiceTest.java`
- `src/test/java/kz/halyk/maqsat/analytics/service/DriftServiceTest.java`
- `src/test/java/kz/halyk/maqsat/analytics/listener/TransactionCategorizedListenerTest.java`

### New module: `analytics-worker/` (Python sidecar)
- `Dockerfile`
- `pyproject.toml`
- `app/main.py` — FastAPI entrypoint
- `app/compute.py` — metrics algorithms (ported from `anal-service/.../halyk_etl.ipynb`)
- `app/models.py` — pydantic schemas matching `WorkerComputeRequest/Response`
- `tests/test_compute.py` — pytest

### Modified files
- `settings.gradle.kts` — register `analytics-service` + (Phase F) `financial-agent-service`, `summary-llm-service`, `recommendation-service`
- `common/src/main/java/kz/halyk/maqsat/common/event/EventTopics.java` — add 4 topic constants
- `common/src/main/java/kz/halyk/maqsat/common/event/MetricsComputed.java` (new record)
- `common/src/main/java/kz/halyk/maqsat/common/event/PlanDriftDetected.java` (new record)
- `common/src/main/java/kz/halyk/maqsat/common/event/SummaryGenerated.java` (new record)
- `common/src/main/java/kz/halyk/maqsat/common/event/RecommendationReady.java` (new record)
- `gateway/src/main/resources/application.yml` — add routes for `/api/analytics/**`, `/api/financial-agent/**`, `/api/summary/**`, `/api/recommendations/**`
- `docker-compose.yml` — add `analytics-service`, `analytics-worker`, `financial-agent-service`, `summary-llm-service`, `recommendation-service`; add Postgres init for `analytics_db`
- `docker-compose.dev.yml` — bind-mounts for new jars
- `db/init-databases.sql` — `CREATE DATABASE analytics_db`
- `integration-service/.../listener/RecommendationListener.java` (new) — consumes `ai.recommendation-ready`
- `notification-service/.../listener/SummaryListener.java` (new) — consumes `ai.summary-generated`
- `docs/ARCHITECTURE.md` — add §17 analytics & AI plane
- `Maqsat_Family_Technical_Context.md` — update §3 service table

### New modules (Phase F — AI split)
- `financial-agent-service/` — re-plan on drift (extracts `generatePlan` from existing ai-assistant-service)
- `summary-llm-service/` — multilingual summary generation
- `recommendation-service/` — targeting signal generator

Each follows the same skeleton: `controller/`, `service/`, `dto/`, `client/` (Anthropic), `listener/` (Kafka consumer), `config/SecurityConfig.java`, `config/AnthropicProperties.java`, `application.yml`.

### Deprecated (Phase F.5)
- `ai-assistant-service/` — kept until F.5; then removed from `settings.gradle.kts`, `docker-compose.yml`, gateway routes. Its `/api/ai/budget-plan` becomes a thin proxy to `financial-agent-service` for one release, then deleted.

---

## Data model (`analytics_db` — Phase A V1__init.sql)

```sql
-- Staging: one row per consumed TransactionCategorized event
CREATE TABLE txn_fact (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  uuid        NOT NULL UNIQUE,
    user_id         varchar(64) NOT NULL,
    account_id      varchar(64) NOT NULL,
    amount          numeric(15,2) NOT NULL,
    mcc             varchar(8),
    category_name   varchar(128) NOT NULL,
    occurred_at     timestamptz NOT NULL,
    ingested_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_txn_fact_user_period ON txn_fact(user_id, occurred_at);

-- Per-user per-period rollup (period = YYYY-MM)
CREATE TABLE user_metrics (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             varchar(64) NOT NULL,
    period              varchar(7)  NOT NULL,
    income_estimate     numeric(15,2),
    total_spent         numeric(15,2) NOT NULL,
    avg_transaction     numeric(15,2),
    median_transaction  numeric(15,2),
    volatility          numeric(6,4),
    savings_rate        numeric(6,4),
    computed_at         timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, period)
);

-- Per-user-per-category-per-period
CREATE TABLE category_stat (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         varchar(64) NOT NULL,
    period          varchar(7)  NOT NULL,
    category_name   varchar(128) NOT NULL,
    txn_count       int NOT NULL,
    total_amount    numeric(15,2) NOT NULL,
    avg_amount      numeric(15,2) NOT NULL,
    median_amount   numeric(15,2) NOT NULL,
    UNIQUE (user_id, period, category_name)
);

-- Detected recurring debits (subscriptions, utilities)
CREATE TABLE recurring_debit (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         varchar(64) NOT NULL,
    label           varchar(128) NOT NULL,
    amount          numeric(15,2) NOT NULL,
    day_of_month    int,
    confidence      numeric(4,3) NOT NULL,
    last_seen_at    timestamptz NOT NULL,
    UNIQUE (user_id, label)
);

-- Plan-vs-actual drift snapshot
CREATE TABLE drift_report (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             varchar(64) NOT NULL,
    plan_id             uuid NOT NULL,
    period              varchar(7) NOT NULL,
    matches             boolean NOT NULL,
    drift_by_category   jsonb NOT NULL,
    recommend_adjustment boolean NOT NULL,
    computed_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_drift_user_period ON drift_report(user_id, period);
```

---

## Event contracts (Phase E)

```java
// common/.../event/MetricsComputed.java
public record MetricsComputed(
    String userId, String period,
    BigDecimal incomeEstimate, BigDecimal totalSpent,
    BigDecimal volatility, BigDecimal savingsRate,
    Instant computedAt
) {}

// common/.../event/PlanDriftDetected.java
public record PlanDriftDetected(
    String userId, UUID planId, String period,
    Map<String, BigDecimal> driftByCategory,
    boolean recommendAdjustment, Instant computedAt
) {}

// common/.../event/SummaryGenerated.java
public record SummaryGenerated(
    String userId, String period, String language,
    String summaryText, List<String> highlights, List<String> suggestions,
    Instant generatedAt
) {}

// common/.../event/RecommendationReady.java
public record RecommendationReady(
    String userId, List<String> goalCategories, List<String> interests,
    List<String> targetAudienceTags, List<String> suggestedOfferTypes,
    Instant generatedAt
) {}
```

```java
// common/.../event/EventTopics.java — additions
public static final String ANALYTICS_METRICS_COMPUTED = "analytics.metrics-computed";
public static final String ANALYTICS_PLAN_DRIFT_DETECTED = "analytics.plan-drift-detected";
public static final String AI_SUMMARY_GENERATED = "ai.summary-generated";
public static final String AI_RECOMMENDATION_READY = "ai.recommendation-ready";
```

---

# Phase A — `analytics-service` skeleton

### Task A1: Register module in Gradle

**Files:**
- Modify: `settings.gradle.kts`
- Create: `analytics-service/build.gradle.kts`

- [ ] **Step 1: Modify `settings.gradle.kts` to add `analytics-service`**

```kotlin
include(
    "common",
    "eureka-server",
    "gateway",
    "auth-service",
    "budget-service",
    "transaction-service",
    "goals-service",
    "family-service",
    "ai-assistant-service",
    "notification-service",
    "integration-service",
    "analytics-service",
)
```

- [ ] **Step 2: Create `analytics-service/build.gradle.kts`** (copy structure from `budget-service/build.gradle.kts`, change name only)

```kotlin
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
}
```

- [ ] **Step 3: Verify build skeleton compiles**

Run: `./gradlew :analytics-service:help`
Expected: BUILD SUCCESSFUL (no source files yet, just the build script resolves).

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts analytics-service/build.gradle.kts
git commit -m "feat(analytics): register analytics-service Gradle module"
```

### Task A2: Application class + minimal `application.yml`

**Files:**
- Create: `analytics-service/src/main/java/kz/halyk/maqsat/analytics/AnalyticsServiceApplication.java`
- Create: `analytics-service/src/main/resources/application.yml`

- [ ] **Step 1: Create Application class**

```java
package kz.halyk.maqsat.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
```

- [ ] **Step 2: Create `application.yml`** (port 8090, copy db config pattern from budget)

```yaml
server:
  port: ${PORT:8090}

spring:
  application:
    name: analytics-service
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:analytics_db}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:29092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: analytics-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.value.default.type: kz.halyk.maqsat.common.event.TransactionCategorized
        spring.json.trusted.packages: kz.halyk.maqsat.common.event
        spring.json.use.type.headers: false
    template:
      observation-enabled: true
    listener:
      observation-enabled: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER:http://localhost:8081/realms/maqsat}

eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_HOST:localhost}:${EUREKA_PORT:8761}/eureka
  instance:
    prefer-ip-address: true

analytics:
  worker:
    base-url: ${ANALYTICS_WORKER_URL:http://localhost:9000}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  otlp:
    tracing:
      endpoint: ${MANAGEMENT_OTLP_TRACING_ENDPOINT:}
```

- [ ] **Step 3: Build + run**

Run: `./gradlew :analytics-service:bootRun`
Expected: starts and registers in Eureka. Fails Flyway validate (no migrations yet) — that's OK, fixed in Task A3.

- [ ] **Step 4: Commit**

```bash
git add analytics-service/src/main/java/kz/halyk/maqsat/analytics/AnalyticsServiceApplication.java analytics-service/src/main/resources/application.yml
git commit -m "feat(analytics): bootstrap Spring Boot application + config"
```

### Task A3: Flyway V1 schema

**Files:**
- Create: `analytics-service/src/main/resources/db/migration/V1__init.sql`
- Modify: `db/init-databases.sql`

- [ ] **Step 1: Add database to init script**

Add to `db/init-databases.sql`:
```sql
CREATE DATABASE analytics_db;
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO postgres;
```

- [ ] **Step 2: Create `V1__init.sql`** — paste the full schema from the "Data model" section above.

- [ ] **Step 3: Manually create the DB on the host Postgres**

Run: `psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE analytics_db;"`
Expected: `CREATE DATABASE`

- [ ] **Step 4: Boot the service to apply migration**

Run: `./gradlew :analytics-service:bootRun`
Expected: log line `Successfully applied 1 migration to schema "public"`

- [ ] **Step 5: Verify tables exist**

Run: `psql -h localhost -p 5432 -U postgres -d analytics_db -c "\dt"`
Expected: 5 tables (`txn_fact`, `user_metrics`, `category_stat`, `recurring_debit`, `drift_report`).

- [ ] **Step 6: Commit**

```bash
git add analytics-service/src/main/resources/db/migration/V1__init.sql db/init-databases.sql
git commit -m "feat(analytics): add V1 schema (txn_fact, user_metrics, category_stat, recurring_debit, drift_report)"
```

### Task A4: SecurityConfig + exception handler + health endpoint

**Files:**
- Create: `analytics-service/src/main/java/kz/halyk/maqsat/analytics/config/SecurityConfig.java`
- Create: `analytics-service/src/main/java/kz/halyk/maqsat/analytics/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Copy `SecurityConfig` from `budget-service`**

```java
package kz.halyk.maqsat.analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> {}));
        return http.build();
    }
}
```

- [ ] **Step 2: Create `GlobalExceptionHandler`** mirroring `budget-service` (returns `ApiError` from common).

- [ ] **Step 3: Smoke test — `/actuator/health`**

Run: `curl http://localhost:8090/actuator/health`
Expected: `{"status":"UP"}`

- [ ] **Step 4: Commit**

```bash
git add analytics-service/src/main/java/kz/halyk/maqsat/analytics/config/SecurityConfig.java analytics-service/src/main/java/kz/halyk/maqsat/analytics/exception/GlobalExceptionHandler.java
git commit -m "feat(analytics): security + exception handler"
```

### Task A5: Gateway route + docker-compose entry

**Files:**
- Modify: `gateway/src/main/resources/application.yml`
- Modify: `docker-compose.yml`
- Modify: `docker-compose.dev.yml`

- [ ] **Step 1: Add gateway route** under `spring.cloud.gateway.routes`:

```yaml
- id: analytics-service
  uri: lb://analytics-service
  predicates:
    - Path=/api/analytics/**
```

- [ ] **Step 2: Add to `docker-compose.yml`** (model on `budget-service` block, port 8090, env vars: `EUREKA_HOST=eureka`, `KEYCLOAK_ISSUER=http://keycloak:8080/realms/maqsat`, `POSTGRES_HOST=postgres`, `POSTGRES_DB=analytics_db`, `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092`, `ANALYTICS_WORKER_URL=http://analytics-worker:9000`).

- [ ] **Step 3: Add to `docker-compose.dev.yml`** — bind-mount jar (copy pattern from budget).

- [ ] **Step 4: Verify aggregated Swagger picks it up**

Run: `./gradlew :analytics-service:build && docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d analytics-service`
Wait 30s. Open: `http://localhost:8080/swagger-ui.html`
Expected: `analytics-service` appears in the dropdown.

- [ ] **Step 5: Commit**

```bash
git add gateway/src/main/resources/application.yml docker-compose.yml docker-compose.dev.yml
git commit -m "feat(analytics): wire gateway route + compose service"
```

---

# Phase B — Ingest (Kafka + scheduled batch)

### Task B1: `TxnFact` entity + repository

**Files:**
- Create: `analytics-service/src/main/java/kz/halyk/maqsat/analytics/domain/TxnFact.java`
- Create: `analytics-service/src/main/java/kz/halyk/maqsat/analytics/repository/TxnFactRepository.java`
- Test: `analytics-service/src/test/java/kz/halyk/maqsat/analytics/repository/TxnFactRepositoryTest.java`

- [ ] **Step 1: Write failing repository test**

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TxnFactRepositoryTest {
    @Autowired TxnFactRepository repo;

    @Test
    void persistsAndRetrievesByUserAndPeriod() {
        TxnFact f = new TxnFact();
        f.setTransactionId(UUID.randomUUID());
        f.setUserId("u1");
        f.setAccountId("a1");
        f.setAmount(new BigDecimal("1000"));
        f.setMcc("5411");
        f.setCategoryName("Продукты");
        f.setOccurredAt(Instant.parse("2026-05-15T10:00:00Z"));
        repo.save(f);

        List<TxnFact> found = repo.findByUserIdAndOccurredAtBetween(
            "u1",
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(found).hasSize(1);
    }
}
```

- [ ] **Step 2: Run, verify FAIL** (no entity)

Run: `./gradlew :analytics-service:test --tests TxnFactRepositoryTest`
Expected: compilation failure.

- [ ] **Step 3: Create entity**

```java
package kz.halyk.maqsat.analytics.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "txn_fact")
@Getter @Setter @NoArgsConstructor
public class TxnFact {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @Column(nullable = false, unique = true)
    private UUID transactionId;
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String accountId;
    @Column(nullable = false)
    private BigDecimal amount;
    private String mcc;
    @Column(nullable = false)
    private String categoryName;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(nullable = false)
    private Instant ingestedAt;

    @PrePersist
    void prePersist() { if (ingestedAt == null) ingestedAt = Instant.now(); }
}
```

- [ ] **Step 4: Create repository**

```java
package kz.halyk.maqsat.analytics.repository;

import kz.halyk.maqsat.analytics.domain.TxnFact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TxnFactRepository extends JpaRepository<TxnFact, UUID> {
    boolean existsByTransactionId(UUID transactionId);
    List<TxnFact> findByUserIdAndOccurredAtBetween(String userId, Instant from, Instant to);
}
```

- [ ] **Step 5: Run test, verify PASS**

Run: `./gradlew :analytics-service:test --tests TxnFactRepositoryTest`
Expected: 1 test passing.

- [ ] **Step 6: Commit**

```bash
git add analytics-service/src/main/java/kz/halyk/maqsat/analytics/domain/TxnFact.java analytics-service/src/main/java/kz/halyk/maqsat/analytics/repository/TxnFactRepository.java analytics-service/src/test/java/kz/halyk/maqsat/analytics/repository/TxnFactRepositoryTest.java
git commit -m "feat(analytics): TxnFact entity + repository"
```

### Task B2: `IngestService` (idempotent insert)

**Files:**
- Create: `analytics-service/src/main/java/kz/halyk/maqsat/analytics/service/IngestService.java`
- Test: `analytics-service/src/test/java/kz/halyk/maqsat/analytics/service/IngestServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IngestServiceTest {
    @Autowired IngestService ingest;
    @Autowired TxnFactRepository repo;

    @Test
    void ingestsOnce_evenIfEventRepeats() {
        TransactionCategorized e = new TransactionCategorized(
            UUID.randomUUID(), "u1", "a1", new BigDecimal("500"),
            "5411", "Продукты", Instant.parse("2026-05-15T10:00:00Z"));
        ingest.handle(e);
        ingest.handle(e);
        assertThat(repo.count()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run, verify FAIL**

Run: `./gradlew :analytics-service:test --tests IngestServiceTest`
Expected: compilation failure (no IngestService).

- [ ] **Step 3: Implement service**

```java
package kz.halyk.maqsat.analytics.service;

import kz.halyk.maqsat.analytics.domain.TxnFact;
import kz.halyk.maqsat.analytics.repository.TxnFactRepository;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestService {
    private final TxnFactRepository repo;

    public void handle(TransactionCategorized e) {
        if (repo.existsByTransactionId(e.transactionId())) {
            log.debug("Skipping already-ingested txn {}", e.transactionId());
            return;
        }
        TxnFact f = new TxnFact();
        f.setTransactionId(e.transactionId());
        f.setUserId(e.userId());
        f.setAccountId(e.accountId());
        f.setAmount(e.amount());
        f.setMcc(e.mcc());
        f.setCategoryName(e.categoryName());
        f.setOccurredAt(e.occurredAt());
        repo.save(f);
    }
}
```

- [ ] **Step 4: Run test, verify PASS**

- [ ] **Step 5: Commit**

```bash
git add analytics-service/src/main/java/kz/halyk/maqsat/analytics/service/IngestService.java analytics-service/src/test/java/kz/halyk/maqsat/analytics/service/IngestServiceTest.java
git commit -m "feat(analytics): idempotent ingest of TransactionCategorized"
```

### Task B3: Kafka listener

**Files:**
- Create: `analytics-service/src/main/java/kz/halyk/maqsat/analytics/listener/TransactionCategorizedListener.java`
- Test: `analytics-service/src/test/java/kz/halyk/maqsat/analytics/listener/TransactionCategorizedListenerTest.java`

- [ ] **Step 1: Write failing test** using `@EmbeddedKafka`

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "transaction.categorized")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionCategorizedListenerTest {
    @Autowired KafkaTemplate<String, Object> producer;
    @Autowired TxnFactRepository repo;

    @Test
    void consumesAndPersists() throws Exception {
        TransactionCategorized e = new TransactionCategorized(
            UUID.randomUUID(), "u1", "a1", new BigDecimal("750"),
            "5411", "Продукты", Instant.now());
        producer.send("transaction.categorized", "u1", e).get();
        await().atMost(Duration.ofSeconds(10)).until(() -> repo.count() == 1);
    }
}
```

- [ ] **Step 2: Run, verify FAIL**

- [ ] **Step 3: Create listener**

```java
package kz.halyk.maqsat.analytics.listener;

import kz.halyk.maqsat.analytics.service.IngestService;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionCategorizedListener {
    private final IngestService ingest;

    @KafkaListener(topics = EventTopics.TRANSACTION_CATEGORIZED, groupId = "analytics-service")
    public void onMessage(TransactionCategorized event) {
        ingest.handle(event);
    }
}
```

- [ ] **Step 4: Run test, verify PASS**

- [ ] **Step 5: Commit**

```bash
git add analytics-service/src/main/java/kz/halyk/maqsat/analytics/listener/TransactionCategorizedListener.java analytics-service/src/test/java/kz/halyk/maqsat/analytics/listener/TransactionCategorizedListenerTest.java
git commit -m "feat(analytics): Kafka listener for transaction.categorized"
```

### Task B4: Scheduled DB pull (backfill) from `transaction-service`

The vision says analytics may also read transactional stores directly. For our isolated-db architecture we instead expose a read endpoint on transaction-service.

**Files:**
- Modify: `transaction-service/.../controller/TransactionController.java` — add `GET /api/transactions/analytics/since`
- Create: `analytics-service/.../client/TransactionsClient.java`
- Create: `analytics-service/.../service/BatchEtlJob.java`
- Test: `analytics-service/.../service/BatchEtlJobTest.java`

- [ ] **Step 1: Add endpoint to transaction-service**

```java
@GetMapping("/analytics/since")
@PreAuthorize("hasAnyRole('ANALYTICS','SERVICE')") // optional; permitAll if no service role yet
public List<TransactionResponse> since(@RequestParam Instant since) {
    return repo.findByOccurredAtGreaterThanEqual(since).stream().map(this::toDto).toList();
}
```

(Repository: add `findByOccurredAtGreaterThanEqual(Instant)`.)

- [ ] **Step 2: Create `TransactionsClient` using `@LoadBalanced WebClient`**

```java
@Component
@RequiredArgsConstructor
public class TransactionsClient {
    private final WebClient webClient;

    public List<TransactionRow> fetchSince(Instant since, String bearer) {
        return webClient.get()
            .uri("lb://transaction-service/api/transactions/analytics/since?since={s}", since.toString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
            .retrieve()
            .bodyToFlux(TransactionRow.class)
            .collectList()
            .block();
    }
}
```

- [ ] **Step 3: Create `BatchEtlJob` with `@Scheduled(cron = "${analytics.etl.cron:0 0 2 * * *}")`**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchEtlJob {
    private final TransactionsClient txClient;
    private final IngestService ingest;
    private final TxnFactRepository repo;

    @Scheduled(cron = "${analytics.etl.cron:0 0 2 * * *}")
    public void run() {
        Instant since = repo.findTopByOrderByIngestedAtDesc()
            .map(TxnFact::getIngestedAt).orElse(Instant.now().minus(Duration.ofDays(30)));
        var rows = txClient.fetchSince(since, serviceToken());
        rows.forEach(r -> ingest.handle(r.toEvent()));
        log.info("Batch ETL ingested {} rows since {}", rows.size(), since);
    }

    private String serviceToken() {
        // TODO Task I3: replace with client_credentials token via Keycloak;
        // for the demo, batch ETL runs only inside docker network and the endpoint may be permitAll.
        return "";
    }
}
```

- [ ] **Step 4: Test** — happy path with mock client, asserts `ingest.handle` called per row.

- [ ] **Step 5: Run test, verify PASS**

- [ ] **Step 6: Commit**

```bash
git add transaction-service/src/main/java/kz/halyk/maqsat/transaction analytics-service/src/main/java/kz/halyk/maqsat/analytics/client/TransactionsClient.java analytics-service/src/main/java/kz/halyk/maqsat/analytics/service/BatchEtlJob.java analytics-service/src/test/java/kz/halyk/maqsat/analytics/service/BatchEtlJobTest.java
git commit -m "feat(analytics): scheduled batch ETL from transaction-service"
```

---

# Phase C — Python worker + metrics compute

### Task C1: `analytics-worker` skeleton (FastAPI)

**Files:**
- Create: `analytics-worker/pyproject.toml`
- Create: `analytics-worker/Dockerfile`
- Create: `analytics-worker/app/main.py`
- Create: `analytics-worker/app/models.py`
- Create: `analytics-worker/app/compute.py`
- Test: `analytics-worker/tests/test_compute.py`

- [ ] **Step 1: `pyproject.toml`**

```toml
[project]
name = "analytics-worker"
version = "0.1.0"
requires-python = ">=3.11"
dependencies = [
    "fastapi>=0.115",
    "uvicorn[standard]>=0.30",
    "pydantic>=2.7",
    "pandas>=2.2",
    "numpy>=1.26",
]

[tool.pytest.ini_options]
testpaths = ["tests"]
```

- [ ] **Step 2: `app/models.py`**

```python
from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class TxnRow(BaseModel):
    transaction_id: str
    user_id: str
    amount: float
    category_name: str
    occurred_at: datetime
    mcc: Optional[str] = None

class WorkerComputeRequest(BaseModel):
    user_id: str
    period: str  # YYYY-MM
    transactions: list[TxnRow]
    income_hint: Optional[float] = None

class CategoryStat(BaseModel):
    category_name: str
    txn_count: int
    total_amount: float
    avg_amount: float
    median_amount: float

class RecurringDebit(BaseModel):
    label: str
    amount: float
    day_of_month: Optional[int]
    confidence: float

class WorkerComputeResponse(BaseModel):
    user_id: str
    period: str
    income_estimate: Optional[float]
    total_spent: float
    avg_transaction: float
    median_transaction: float
    volatility: float
    savings_rate: float
    categories: list[CategoryStat]
    recurring: list[RecurringDebit]
```

- [ ] **Step 3: `app/compute.py`** — port the notebook math

```python
import pandas as pd, numpy as np
from .models import (WorkerComputeRequest, WorkerComputeResponse,
                     CategoryStat, RecurringDebit)

def compute(req: WorkerComputeRequest) -> WorkerComputeResponse:
    df = pd.DataFrame([t.model_dump() for t in req.transactions])
    if df.empty:
        return WorkerComputeResponse(user_id=req.user_id, period=req.period,
            income_estimate=req.income_hint, total_spent=0.0,
            avg_transaction=0.0, median_transaction=0.0,
            volatility=0.0, savings_rate=0.0, categories=[], recurring=[])
    total = float(df.amount.sum())
    avg, med = float(df.amount.mean()), float(df.amount.median())
    vol = float(df.amount.std() / df.amount.mean()) if df.amount.mean() else 0.0
    income = req.income_hint or float(total)
    savings_rate = max(0.0, (income - total) / income) if income > 0 else 0.0

    cats = (df.groupby("category_name")
              .agg(txn_count=("amount", "count"),
                   total_amount=("amount", "sum"),
                   avg_amount=("amount", "mean"),
                   median_amount=("amount", "median"))
              .reset_index())
    categories = [CategoryStat(**row) for row in cats.to_dict("records")]

    # Recurring: same (rounded) amount within ±2 KZT, ≥3 occurrences across distinct days
    df["day"] = pd.to_datetime(df["occurred_at"]).dt.day
    rec_rows = []
    for amt, grp in df.groupby(df["amount"].round()):
        if len(grp) >= 3 and grp["day"].nunique() >= 3:
            rec_rows.append(RecurringDebit(
                label=grp.iloc[0].category_name,
                amount=float(amt),
                day_of_month=int(grp["day"].mode().iloc[0]),
                confidence=min(1.0, len(grp) / 6.0)))

    return WorkerComputeResponse(
        user_id=req.user_id, period=req.period,
        income_estimate=income, total_spent=total,
        avg_transaction=avg, median_transaction=med,
        volatility=round(vol, 4), savings_rate=round(savings_rate, 4),
        categories=categories, recurring=rec_rows)
```

- [ ] **Step 4: `app/main.py`**

```python
from fastapi import FastAPI
from .models import WorkerComputeRequest, WorkerComputeResponse
from .compute import compute

app = FastAPI(title="analytics-worker")

@app.get("/health")
def health(): return {"status": "ok"}

@app.post("/compute", response_model=WorkerComputeResponse)
def do_compute(req: WorkerComputeRequest): return compute(req)
```

- [ ] **Step 5: `tests/test_compute.py`** — 3 test cases (empty input, single txn, recurring detection)

```python
from datetime import datetime
from app.models import WorkerComputeRequest, TxnRow
from app.compute import compute

def _tx(amount, day, cat="Подписки"):
    return TxnRow(transaction_id="t", user_id="u",
                  amount=amount, category_name=cat,
                  occurred_at=datetime(2026, 5, day))

def test_empty():
    r = compute(WorkerComputeRequest(user_id="u", period="2026-05", transactions=[]))
    assert r.total_spent == 0

def test_single():
    r = compute(WorkerComputeRequest(user_id="u", period="2026-05",
                                     transactions=[_tx(1000, 1)],
                                     income_hint=10000))
    assert r.total_spent == 1000
    assert r.savings_rate == 0.9

def test_recurring_detected():
    r = compute(WorkerComputeRequest(user_id="u", period="2026-05",
                                     transactions=[_tx(990, 1), _tx(990, 8),
                                                   _tx(990, 15)],
                                     income_hint=10000))
    assert len(r.recurring) == 1
    assert r.recurring[0].amount == 990.0
```

- [ ] **Step 6: Run pytest, verify PASS**

Run: `cd analytics-worker && pip install -e . && pytest`
Expected: 3 passed.

- [ ] **Step 7: `Dockerfile`**

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY pyproject.toml ./
RUN pip install --no-cache-dir .
COPY app ./app
EXPOSE 9000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9000"]
```

- [ ] **Step 8: Add `analytics-worker` to `docker-compose.yml`**

```yaml
analytics-worker:
  build: ./analytics-worker
  ports: ["9000:9000"]
  networks: [maqsat]
```

- [ ] **Step 9: Smoke test running container**

Run: `docker compose up -d analytics-worker && curl http://localhost:9000/health`
Expected: `{"status":"ok"}`

- [ ] **Step 10: Commit**

```bash
git add analytics-worker
git commit -m "feat(analytics-worker): FastAPI Python sidecar with metrics compute"
```

### Task C2: `AnalyticsWorkerClient` (Java)

**Files:**
- Create: `analytics-service/.../client/AnalyticsWorkerClient.java`
- Create: `analytics-service/.../dto/WorkerComputeRequest.java`
- Create: `analytics-service/.../dto/WorkerComputeResponse.java`
- Test: `analytics-service/.../client/AnalyticsWorkerClientTest.java` — `MockWebServer`

- [ ] **Step 1: DTOs** (records mirroring the Python `models.py`)

- [ ] **Step 2: Write failing test** using `MockWebServer`

```java
class AnalyticsWorkerClientTest {
    static MockWebServer server;
    AnalyticsWorkerClient client;

    @BeforeAll static void start() throws IOException { server = new MockWebServer(); server.start(); }
    @AfterAll  static void stop()  throws IOException { server.shutdown(); }

    @BeforeEach void setup() {
        client = new AnalyticsWorkerClient(WebClient.builder().build(), server.url("/").toString().replaceFirst("/$", ""));
    }

    @Test
    void postsAndDeserializes() {
        server.enqueue(new MockResponse().setBody("""
            {"user_id":"u","period":"2026-05","income_estimate":10000,"total_spent":1000,
             "avg_transaction":1000,"median_transaction":1000,"volatility":0,"savings_rate":0.9,
             "categories":[],"recurring":[]}""")
            .addHeader("Content-Type", "application/json"));
        WorkerComputeResponse r = client.compute(new WorkerComputeRequest("u","2026-05", List.of(), new BigDecimal("10000")));
        assertThat(r.totalSpent()).isEqualByComparingTo("1000");
    }
}
```

- [ ] **Step 3: Implement client**

```java
@Component
public class AnalyticsWorkerClient {
    private final WebClient web;
    private final String baseUrl;

    public AnalyticsWorkerClient(WebClient.Builder builder,
                                 @Value("${analytics.worker.base-url}") String baseUrl) {
        this.web = builder.build();
        this.baseUrl = baseUrl;
    }

    public WorkerComputeResponse compute(WorkerComputeRequest req) {
        return web.post()
            .uri(baseUrl + "/compute")
            .bodyValue(req)
            .retrieve()
            .bodyToMono(WorkerComputeResponse.class)
            .block();
    }
}
```

- [ ] **Step 4: Run test, PASS**

- [ ] **Step 5: Commit**

```bash
git add analytics-service/src/main/java/kz/halyk/maqsat/analytics/client/AnalyticsWorkerClient.java analytics-service/src/main/java/kz/halyk/maqsat/analytics/dto analytics-service/src/test/java/kz/halyk/maqsat/analytics/client/AnalyticsWorkerClientTest.java
git commit -m "feat(analytics): Java client for analytics-worker"
```

### Task C3: `MetricsService` — orchestrate compute + persist + emit event

**Files:**
- Create: `analytics-service/.../domain/UserMetrics.java`, `CategoryStat.java`, `RecurringDebit.java`
- Create: `analytics-service/.../repository/UserMetricsRepository.java`, `CategoryStatRepository.java`, `RecurringDebitRepository.java`
- Create: `analytics-service/.../service/MetricsService.java`
- Test: `analytics-service/.../service/MetricsServiceTest.java`

- [ ] **Step 1: Entities + repositories** (mirror SQL columns; `UNIQUE` keys map to `@UniqueConstraint`)

- [ ] **Step 2: Failing test** — given seeded `txn_fact` rows for user `u1` period `2026-05`, when `metricsService.computeForUser("u1", "2026-05")` runs, then (a) a `UserMetrics` row exists, (b) `CategoryStat` rows match the worker output, (c) a `MetricsComputed` event is published. Use `KafkaTemplate` mock + repository fixture.

- [ ] **Step 3: Implement service**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {
    private final TxnFactRepository txnRepo;
    private final UserMetricsRepository metricsRepo;
    private final CategoryStatRepository catRepo;
    private final RecurringDebitRepository recRepo;
    private final AnalyticsWorkerClient worker;
    private final KafkaTemplate<String, Object> producer;

    @Transactional
    public UserMetrics computeForUser(String userId, String period) {
        var range = monthRange(period);
        var txns = txnRepo.findByUserIdAndOccurredAtBetween(userId, range.start(), range.end());
        var req = toWorkerRequest(userId, period, txns);
        var resp = worker.compute(req);
        var m = upsertMetrics(resp);
        upsertCategories(resp);
        upsertRecurring(resp);
        producer.send(EventTopics.ANALYTICS_METRICS_COMPUTED, userId,
            new MetricsComputed(userId, period, m.getIncomeEstimate(), m.getTotalSpent(),
                m.getVolatility(), m.getSavingsRate(), m.getComputedAt()));
        return m;
    }
    // helpers omitted for brevity — implement upsert by (user_id, period) etc.
}
```

- [ ] **Step 4: Run test, PASS**

- [ ] **Step 5: Wire into `BatchEtlJob`** — after ingesting new txns, call `metricsService.computeForUser` for every distinct affected `(userId, period)`.

- [ ] **Step 6: Commit**

```bash
git add analytics-service/src/main/java/kz/halyk/maqsat/analytics/{domain,repository,service}/ analytics-service/src/test/java/kz/halyk/maqsat/analytics/service/MetricsServiceTest.java
git commit -m "feat(analytics): MetricsService orchestrates worker + emits metrics-computed"
```

### Task C4: `MetricsController` — read API

**Files:**
- Create: `analytics-service/.../controller/MetricsController.java`
- Create: `analytics-service/.../dto/UserMetricsResponse.java`
- Test: `analytics-service/.../controller/MetricsControllerTest.java`

- [ ] **Step 1: Failing test** with `MockMvc` + JWT mock

- [ ] **Step 2: Controller**

```java
@RestController
@RequestMapping("/api/analytics/metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final UserMetricsRepository repo;
    private final CategoryStatRepository cats;

    @GetMapping("/{userId}/{period}")
    public UserMetricsResponse get(@PathVariable String userId, @PathVariable String period) {
        var m = repo.findByUserIdAndPeriod(userId, period)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var c = cats.findByUserIdAndPeriod(userId, period);
        return UserMetricsResponse.from(m, c);
    }
}
```

- [ ] **Step 3: PASS + commit**

```bash
git commit -am "feat(analytics): GET /api/analytics/metrics/{userId}/{period}"
```

---

# Phase D — Drift detection

### Task D1: `BudgetClient` (read active plan)

**Files:**
- Modify: `budget-service/.../controller/BudgetController.java` — add `GET /api/budget/internal/active/{userId}` (returns the current plan + per-category limits)
- Create: `analytics-service/.../client/BudgetClient.java` + DTOs
- Test: `analytics-service/.../client/BudgetClientTest.java` (MockWebServer)

- [ ] Steps mirror Task B4 / C2.

- [ ] **Commit:** `feat(analytics): BudgetClient (read active plan from budget-service)`

### Task D2: `DriftService`

**Files:**
- Create: `analytics-service/.../domain/DriftReport.java`
- Create: `analytics-service/.../repository/DriftReportRepository.java`
- Create: `analytics-service/.../service/DriftService.java`
- Test: `analytics-service/.../service/DriftServiceTest.java`

- [ ] **Step 1: Failing test** — given a plan {Продукты: 100000} and category stat {Продукты: 130000}, `DriftService.detect(userId, period)` returns drift report `matches=false, driftByCategory={Продукты:+0.30}, recommendAdjustment=true` AND publishes `PlanDriftDetected`.

- [ ] **Step 2: Implement** — threshold = 15 % per category configurable via `analytics.drift.threshold` (default 0.15). Persist + emit.

```java
@Service
@RequiredArgsConstructor
public class DriftService {
    private final BudgetClient budget;
    private final CategoryStatRepository cats;
    private final DriftReportRepository reports;
    private final KafkaTemplate<String, Object> producer;
    @Value("${analytics.drift.threshold:0.15}") double threshold;

    @Transactional
    public DriftReport detect(String userId, String period) {
        var plan = budget.activePlan(userId);
        var actuals = cats.findByUserIdAndPeriod(userId, period);
        var drift = new HashMap<String, BigDecimal>();
        for (var c : plan.categories()) {
            var actual = actuals.stream().filter(a -> a.getCategoryName().equalsIgnoreCase(c.name()))
                .findFirst().map(CategoryStat::getTotalAmount).orElse(BigDecimal.ZERO);
            var ratio = actual.subtract(c.limit())
                .divide(c.limit().max(BigDecimal.ONE), 4, RoundingMode.HALF_UP);
            if (ratio.abs().doubleValue() > threshold) drift.put(c.name(), ratio);
        }
        boolean matches = drift.isEmpty();
        var report = reports.save(DriftReport.of(userId, plan.id(), period, matches, drift, !matches));
        producer.send(EventTopics.ANALYTICS_PLAN_DRIFT_DETECTED, userId,
            new PlanDriftDetected(userId, plan.id(), period, drift, !matches, report.getComputedAt()));
        return report;
    }
}
```

- [ ] **Step 3: PASS + commit**

```bash
git commit -am "feat(analytics): DriftService detects budget plan drift, emits event"
```

### Task D3: Trigger drift after every metrics compute + expose REST

**Files:**
- Modify: `MetricsService.computeForUser` — call `driftService.detect(...)` at the end.
- Create: `analytics-service/.../controller/DriftController.java` — `GET /api/analytics/drift/{userId}/{period}` + `POST /api/analytics/drift/{userId}/{period}/recompute`.

- [ ] **Commit:** `feat(analytics): wire drift detection into MetricsService + expose REST`

---

# Phase E — Event contracts in `common`

### Task E1: Add the four event records

**Files:**
- Create: `common/.../event/MetricsComputed.java`
- Create: `common/.../event/PlanDriftDetected.java`
- Create: `common/.../event/SummaryGenerated.java`
- Create: `common/.../event/RecommendationReady.java`
- Modify: `common/.../event/EventTopics.java`

- [ ] **Step 1:** Paste records from the "Event contracts" section above.
- [ ] **Step 2:** Build whole project: `./gradlew build`
- [ ] **Step 3:** Commit `feat(common): add analytics + AI event contracts`

> Note: tasks B–D referenced these classes; either implement Phase E first, or stub the records earlier and finalise here. Recommendation: do Phase E (just the records) immediately after Phase A, before B.

---

# Phase F — Split `ai-assistant-service` into three GPU stand-in services

Each new service is a copy of the existing `ai-assistant-service` skeleton (SecurityConfig, AnthropicClient, AnthropicProperties, WebClientConfig, exception handler) with one prompt family.

### Task F1: `financial-agent-service`

**Files:**
- Create: `financial-agent-service/build.gradle.kts` (copy from ai-assistant)
- Create: `financial-agent-service/src/main/java/kz/halyk/maqsat/financial/...` (mirror ai-assistant layout, port 8091)
- Create: `financial-agent-service/.../listener/DriftListener.java` — consumes `analytics.plan-drift-detected`, reads metrics via `AnalyticsClient`, calls `FinancialAgentService.replan(...)`, POSTs new plan to `budget-service` via `BudgetClient`.
- Create: `financial-agent-service/.../service/FinancialAgentService.java` — single method `replan(userId, period, drift, metrics) -> BudgetPlanResult` using prompts that take **metrics + drift report** (different from current `generatePlan` which only takes income).
- Modify: `settings.gradle.kts`, `gateway/application.yml`, `docker-compose.yml`.

- [ ] **Step 1: Module skeleton + SecurityConfig copy + bootRun smoke**
- [ ] **Step 2: Failing test for `FinancialAgentService.replan` with fallback (no API key) — produces a plan with `createdByAi=true`**
- [ ] **Step 3: Implement service** — system prompt similar to existing but takes `metrics` and `driftByCategory` as inputs; constrains output to same JSON shape `{categories:[...]}`.
- [ ] **Step 4: `DriftListener` test** with `@EmbeddedKafka` + WireMocked budget POST
- [ ] **Step 5: Implement listener**
- [ ] **Step 6: Commit** `feat: financial-agent-service consumes drift, re-plans via LLM, posts new plan to budget`

### Task F2: `summary-llm-service`

**Files:** mirror F1; port 8092; consumes `analytics.metrics-computed`, calls Anthropic with `Light Summary` prompt that produces `{summaryText, highlights, suggestions}` in `kk|ru|en` based on a user profile field (default `ru`); publishes `ai.summary-generated`.

- [ ] **Step 1–6:** as F1 (TDD)
- [ ] **Commit** `feat: summary-llm-service generates multilingual spending summaries`

### Task F3: `recommendation-service`

**Files:** mirror F1; port 8093; on `analytics.metrics-computed`, also pulls goals (`lb://goals-service`), computes targeting signals (deterministic rule-based first; optionally LLM-refined), publishes `ai.recommendation-ready`.

- [ ] **Step 1–6:** as F1 (TDD)
- [ ] **Commit** `feat: recommendation-service emits ecosystem targeting signals`

### Task F4: Documentation update

- [ ] Update `docs/ARCHITECTURE.md` §3 service table (+4 rows) and §4 diagram (+3 services).
- [ ] Update `Maqsat_Family_Technical_Context.md` §3 to mark `ai-assistant-service` deprecated and add the three new services.
- [ ] **Commit** `docs: document analytics + AI plane`

### Task F5: Deprecate `ai-assistant-service`

- [ ] **Step 1:** In `ai-assistant-service.AiController`, convert `/api/ai/budget-plan` to a `WebClient` proxy of `lb://financial-agent-service/api/financial-agent/initial-plan` and `/api/ai/chat` to `lb://financial-agent-service/api/financial-agent/chat`. Add `@Deprecated`.
- [ ] **Step 2:** Run `bash demo/run-demo.sh` to confirm nothing breaks.
- [ ] **Step 3:** (later release, not this PR) remove the module — out of scope here.
- [ ] **Commit** `refactor(ai-assistant): proxy to financial-agent-service (deprecated shim)`

---

# Phase G — Consumer wiring

### Task G1: `notification-service` consumes `ai.summary-generated`

**Files:**
- Create: `notification-service/.../listener/SummaryListener.java`
- Test: `notification-service/.../listener/SummaryListenerTest.java`

- [ ] **Step 1:** TDD listener that converts `SummaryGenerated` event into a `[PUSH]` log line (same pattern as existing SmartPushListener). Multi-type Kafka consumer config (already uses `ByteArrayJsonMessageConverter` — just add a new `@KafkaListener` method).
- [ ] **Commit** `feat(notification): push AI summaries`

### Task G2: `integration-service` consumes `ai.recommendation-ready`

**Files:**
- Create: `integration-service/.../listener/RecommendationListener.java`
- Create: `integration-service/.../client/PartnerClient.java` (HTTP stub — log call to "Alser" / "Halyk Travel")
- Test: `integration-service/.../listener/RecommendationListenerTest.java`

- [ ] **Step 1:** TDD listener that picks `suggestedOfferTypes` and dispatches per partner.
- [ ] **Commit** `feat(integration): forward targeting signals to partner stubs`

---

# Phase H — Demo extension

### Task H1: Extend `demo/run-demo.sh`

- [ ] **Step 1:** After existing flow, add curl steps:
  1. `POST /api/analytics/etl/run` (manual trigger endpoint added to `BatchEtlJob`)
  2. `GET /api/analytics/metrics/{userId}/2026-05`
  3. `POST /api/analytics/drift/{userId}/2026-05/recompute`
  4. `GET /api/analytics/drift/{userId}/2026-05` → assert `matches=false` after a deliberate over-spend
  5. Wait, then `GET /api/budget/dashboard` → assert plan version bumped (re-plan landed)
  6. Tail notification + integration logs to show summary + targeting

- [ ] **Step 2:** Run `bash demo/run-demo.sh`; all curl steps return 2xx.
- [ ] **Commit** `chore(demo): exercise analytics + AI plane end-to-end`

---

# Phase I — Observability

### Task I1: Custom business metrics

- [ ] In `analytics-service`: counters `maqsat_metrics_computed_total{period}`, `maqsat_drift_detected_total{category}`.
- [ ] In `financial-agent-service`: `maqsat_replans_total`.
- [ ] In `summary-llm-service`: `maqsat_summaries_generated_total{lang}`.
- [ ] In `recommendation-service`: `maqsat_recommendations_total{partner}`.
- [ ] **Commit** `feat(observability): analytics + AI business counters`

### Task I2: Grafana dashboard panel

- [ ] Add panels to `observability/grafana/dashboards/maqsat-business.json` for the new counters.
- [ ] Restart compose with observability overlay; verify panels render.
- [ ] **Commit** `chore(observability): dashboard panels for analytics + AI plane`

### Task I3: Service-to-service auth for analytics → transaction

- [ ] Add Keycloak service-account client `analytics-service` with `client_credentials`. Use it in `TransactionsClient.serviceToken()`. Replace the stub `""` token.
- [ ] **Commit** `feat(analytics): client_credentials token for transaction-service calls`

---

## Test plan summary

| Layer | Test type | Coverage |
|---|---|---|
| Python worker | pytest unit | empty / single / recurring / volatility / savings rate edge cases |
| Java repositories | `@DataJpaTest` against ephemeral Postgres | round-trip per entity |
| Services | `@SpringBootTest` + Mockito | IngestService idempotency, MetricsService orchestration, DriftService threshold logic |
| Kafka listeners | `@EmbeddedKafka` | end-to-end publish→consume |
| Clients | `MockWebServer` | analytics-worker + transaction-service + budget-service contracts |
| Controllers | `MockMvc` + mock JWT | happy + 404 |
| End-to-end | `demo/run-demo.sh` | exercises the full plane |

## Self-review notes

- Spec coverage: every vision §4 component has a task (Analysis Notebook = Python worker / B+C; Metrics DB = A3; Metric analysis = D2; Comprehensive Financial Agent = F1; Parse Budget Plan = F1 prompt validation; Light Summary LLM = F2; AI Recommendation = F3).
- Placeholder scan: one `TODO` in B4 step 3 for `serviceToken()` — resolved by I3.
- Type consistency: `MetricsComputed`, `PlanDriftDetected`, `SummaryGenerated`, `RecommendationReady` field names match across Phase E records and all consumers.
- Independent shipping: Phase A–E ship analytics alone; F is one new service per task with no inter-dependencies; G and H assume F is complete.
