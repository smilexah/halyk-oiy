# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Maqsat & Family** — a Spring Boot microservices backend for a Halyk SuperApp add-on (AI budgeting,
savings goals, and a family layer with child daily-limits and a parental SOS-approval flow). It is a
**single Gradle multi-module monorepo** (packages `kz.halyk.maqsat.<svc>`), not separate repos.

Authoritative deeper docs: `docs/ARCHITECTURE.md` (full spec, diagrams, ER model) and `README.md`
(demo curl). Read those before large changes.

## Build, run, test

```bash
# Build everything / one module / run a single test
./gradlew build
./gradlew :budget-service:build
./gradlew :transaction-service:test --tests "CategorizationEngineTest"
./gradlew :transaction-service:test --tests "CategorizationEngineTest.categorizesByMcc"

# Run the full stack
docker compose up --build                                                      # builds an image per service
./gradlew build && docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d   # FAST: runs prebuilt jars in JRE containers
docker compose -f docker-compose.yml -f docker-compose.dev.yml -f docker-compose.observability.yml up -d   # + Prometheus/Tempo/Loki/Grafana

bash demo/run-demo.sh   # end-to-end: budget tracking, invite, conflict scenario, AI
```

- After changing code in **dev mode**, you must `./gradlew :<svc>:build` then
  `docker compose ... restart <svc>` (the dev override mounts the jar; restart reloads it).
- After mass restarts the gateway's load-balancer cache lags ~30s and returns 503 — wait, don't assume a bug.
- A single Spring Boot service can be run from the IDE/`bootRun`; it defaults all endpoints to `localhost`.

## Architecture (the big picture)

- **Modules.** Root `build.gradle.kts` configures all subprojects (Java 21 toolchain, Spring BOMs via
  `io.spring.dependency-management`, shared observability+Lombok+test deps). `common/` is a thin
  `java-library` (no bootJar): it holds **only** the Kafka event records, `EventTopics`, `ApiError`,
  and `CurrentUser`. Each service applies the Spring Boot plugin in its own `build.gradle.kts`.
- **Ports / DBs / topics** are in `docker-compose.yml` and `docs/ARCHITECTURE.md` §3,5. Gateway 8080,
  Keycloak 8081, budget 8082, transaction 8083, goals 8084, family 8085, ai 8086, notification 8087,
  integration 8088, auth 8089, eureka 8761. **Only gateway/keycloak/eureka (+ postgres/kafka for host
  tooling) publish host ports**; the 8 application services use `expose:` (internal-only) so they're
  reachable in-network via `lb://<svc>` but NOT directly from the host — all external API traffic must
  go through the gateway. (Don't `curl localhost:8089` — it won't connect; use `localhost:8080/api/...`.)
- **Cross-service state flows through Kafka; synchronous reads go through Eureka** (`@LoadBalanced
  WebClient` to `lb://<svc>`). The three event contracts (`TransactionCategorized`, `LimitExceeded`,
  `LimitOverrideApproved`) live in `common`; producers use `KafkaTemplate<String,Object>` keyed by userId.
- **The conflict scenario is the core flow and spans 4 services + 3 topics** — understand it before
  touching transaction/family: child txn → transaction reads the limit from family (REST, bearer
  propagated) → if exceeded, status `PENDING_APPROVAL` + publish `LimitExceeded` → notification SOS push
  → parent `POST /api/family/approvals/{txnId}` → family writes the override + publishes
  `LimitOverrideApproved` → transaction consumes it, sets `POSTED`, and publishes `TransactionCategorized`
  so budget counts it.
- **Categorization** (`transaction-service/.../service/CategorizationEngine.java`): MCC → category, then
  a merchant-name heuristic, else "Прочее". Category names are Russian and **must match budget category
  names exactly** — budget tracking does `equalsIgnoreCase` on them.
- **Roles `ADULT`/`CHILD` live in `family-service`** (on `membership`), never in Keycloak. Keycloak only
  answers identity; the JWT `sub` is the cross-service user id, read via `CurrentUser.current()`.

## Conventions

- Per service: `config/` (`SecurityConfig`, clients), `controller/`, `service/`, `repository/`,
  `domain/`, `dto/`, `event/`, `exception/`, `client/`. Servlet services share an identical
  `SecurityConfig` (resource server, STATELESS, permitAll `/actuator/**` `/swagger-ui/**` `/v3/api-docs/**`).
- JPA: `@GeneratedValue(strategy = UUID)`, enum `@Enumerated(STRING)`, `ddl-auto: validate` (Flyway owns
  the schema — every DB has `db/migration/V1__init.sql`; column types must match the entities or startup
  fails validation).
- **Multi-type Kafka consumers** (notification, integration) use a `ByteArrayJsonMessageConverter` so one
  consumer handles several event types; single-type consumers (budget) use `JsonDeserializer` +
  `spring.json.value.default.type`. Both set `use.type.headers=false`.
- Adding a route: extend `gateway/.../application.yml` routes (`lb://<svc>` by path prefix). Adding a
  service-to-service Swagger doc: it auto-appears in the aggregated UI at
  `http://localhost:8080/swagger-ui.html` if the service exposes `/v3/api-docs` (springdoc).

## Gotchas (hard-won — see also `memory/maqsat-env-gotchas.md`)

- **Kafka KRaft (apache/kafka):** listeners must bind to the routable `kafka` host, **not `0.0.0.0`** —
  the format step rejects a `0.0.0.0` advertised address. Services use `kafka:9092` (INTERNAL); host
  clients use `localhost:29092` (EXTERNAL).
- **Keycloak issuer:** `KC_HOSTNAME=http://keycloak:8080` fixes the token `iss` so host-minted tokens
  (`localhost:8081`) validate in-network. **Do not set both `issuer-uri` and `jwk-set-uri`** — Boot's
  issuer decoder wins and eagerly fetches the issuer URL (fails inside containers).
- **`sub` claim:** comes from Keycloak's `basic` default client scope (24+). Do not override a client's
  `defaultClientScopes` in `keycloak/realm-export.json` or `sub` disappears and `CurrentUser` breaks.
- **Host Postgres on 5432:** this dev machine runs a native Postgres on 5432; compose publishes the
  container on `${POSTGRES_HOST_PORT:-5432}` (`.env` sets 5433). In-container comms use `postgres:5432`.
- **Cyrillic over curl on Windows** turns into `?`; send JSON bodies from a UTF-8 file
  (`--data-binary @demo/plan.json`).
- **Toolchain:** Java 21 resolves from `~/.jdks/graalvm-jdk-21.*` (only JDK 22 is on PATH). Gradle wrapper
  is 9.4.1 — keep it; the user has asked not to change versions.

## Observability

Every service emits metrics (Micrometer → Prometheus via Eureka SD, plus custom `maqsat_*` business
counters), traces (OTLP → Tempo; one trace spans gateway→transaction→Kafka→consumers via
`spring.kafka.{template,listener}.observation-enabled`), and ECS-JSON logs (`traceId`/`spanId`, Alloy →
Loki). Grafana provisions 3 dashboards under `observability/grafana/`. Regenerate the architecture PDF
with headless Chrome from `docs/_arch.html` (see how the existing `docs/Maqsat-Architecture.pdf` was made).
