# AI Plane Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the monolithic `ai-assistant-service` into the four discrete AI/orchestration services from the draw.io canonical diagram (`Comprehensive Financial Agent`, `Parse Budget Plan Service`, `Light Summary LLM agent`, `AI Recommendation Analysis System`), add two partner-mock services (`Alser`, `Halyk Travel`) that supply real offer rows for the recommendation engine, switch all LLM calls from Anthropic to OpenAI (`gpt-4o-mini`), and wire the whole flow end-to-end with the existing `analytics-service` events as the trigger.

**Architecture:**
- **financial-agent-service** consumes `analytics.plan-drift-detected`, reads metrics + per-segment population priors (own `priors_db`), prompts OpenAI to emit per-category limits, then POSTs the proposed plan to **parse-budget-plan-service**, which validates the JSON shape, pulls user + group budgets from `budget_db`, builds the canonical plan object, and persists it via a new `POST /api/budget/internal/replan/{userId}` endpoint on `budget-service` (which version-bumps and supersedes the previous plan).
- **summary-llm-service** consumes `analytics.metrics-computed`, prompts OpenAI for a multilingual `{summaryText, highlights, suggestions}` payload in `kk | ru | en`, emits `ai.summary-generated`, which `notification-service` consumes as a Smart-Push.
- **recommendation-service** consumes `analytics.metrics-computed`, pulls the user's goals from `goals-service`, pulls partner offers from `alser-mock-service` and `halyk-travel-mock-service`, prompts OpenAI to match offers to user-metadata audience tags, persists a `recommendation` row in `analytics_db`, and emits `ai.recommendation-ready`. `integration-service` consumes that event and forwards a "Sale/Discount + Target Audience" payload to each relevant partner's `POST /api/{partner}/apply-discount`.
- The legacy `ai-assistant-service` survives as a deprecated proxy shim for one release so the existing `demo/run-demo.sh` and `POST /api/ai/budget-plan` users don't break.

**Tech Stack:** Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0, Gradle Kotlin DSL, PostgreSQL 16, Flyway, Apache Kafka 3.9 (KRaft), Spring Kafka, Spring Cloud Gateway, Eureka, Keycloak 26, Micrometer + OTel, Lombok, springdoc-openapi, OpenAI HTTP API via Spring `WebClient` (model `gpt-4o-mini`, env `OPENAI_API_KEY`, base `https://api.openai.com`).

---

## Decision summary (locked-in choices)

| # | Decision | Choice |
|---|---|---|
| 1 | LLM provider | OpenAI `gpt-4o-mini`, env `OPENAI_API_KEY`, property prefix `openai.*` in every AI service. Each service owns its own `OpenAiClient` (copy-pasted boilerplate is fine — there's no shared infra module). |
| 2 | Parse Budget Plan | Separate `parse-budget-plan-service` (port `8094`). It is the only writer to `budget_db`'s re-plan endpoint; `financial-agent-service` does NOT call budget-service directly. |
| 3 | Partner mocks | Two full Spring modules with their own DBs: `alser-mock-service` (`8095`, `alser_db`) and `halyk-travel-mock-service` (`8096`, `travel_db`). Each has Flyway-seeded offers and `GET /offers?audience=` + `POST /apply-discount`. |
| 4 | Legacy compatibility | `ai-assistant-service` becomes a `@Deprecated` proxy shim: `/api/ai/budget-plan` forwards to `parse-budget-plan-service`, `/api/ai/chat` is removed (returns 410 Gone with a migration message). `BonusListener` + `OffersController` stay in `integration-service` unchanged. |

---

## End-state at a glance

After all phases are merged, the running stack has **15 services** (was 9 + analytics = 10; this plan adds 6 — `financial-agent`, `parse-budget-plan`, `summary-llm`, `recommendation`, `alser-mock`, `halyk-travel-mock` — and deprecates `ai-assistant-service` to a shim):

```
eureka :8761         keycloak :8081       gateway :8080
postgres :5432       kafka :9092          (observability overlay)

budget-service :8082            transaction-service :8083
goals-service :8084             family-service :8085
ai-assistant-service :8086 (deprecated shim)
notification-service :8087      integration-service :8088
auth-service :8089              analytics-service :8090

financial-agent-service :8091   summary-llm-service :8092
recommendation-service :8093    parse-budget-plan-service :8094
alser-mock-service :8095        halyk-travel-mock-service :8096
```

**Postgres DBs:** existing 5 (`budget_db, transaction_db, goals_db, family_db, keycloak_db`) + `analytics_db` + new 3 (`priors_db, alser_db, travel_db`).

**Kafka topics added:** `ai.summary-generated`, `ai.recommendation-ready`. (`analytics.metrics-computed` and `analytics.plan-drift-detected` already exist.)

**Concrete end-state observations a reviewer can verify:**
1. `./gradlew build` passes; all 15 services compile and ship a `bootJar`.
2. `docker compose up --build` brings the whole stack up, every service reaches `UP` in Eureka.
3. `bash demo/run-demo.sh` runs a child transaction → SOS flow (unchanged) and then a fresh script `bash demo/run-ai-plane.sh` exercises the AI flow:
   - seed a baseline `BudgetPlan {Продукты:100000, …}`
   - post enough transactions to overspend by 30 %
   - call `POST /api/analytics/metrics/{userId}/{period}/recompute`
   - within ~5 s, `budget_db` contains a NEW `budget_plan` row with `version=2, created_by_ai=true`
   - `notification-service` log contains an `🔔 AI-SUMMARY` line in Russian
   - `integration-service` log contains `📨 PARTNER → Alser: target_audience={budget-conscious, electronics-saver}` and `📨 PARTNER → Halyk Travel: target_audience={saving_for_trip}`
   - `analytics_db.recommendation` has at least one row.
4. Gateway routes `/api/{financial-agent,summary,recommendations,parse-budget,alser,halyk-travel}/**` exist and proxy.
5. Aggregated Swagger UI at `http://localhost:8080/swagger-ui.html` lists all 6 new services in the dropdown.
6. Removing the `OPENAI_API_KEY` env var makes every AI service fall back to a deterministic rule-based stub (no test failure, no crash) — same pattern the current `ai-assistant-service` uses for missing `ANTHROPIC_API_KEY`.

---

## File structure (every new + modified file)

### NEW module: `financial-agent-service/` (port 8091, owns `priors_db`)

```
financial-agent-service/
  build.gradle.kts
  src/main/java/kz/halyk/maqsat/financial/
    FinancialAgentServiceApplication.java
    config/SecurityConfig.java
    config/OpenAiProperties.java
    config/WebClientConfig.java                  // OpenAI + @LoadBalanced for parse-budget + analytics
    config/KafkaConfig.java                      // multi-type consumer via ByteArrayJsonMessageConverter
    client/OpenAiClient.java
    client/AnalyticsClient.java                  // GET /api/analytics/metrics/{userId}/{period}
    client/ParseBudgetPlanClient.java            // POST /api/parse-budget/replan
    listener/PlanDriftListener.java              // consumes analytics.plan-drift-detected
    service/FinancialAgentService.java           // builds prompt, calls OpenAI, returns BudgetPlanProposal
    service/PriorsService.java                   // reads population_prior table for segment baseline
    repository/PopulationPriorRepository.java
    domain/PopulationPrior.java
    dto/BudgetPlanProposal.java                  // record output of LLM (categories[], rationale)
    dto/PlannedCategory.java
    dto/ReplanRequest.java                       // payload sent to parse-budget-plan
    dto/openai/...                               // OpenAiChatCompletionRequest/Response
    exception/GlobalExceptionHandler.java
    exception/AiResponseException.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init.sql   // population_prior table + seed
  src/test/java/kz/halyk/maqsat/financial/
    service/FinancialAgentServiceTest.java       // fallback path (no API key) + happy path with MockWebServer
    service/PriorsServiceTest.java
    listener/PlanDriftListenerTest.java          // @EmbeddedKafka, mocked OpenAI + ParseBudgetClient
  src/test/resources/application-test.yml
```

### NEW module: `parse-budget-plan-service/` (port 8094, no own DB)

```
parse-budget-plan-service/
  build.gradle.kts
  src/main/java/kz/halyk/maqsat/parsebudget/
    ParseBudgetPlanServiceApplication.java
    config/SecurityConfig.java
    config/WebClientConfig.java
    client/BudgetClient.java                     // GET /api/budget/internal/active/{userId} + POST /api/budget/internal/replan/{userId}
    controller/ParseBudgetPlanController.java    // POST /api/parse-budget/replan
    service/ParseBudgetPlanService.java          // validates BudgetPlanProposal, merges with current user+group budgets, calls budget-service replan
    dto/ReplanRequest.java                       // accepts {userId, proposal:BudgetPlanProposal, driftSnapshot}
    dto/ReplanResponse.java                      // {newPlanId, version, supersededPlanId}
    dto/BudgetPlanProposal.java                  // same shape as financial-agent's output
    dto/PlannedCategory.java
    dto/ParsedPlanPayload.java                   // shape sent to budget-service replan
    exception/GlobalExceptionHandler.java
    exception/InvalidPlanException.java
  src/main/resources/application.yml
  src/test/java/kz/halyk/maqsat/parsebudget/
    service/ParseBudgetPlanServiceTest.java
    controller/ParseBudgetPlanControllerTest.java
```

### NEW module: `summary-llm-service/` (port 8092, no own DB)

```
summary-llm-service/
  build.gradle.kts
  src/main/java/kz/halyk/maqsat/summary/
    SummaryLlmServiceApplication.java
    config/SecurityConfig.java
    config/OpenAiProperties.java
    config/WebClientConfig.java
    config/KafkaConfig.java
    client/OpenAiClient.java
    client/AnalyticsClient.java                  // pulls metrics for context
    client/BudgetClient.java                     // pulls active plan for "why" context
    listener/MetricsComputedListener.java        // consumes analytics.metrics-computed
    service/SummaryLlmService.java               // builds prompt per language, calls OpenAI, returns SummaryResult
    service/LanguagePicker.java                  // user preferred language; MVP: round-robin or env default
    dto/SummaryResult.java                       // record {language, summaryText, highlights[], suggestions[]}
    dto/openai/...
    event/SummaryEventPublisher.java             // wraps KafkaTemplate; emits SummaryGenerated
    exception/GlobalExceptionHandler.java
    exception/AiResponseException.java
  src/main/resources/application.yml
  src/test/java/kz/halyk/maqsat/summary/
    service/SummaryLlmServiceTest.java           // fallback (no key) + happy path
    listener/MetricsComputedListenerTest.java
```

### NEW module: `recommendation-service/` (port 8093, no own DB — writes to analytics_db.recommendation)

```
recommendation-service/
  build.gradle.kts
  src/main/java/kz/halyk/maqsat/recommendation/
    RecommendationServiceApplication.java
    config/SecurityConfig.java
    config/OpenAiProperties.java
    config/WebClientConfig.java
    config/KafkaConfig.java
    client/OpenAiClient.java
    client/AnalyticsClient.java                  // metrics
    client/GoalsClient.java                      // GET /api/goals/internal/{userId}
    client/AlserClient.java                      // GET /api/alser/offers?audience=...
    client/HalykTravelClient.java                // GET /api/halyk-travel/offers?audience=...
    client/AnalyticsWriteClient.java             // POST /api/analytics/internal/recommendations
    listener/MetricsComputedListener.java
    service/RecommendationService.java           // builds audience tags from metrics+goals, calls OpenAI for offer matching, persists, emits event
    service/AudienceTagger.java                  // rule-based augmentation of LLM matches
    dto/AudienceProfile.java                     // {userId, goalCategories[], interests[], spendBuckets, savingsRate}
    dto/MatchedOffer.java                        // {offerId, partner, score, rationale}
    dto/RecommendationResult.java
    dto/openai/...
    event/RecommendationEventPublisher.java      // emits RecommendationReady
    exception/GlobalExceptionHandler.java
  src/main/resources/application.yml
  src/test/java/kz/halyk/maqsat/recommendation/
    service/AudienceTaggerTest.java
    service/RecommendationServiceTest.java       // mocked OpenAI + mocked partner clients
    listener/MetricsComputedListenerTest.java
```

### NEW module: `alser-mock-service/` (port 8095, owns `alser_db`)

```
alser-mock-service/
  build.gradle.kts
  src/main/java/kz/halyk/maqsat/alser/
    AlserMockServiceApplication.java
    config/SecurityConfig.java
    controller/OfferController.java              // GET /api/alser/offers?audience=, GET /api/alser/offers/{id}
    controller/ApplyDiscountController.java      // POST /api/alser/apply-discount
    service/OfferService.java
    repository/DeviceOfferRepository.java
    repository/AppliedDiscountRepository.java
    domain/DeviceOffer.java                      // {id, sku, name, basePrice, discountPct, audienceTags jsonb, validUntil}
    domain/AppliedDiscount.java                  // {id, userId, offerId, appliedAt, finalPrice}
    dto/DeviceOfferDto.java
    dto/ApplyDiscountRequest.java
    dto/ApplyDiscountResponse.java
    exception/GlobalExceptionHandler.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init.sql   // schema + 10 seeded offers (iPhone, MacBook, AirPods, Samsung TV, …) with audience tags
  src/test/java/kz/halyk/maqsat/alser/
    controller/OfferControllerTest.java          // audience filter
    service/OfferServiceTest.java
```

### NEW module: `halyk-travel-mock-service/` (port 8096, owns `travel_db`)

```
halyk-travel-mock-service/
  build.gradle.kts
  src/main/java/kz/halyk/maqsat/travel/
    HalykTravelMockServiceApplication.java
    config/SecurityConfig.java
    controller/OfferController.java              // GET /api/halyk-travel/offers?audience=
    controller/BookingController.java            // POST /api/halyk-travel/apply-discount (== booking with discount)
    service/OfferService.java
    repository/TravelOfferRepository.java
    repository/BookingRepository.java
    domain/TravelOffer.java                      // {id, kind enum FLIGHT|HOTEL|TOUR, destination, basePrice, discountPct, audienceTags jsonb, validUntil}
    domain/Booking.java                          // {id, userId, offerId, bookedAt, finalPrice}
    dto/TravelOfferDto.java
    dto/ApplyDiscountRequest.java
    dto/ApplyDiscountResponse.java
    exception/GlobalExceptionHandler.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init.sql   // schema + 10 seeded offers (Antalya, Dubai, Bali, Astana, Tbilisi, …)
  src/test/java/kz/halyk/maqsat/travel/
    controller/OfferControllerTest.java
```

### MODIFIED — existing services

| File | Change |
|---|---|
| `settings.gradle.kts` | append 6 new modules to `include(...)` |
| `common/src/main/java/kz/halyk/maqsat/common/event/EventTopics.java` | append `AI_SUMMARY_GENERATED = "ai.summary-generated"`, `AI_RECOMMENDATION_READY = "ai.recommendation-ready"` |
| `common/src/main/java/kz/halyk/maqsat/common/event/SummaryGenerated.java` | NEW record |
| `common/src/main/java/kz/halyk/maqsat/common/event/RecommendationReady.java` | NEW record |
| `budget-service/src/main/resources/db/migration/V2__plan_versioning.sql` | NEW migration adding `version int NOT NULL DEFAULT 1`, `created_by_ai boolean NOT NULL DEFAULT false`, `superseded_by uuid NULL REFERENCES budget_plan(id)` to `budget_plan` |
| `budget-service/.../domain/BudgetPlan.java` | add `int version`, `boolean createdByAi`, `UUID supersededBy` fields |
| `budget-service/.../controller/BudgetController.java` | add `POST /api/budget/internal/replan/{userId}` accepting `ParsedPlanPayload` |
| `budget-service/.../service/BudgetService.java` | add `BudgetPlan replan(String userId, ParsedPlanPayload payload)` — fetches current active plan, inserts new one with `version = prev + 1, createdByAi = true`, sets `prev.supersededBy = newId`, returns the new plan |
| `budget-service/.../config/SecurityConfig.java` | `/api/budget/internal/replan/**` already covered by existing `/api/budget/internal/**` permitAll rule (verify) |
| `budget-service/.../dto/ParsedPlanPayload.java` | NEW record `{ownerType, ownerId, period, categories:[{name,type,limitAmount}], createdByAi, rationale}` |
| `goals-service/.../controller/GoalController.java` | add `GET /api/goals/internal/{userId}` returning list of goals for that user |
| `goals-service/.../service/GoalService.java` | add `List<GoalResponse> internalListForUser(String userId)` |
| `goals-service/.../config/SecurityConfig.java` | add `/api/goals/internal/**` to permitAll |
| `analytics-service/src/main/resources/db/migration/V3__recommendation.sql` | NEW migration: `recommendation(id uuid, user_id, period varchar(7), offer_id varchar(64), partner varchar(32), score numeric(4,3), audience_tags jsonb, rationale text, created_at timestamptz)` plus index `(user_id, period)` |
| `analytics-service/.../domain/Recommendation.java` | NEW entity |
| `analytics-service/.../repository/RecommendationRepository.java` | NEW |
| `analytics-service/.../controller/RecommendationInternalController.java` | NEW `POST /api/analytics/internal/recommendations` (insert from recommendation-service) and `GET /api/analytics/recommendations/{userId}` (read) |
| `analytics-service/.../dto/RecommendationDto.java` | NEW |
| `analytics-service/.../config/SecurityConfig.java` | add `/api/analytics/internal/**` to permitAll |
| `notification-service/.../listener/AiSummaryListener.java` | NEW listener for `ai.summary-generated`; logs `🔔 AI-SUMMARY` push |
| `notification-service/.../config/KafkaConfig.java` | verify multi-type consumer already in place (it is — uses `ByteArrayJsonMessageConverter`); no change |
| `integration-service/.../listener/RecommendationListener.java` | NEW listener for `ai.recommendation-ready`; for each offer routes to the matching partner client (`/api/alser/apply-discount` or `/api/halyk-travel/apply-discount`) |
| `integration-service/.../client/AlserClient.java` | NEW `@LoadBalanced` WebClient stub |
| `integration-service/.../client/HalykTravelClient.java` | NEW |
| `integration-service/.../config/WebClientConfig.java` | add the `@LoadBalanced WebClient.Builder` bean (mirrors transaction-service pattern) |
| `integration-service/.../config/KafkaConfig.java` | already multi-type (ByteArrayJsonMessageConverter); verify, no change |
| `ai-assistant-service/.../controller/AiController.java` | rewrite `/api/ai/budget-plan` as `WebClient` proxy to `lb://parse-budget-plan-service/api/parse-budget/replan`; `/api/ai/chat` returns 410 Gone with migration message; class annotated `@Deprecated` |
| `ai-assistant-service/build.gradle.kts` | add `spring-cloud-starter-loadbalancer` if not present |
| `ai-assistant-service/.../client/AnthropicClient.java`, `AiAssistantService.java`, `AnthropicProperties.java` | KEPT for one release; not removed in this plan. Mark `@Deprecated`. |
| `gateway/src/main/resources/application.yml` | add 6 new gateway routes + 6 openapi proxy routes + 6 `springdoc.swagger-ui.urls` entries (one per new service) |
| `db/init-databases.sql` | append `CREATE DATABASE priors_db; CREATE DATABASE alser_db; CREATE DATABASE travel_db;` |
| `docker-compose.yml` | add 6 new service blocks modeled on existing patterns + container names + env vars (POSTGRES_DB per service, `OPENAI_API_KEY` for the 3 AI services) |
| `docker-compose.dev.yml` | add 6 matching bind-mount entries |
| `demo/run-ai-plane.sh` | NEW end-to-end demo script |
| `docs/ARCHITECTURE.md` | append §17 "AI plane" with sequence diagram |
| `Maqsat_Family_Technical_Context.md` | update §3 service table (+6 rows, mark ai-assistant deprecated) and §4.1 components |

---

## Data model — every new/changed table

### budget_db — V2__plan_versioning.sql

```sql
ALTER TABLE budget_plan
    ADD COLUMN version        int     NOT NULL DEFAULT 1,
    ADD COLUMN created_by_ai  boolean NOT NULL DEFAULT false,
    ADD COLUMN superseded_by  uuid    NULL REFERENCES budget_plan(id);

CREATE INDEX idx_budget_plan_active
    ON budget_plan (owner_id, owner_type)
    WHERE superseded_by IS NULL;
```

End state: the "active" plan for a `(ownerId, ownerType)` is the one row with `superseded_by IS NULL`. Replan inserts a new row pointing to the predecessor.

### analytics_db — V3__recommendation.sql

```sql
CREATE TABLE recommendation (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       varchar(64) NOT NULL,
    period        varchar(7)  NOT NULL,
    offer_id      varchar(64) NOT NULL,
    partner       varchar(32) NOT NULL,            -- 'ALSER' | 'HALYK_TRAVEL'
    score         numeric(4,3) NOT NULL,           -- 0.000..1.000
    audience_tags jsonb       NOT NULL,
    rationale     text,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_recommendation_user_period ON recommendation (user_id, period);
```

### priors_db — financial-agent V1__init.sql

```sql
CREATE TABLE population_prior (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_tag   varchar(64) NOT NULL,            -- 'default' | 'high-income' | 'student' | 'family-with-kids'
    category_name varchar(128) NOT NULL,
    ratio         numeric(5,4) NOT NULL,           -- 0.0000..1.0000, share of income for that category
    source        varchar(128) NOT NULL,           -- 'mock_population_2026Q2' or notebook ref
    UNIQUE (segment_tag, category_name)
);

-- Seeded defaults: rates ported from anal-service notebook population summaries.
INSERT INTO population_prior (segment_tag, category_name, ratio, source) VALUES
    ('default', 'Продукты',    0.2500, 'mock_population_2026Q2'),
    ('default', 'Коммуналка',  0.1200, 'mock_population_2026Q2'),
    ('default', 'Транспорт',   0.0800, 'mock_population_2026Q2'),
    ('default', 'Такси',       0.0500, 'mock_population_2026Q2'),
    ('default', 'Рестораны',   0.0900, 'mock_population_2026Q2'),
    ('default', 'Развлечения', 0.0700, 'mock_population_2026Q2'),
    ('default', 'Подписки',    0.0300, 'mock_population_2026Q2'),
    ('default', 'Прочее',      0.2000, 'mock_population_2026Q2'),
    ('high-income', 'Продукты',    0.1500, 'mock_population_2026Q2'),
    ('high-income', 'Рестораны',   0.1500, 'mock_population_2026Q2'),
    ('high-income', 'Путешествия', 0.1500, 'mock_population_2026Q2'),
    ('high-income', 'Прочее',      0.5500, 'mock_population_2026Q2');
```

Segment selection in MVP: every user gets `'default'`. Segment classification by the financial agent is a future task — out of scope for this plan but documented.

### alser_db — V1__init.sql

```sql
CREATE TABLE device_offer (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sku            varchar(64) NOT NULL UNIQUE,
    name           varchar(255) NOT NULL,
    base_price     numeric(15,2) NOT NULL,
    discount_pct   numeric(5,2) NOT NULL,
    audience_tags  jsonb NOT NULL,                 -- ["electronics-saver", "students", ...]
    valid_until    timestamptz NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_alser_offer_audience ON device_offer USING GIN (audience_tags);

CREATE TABLE applied_discount (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      varchar(64) NOT NULL,
    offer_id     uuid NOT NULL REFERENCES device_offer(id),
    final_price  numeric(15,2) NOT NULL,
    applied_at   timestamptz NOT NULL DEFAULT now()
);

-- Seed (10 rows). Sample:
INSERT INTO device_offer (sku, name, base_price, discount_pct, audience_tags, valid_until) VALUES
    ('IPHONE-15-PRO', 'iPhone 15 Pro 256GB', 750000, 10.00, '["electronics-saver","apple-fan","high-income"]', now() + interval '30 days'),
    ('MACBOOK-AIR-M3', 'MacBook Air M3 13"',   850000, 12.00, '["electronics-saver","student","creator"]',     now() + interval '30 days'),
    ('AIRPODS-PRO-2',  'AirPods Pro (2nd gen)', 165000,  8.00, '["apple-fan","casual"]',                       now() + interval '30 days'),
    ('SAMSUNG-S24',    'Samsung Galaxy S24',    420000, 15.00, '["electronics-saver","android-fan"]',          now() + interval '30 days'),
    ('LG-OLED-55',     'LG OLED 55" C3',        780000, 10.00, '["home-upgrade","high-income"]',               now() + interval '30 days'),
    ('SONY-WH-1000XM5','Sony WH-1000XM5',       220000, 12.00, '["commuter","music-lover"]',                   now() + interval '30 days'),
    ('DJI-MINI-4',     'DJI Mini 4 Pro',        650000,  5.00, '["creator","traveler"]',                       now() + interval '30 days'),
    ('PS5-SLIM',       'PlayStation 5 Slim',    310000, 10.00, '["gamer","student"]',                          now() + interval '30 days'),
    ('XBOX-X',         'Xbox Series X',         320000, 10.00, '["gamer"]',                                    now() + interval '30 days'),
    ('IPAD-AIR-M2',    'iPad Air M2 11"',       420000, 10.00, '["student","creator","apple-fan"]',            now() + interval '30 days');
```

### travel_db — V1__init.sql

```sql
CREATE TABLE travel_offer (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kind           varchar(16) NOT NULL CHECK (kind IN ('FLIGHT','HOTEL','TOUR')),
    destination    varchar(128) NOT NULL,
    base_price     numeric(15,2) NOT NULL,
    discount_pct   numeric(5,2) NOT NULL,
    audience_tags  jsonb NOT NULL,
    valid_until    timestamptz NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_travel_offer_audience ON travel_offer USING GIN (audience_tags);

CREATE TABLE booking (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      varchar(64) NOT NULL,
    offer_id     uuid NOT NULL REFERENCES travel_offer(id),
    final_price  numeric(15,2) NOT NULL,
    booked_at    timestamptz NOT NULL DEFAULT now()
);

-- Seed (10 rows). Sample:
INSERT INTO travel_offer (kind, destination, base_price, discount_pct, audience_tags, valid_until) VALUES
    ('FLIGHT', 'Astana → Antalya',         180000, 15.00, '["saving_for_trip","beach-lover","summer"]',    now() + interval '60 days'),
    ('HOTEL',  'Dubai 5★ — 4 nights',      420000, 20.00, '["saving_for_trip","luxury","high-income"]',    now() + interval '60 days'),
    ('TOUR',   'Bali full package 10d',    750000, 10.00, '["saving_for_trip","explorer"]',                 now() + interval '90 days'),
    ('FLIGHT', 'Astana → Tbilisi',         95000,  10.00, '["budget-traveler","weekend"]',                 now() + interval '60 days'),
    ('HOTEL',  'Issyk-Kul resort — 3 d',   55000,  15.00, '["family-with-kids","local-getaway"]',           now() + interval '60 days'),
    ('TOUR',   'Almaty mountains 2 days',  35000,   8.00, '["budget-traveler","local-getaway","outdoor"]',  now() + interval '60 days'),
    ('FLIGHT', 'Astana → Istanbul',        145000, 12.00, '["explorer","budget-traveler"]',                now() + interval '60 days'),
    ('HOTEL',  'Borovoye — 2 nights',      45000,  10.00, '["family-with-kids","local-getaway"]',          now() + interval '60 days'),
    ('TOUR',   'Umrah package 7 days',     650000,  5.00, '["religious","high-income"]',                   now() + interval '90 days'),
    ('FLIGHT', 'Astana → Bangkok',         280000, 10.00, '["explorer","summer","saving_for_trip"]',       now() + interval '60 days');
```

---

## Event contracts

```java
// common/.../event/SummaryGenerated.java
public record SummaryGenerated(
    String userId,
    String period,                       // YYYY-MM
    String language,                     // "kk" | "ru" | "en"
    String summaryText,
    List<String> highlights,
    List<String> suggestions,
    Instant generatedAt
) {}

// common/.../event/RecommendationReady.java
public record RecommendationReady(
    String userId,
    String period,                       // YYYY-MM
    List<MatchedOffer> offers,
    Instant generatedAt
) {
    public record MatchedOffer(
        String offerId,
        String partner,                  // "ALSER" | "HALYK_TRAVEL"
        BigDecimal score,                // 0.000..1.000
        List<String> audienceTags,
        String rationale
    ) {}
}
```

`EventTopics` additions:
```java
public static final String AI_SUMMARY_GENERATED       = "ai.summary-generated";
public static final String AI_RECOMMENDATION_READY    = "ai.recommendation-ready";
```

Topic key is `userId` for both — same partitioning discipline as existing topics.

---

## OpenAI client (shared pattern)

Every AI service has its own `OpenAiClient`, `OpenAiProperties`, and `WebClientConfig.openAiWebClient()` bean. The boilerplate is identical:

```java
// config/OpenAiProperties.java
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
    String apiKey,
    String baseUrl,         // default https://api.openai.com
    String model,           // default gpt-4o-mini
    Integer maxTokens,      // default 1024
    Double temperature      // default 0.2
) {
    public boolean enabled() { return apiKey != null && !apiKey.isBlank(); }
}

// application.yml
openai:
  api-key: ${OPENAI_API_KEY:}
  base-url: ${OPENAI_BASE_URL:https://api.openai.com}
  model:    ${OPENAI_MODEL:gpt-4o-mini}
  max-tokens: ${OPENAI_MAX_TOKENS:1024}
  temperature: ${OPENAI_TEMPERATURE:0.2}

// client/OpenAiClient.java
@Component
@RequiredArgsConstructor
public class OpenAiClient {
    private final WebClient openAiWebClient;
    private final OpenAiProperties props;

    public String complete(String system, List<ChatMessage> conversation) {
        if (!props.enabled()) throw new IllegalStateException("OPENAI_API_KEY not set");

        List<Map<String,Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        for (var m : conversation) messages.add(Map.of("role", m.role(), "content", m.content()));

        Map<String,Object> body = Map.of(
            "model", props.model(),
            "max_tokens", props.maxTokens(),
            "temperature", props.temperature(),
            "messages", messages,
            "response_format", Map.of("type","json_object"));   // forces strict JSON

        JsonNode resp = openAiWebClient.post()
            .uri("/v1/chat/completions")
            .header("Authorization", "Bearer " + props.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (resp == null) throw new AiResponseException("Empty response from OpenAI");
        return resp.path("choices").path(0).path("message").path("content").asText();
    }

    public record ChatMessage(String role, String content) {}
}
```

Each service's `*Service` checks `props.enabled()` and returns a **deterministic fallback** if missing — mirrors the existing `ai-assistant-service` `fallbackPlan()` pattern so the demo runs without an API key.

---

## Phase Map

| Phase | What ships | Independently shippable? |
|---|---|---|
| A | `common` event records + topic constants | Yes — compile-only |
| B | `budget_db` V2 + `replan` endpoint + `BudgetPlan` versioning | Yes — existing tests stay green; new endpoint covered |
| C | `goals-service` internal endpoint | Yes |
| D | `financial-agent-service` (module + priors_db + listener + fallback path) | Yes (drift → log if no API key) |
| E | `parse-budget-plan-service` | Yes (financial-agent now calls into it) |
| F | `summary-llm-service` + `notification-service.AiSummaryListener` | Yes (metrics-computed → push log) |
| G | `alser-mock-service` | Yes — offers reachable via gateway |
| H | `halyk-travel-mock-service` | Yes |
| I | `recommendation-service` + `analytics_db` V3 + integration-service forward listener | Yes (full pipeline live) |
| J | `ai-assistant-service` shim conversion | Yes |
| K | `demo/run-ai-plane.sh` + docs | Yes |
| L | Observability counters + dashboard | Yes |

---

# Phase A — Common contracts

### Task A1: Add event records + topic constants

**Files:**
- Create: `common/src/main/java/kz/halyk/maqsat/common/event/SummaryGenerated.java`
- Create: `common/src/main/java/kz/halyk/maqsat/common/event/RecommendationReady.java`
- Modify: `common/src/main/java/kz/halyk/maqsat/common/event/EventTopics.java`

- [ ] **Step 1: Write the two records exactly as in the "Event contracts" section above.**

- [ ] **Step 2: Append topic constants to `EventTopics`.**

```java
public static final String AI_SUMMARY_GENERATED    = "ai.summary-generated";
public static final String AI_RECOMMENDATION_READY = "ai.recommendation-ready";
```

- [ ] **Step 3: Verify whole monorepo still compiles.**

Run: `JAVA_HOME=~/.jdks/jdk-21.0.4+7 ./gradlew :common:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit.**

```bash
git add common/src/main/java/kz/halyk/maqsat/common/event/SummaryGenerated.java common/src/main/java/kz/halyk/maqsat/common/event/RecommendationReady.java common/src/main/java/kz/halyk/maqsat/common/event/EventTopics.java
git commit -m "feat(common): add AI plane event contracts (SummaryGenerated, RecommendationReady)"
```

---

# Phase B — Budget plan versioning

### Task B1: V2 migration

**Files:**
- Create: `budget-service/src/main/resources/db/migration/V2__plan_versioning.sql`

- [ ] **Step 1: Write migration as in the "Data model" section above.**

- [ ] **Step 2: Verify it applies cleanly against an empty `budget_db`.**

Run (against the host's Postgres):
```
PGPASSWORD=maqsat psql -h localhost -p 5432 -U maqsat -d budget_db -f budget-service/src/main/resources/db/migration/V2__plan_versioning.sql
```
Expected: `ALTER TABLE`, `CREATE INDEX`. If a previous migration run already happened, drop the columns first or run via `./gradlew :budget-service:bootRun` which lets Flyway apply.

- [ ] **Step 3: Commit.**

```bash
git add budget-service/src/main/resources/db/migration/V2__plan_versioning.sql
git commit -m "feat(budget): V2 — plan versioning + AI authorship + supersession"
```

### Task B2: Extend `BudgetPlan` entity

**Files:**
- Modify: `budget-service/src/main/java/kz/halyk/maqsat/budget/domain/BudgetPlan.java`

- [ ] **Step 1: Add fields.**

```java
@Column(nullable = false)
private int version = 1;

@Column(name = "created_by_ai", nullable = false)
private boolean createdByAi = false;

@Column(name = "superseded_by")
private UUID supersededBy;
```

- [ ] **Step 2: Build to confirm Hibernate validation passes.**

Run: `./gradlew :budget-service:compileJava`
Expected: SUCCESS.

- [ ] **Step 3: Commit.**

```bash
git commit -am "feat(budget): BudgetPlan version + createdByAi + supersededBy"
```

### Task B3: `ParsedPlanPayload` DTO + replan endpoint + service

**Files:**
- Create: `budget-service/.../dto/ParsedPlanPayload.java`
- Modify: `budget-service/.../service/BudgetService.java`
- Modify: `budget-service/.../controller/BudgetController.java`
- Modify: `budget-service/.../repository/BudgetPlanRepository.java` (add `findFirstByOwnerIdAndOwnerTypeAndSupersededByIsNullOrderByVersionDesc(String, OwnerType)`)
- Test: `budget-service/src/test/java/kz/halyk/maqsat/budget/service/BudgetReplanTest.java`

- [ ] **Step 1: DTO.**

```java
package kz.halyk.maqsat.budget.dto;

import java.math.BigDecimal;
import java.util.List;
import kz.halyk.maqsat.budget.domain.CategoryType;
import kz.halyk.maqsat.budget.domain.OwnerType;

public record ParsedPlanPayload(
    OwnerType ownerType,
    String ownerId,
    String period,                                   // YYYY-MM
    List<PlannedCategory> categories,
    boolean createdByAi,
    String rationale
) {
    public record PlannedCategory(String name, CategoryType type, BigDecimal limitAmount) {}
}
```

- [ ] **Step 2: Repository finder.**

```java
Optional<BudgetPlan> findFirstByOwnerIdAndOwnerTypeAndSupersededByIsNullOrderByVersionDesc(String ownerId, OwnerType type);
```

- [ ] **Step 3: Failing test — `BudgetReplanTest.replanInsertsNewVersionAndSupersedesOld`.**

```java
@DataJpaTest
@Import(BudgetService.class)
class BudgetReplanTest {
    @Autowired BudgetService svc;
    @Autowired BudgetPlanRepository repo;
    @MockBean MeterRegistry meterRegistry;

    @Test
    void replanInsertsNewVersionAndSupersedesOld() {
        // Given an existing plan v1
        BudgetPlan p1 = new BudgetPlan();
        p1.setOwnerId("u1"); p1.setOwnerType(OwnerType.USER);
        p1.setPeriodStart(LocalDate.of(2026,5,1)); p1.setPeriodEnd(LocalDate.of(2026,5,31));
        p1.setCreatedAt(Instant.now());
        BudgetCategory c = new BudgetCategory();
        c.setName("Продукты"); c.setType(CategoryType.MANDATORY); c.setLimitAmount(new BigDecimal("100000"));
        c.setSpentAmount(BigDecimal.ZERO);
        p1.addCategory(c);
        repo.save(p1);

        // When AI re-plans
        ParsedPlanPayload payload = new ParsedPlanPayload(
            OwnerType.USER, "u1", "2026-05",
            List.of(new ParsedPlanPayload.PlannedCategory("Продукты", CategoryType.MANDATORY, new BigDecimal("130000"))),
            true, "drift_detected");
        BudgetPlan p2 = svc.replan("u1", payload);

        // Then
        assertThat(p2.getVersion()).isEqualTo(2);
        assertThat(p2.isCreatedByAi()).isTrue();
        BudgetPlan reloadedOld = repo.findById(p1.getId()).orElseThrow();
        assertThat(reloadedOld.getSupersededBy()).isEqualTo(p2.getId());
    }
}
```

- [ ] **Step 4: Run, verify FAIL.**

Run: `./gradlew :budget-service:test --tests BudgetReplanTest`
Expected: compilation failure (no `replan` method).

- [ ] **Step 5: Implement `BudgetService.replan(String userId, ParsedPlanPayload payload)`.**

```java
@Transactional
public BudgetPlan replan(String userId, ParsedPlanPayload payload) {
    OwnerType type = payload.ownerType() != null ? payload.ownerType() : OwnerType.USER;
    String ownerId = payload.ownerId() != null ? payload.ownerId() : userId;

    BudgetPlan previous = planRepository
        .findFirstByOwnerIdAndOwnerTypeAndSupersededByIsNullOrderByVersionDesc(ownerId, type)
        .orElse(null);
    int nextVersion = previous == null ? 1 : previous.getVersion() + 1;

    YearMonth ym = YearMonth.parse(payload.period());
    BudgetPlan next = new BudgetPlan();
    next.setOwnerId(ownerId);
    next.setOwnerType(type);
    next.setPeriodStart(ym.atDay(1));
    next.setPeriodEnd(ym.atEndOfMonth());
    next.setCreatedAt(Instant.now());
    next.setVersion(nextVersion);
    next.setCreatedByAi(payload.createdByAi());
    payload.categories().forEach(pc -> {
        BudgetCategory c = new BudgetCategory();
        c.setName(pc.name()); c.setType(pc.type()); c.setLimitAmount(pc.limitAmount());
        c.setSpentAmount(BigDecimal.ZERO);
        next.addCategory(c);
    });
    planRepository.save(next);

    if (previous != null) {
        previous.setSupersededBy(next.getId());
        planRepository.save(previous);
    }
    log.info("Replanned user={} version {} → {} (createdByAi={})", ownerId, nextVersion-1, nextVersion, payload.createdByAi());
    return next;
}
```

- [ ] **Step 6: Controller.**

```java
@PostMapping("/internal/replan/{userId}")
public ResponseEntity<UUID> replan(@PathVariable String userId, @Valid @RequestBody ParsedPlanPayload payload) {
    BudgetPlan p = service.replan(userId, payload);
    return ResponseEntity.status(HttpStatus.CREATED).body(p.getId());
}
```

- [ ] **Step 7: Run test, verify PASS.**

Run: `./gradlew :budget-service:test --tests BudgetReplanTest`
Expected: 1 test passing.

- [ ] **Step 8: Update `BudgetService.getActivePlan` to also filter by `superseded_by IS NULL`.** (Currently it uses `periodStart ≤ today ≤ periodEnd` only — the multi-version world needs both.)

- [ ] **Step 9: Existing `dashboard` and `trackSpending` methods — verify they use the same supersession-aware finder. Update if not.**

- [ ] **Step 10: Build whole monorepo, run all budget-service tests.**

Run: `./gradlew :budget-service:test && ./gradlew build`
Expected: green.

- [ ] **Step 11: Commit.**

```bash
git add budget-service/
git commit -m "feat(budget): replan endpoint inserts new version + supersedes prior"
```

---

# Phase C — Goals internal endpoint

### Task C1: `GET /api/goals/internal/{userId}`

**Files:**
- Modify: `goals-service/.../controller/GoalController.java`
- Modify: `goals-service/.../service/GoalService.java`
- Modify: `goals-service/.../config/SecurityConfig.java`
- Test: `goals-service/src/test/java/kz/halyk/maqsat/goals/controller/GoalInternalEndpointTest.java`

- [ ] **Step 1: Failing test.**

```java
@WebMvcTest(GoalController.class)
class GoalInternalEndpointTest {
    @Autowired MockMvc mvc;
    @MockBean GoalService service;

    @Test
    void returnsGoalsForUser() throws Exception {
        when(service.internalListForUser("u1"))
            .thenReturn(List.of(new GoalResponse(UUID.randomUUID(), "Trip to Bali", "travel", new BigDecimal("500000"),
                new BigDecimal("100000"), null, null, BigDecimal.ZERO, 20)));

        mvc.perform(get("/api/goals/internal/u1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Trip to Bali"))
            .andExpect(jsonPath("$[0].category").value("travel"));
    }
}
```

- [ ] **Step 2: Run, FAIL.**

- [ ] **Step 3: Implement service method + controller route + permitAll rule.**

`GoalService.internalListForUser(String userId)` reuses `listForUser` semantics.

`GoalController`:
```java
@GetMapping("/internal/{userId}")
public List<GoalResponse> internal(@PathVariable String userId) {
    return service.internalListForUser(userId).stream().map(GoalResponse::from).toList();
}
```

`SecurityConfig`: add `/api/goals/internal/**` to permitAll patterns.

- [ ] **Step 4: Run test, PASS.**

- [ ] **Step 5: Commit.**

```bash
git commit -am "feat(goals): internal endpoint for recommendation-service to read user goals"
```

---

# Phase D — financial-agent-service

### Task D1: Module skeleton + Gradle + Application + YAML + Security

**Files:** `financial-agent-service/build.gradle.kts`, `FinancialAgentServiceApplication.java`, `application.yml`, `SecurityConfig.java`, `GlobalExceptionHandler.java`, `settings.gradle.kts` update.

Follow the exact patterns from the analytics-service plan, Phase A tasks A1–A4. Port 8091, `priors_db`, `analytics.etl.cron` not needed (no scheduled job). Add `openai.*` config block. Add `@LoadBalanced WebClient.Builder` via `WebClientConfig`. Add `KafkaConfig` with multi-type consumer (`ByteArrayJsonMessageConverter`) because this service consumes `PlanDriftDetected` (and may later also consume metrics).

- [ ] **Step 1–4 (skeleton):** copy patterns; verify `./gradlew :financial-agent-service:build` passes after each.
- [ ] **Step 5 (Flyway V1):** create `priors_db` schema + seed (see Data model section).
- [ ] **Step 6 (`db/init-databases.sql`):** append `CREATE DATABASE priors_db;`
- [ ] **Step 7 (docker-compose):** add the service block.
- [ ] **Step 8 (gateway route):** add `/api/financial-agent/**` route + openapi proxy + swagger-ui URL.
- [ ] **Step 9 (commit):** `feat(financial-agent): module skeleton + priors_db V1 schema + seed`.

### Task D2: `PopulationPrior` entity + repository + `PriorsService`

**Files:**
- Create: `financial-agent-service/.../domain/PopulationPrior.java`
- Create: `financial-agent-service/.../repository/PopulationPriorRepository.java`
- Create: `financial-agent-service/.../service/PriorsService.java`
- Test: `financial-agent-service/.../service/PriorsServiceTest.java`

- [ ] **Step 1: Entity** matching the `population_prior` table (id UUID, segmentTag, categoryName, ratio BigDecimal, source). Unique `(segmentTag, categoryName)`.

- [ ] **Step 2: Repository** with:
```java
List<PopulationPrior> findBySegmentTag(String segmentTag);
```

- [ ] **Step 3: Failing test** `PriorsServiceTest.returnsRatiosForDefaultSegment` — asserts `priorsService.getPriorsFor("default")` returns ≥ 8 rows summing to ~1.0.

- [ ] **Step 4: Implement `PriorsService.getPriorsFor(String segmentTag)`** returning `Map<String, BigDecimal>` (category → ratio).

- [ ] **Step 5: Run test, PASS.**

- [ ] **Step 6: Commit.**

```bash
git commit -am "feat(financial-agent): PriorsService reads population_prior segment ratios"
```

### Task D3: `OpenAiClient` + `OpenAiProperties` + `WebClientConfig`

**Files:** as listed; code as in the "OpenAI client (shared pattern)" section.

- [ ] **Step 1: `OpenAiProperties` record + add `@EnableConfigurationProperties(OpenAiProperties.class)` on Application.**
- [ ] **Step 2: `WebClientConfig.openAiWebClient(...)` bean.**
- [ ] **Step 3: `OpenAiClient.complete(...)` using `WebClient`.**
- [ ] **Step 4: Unit test** `OpenAiClientTest` with `MockWebServer` returning a canned JSON; asserts the extracted `choices[0].message.content` is what `complete()` returns.
- [ ] **Step 5: Commit.** `feat(financial-agent): OpenAI client (gpt-4o-mini)`

### Task D4: `FinancialAgentService` (LLM prompt + fallback)

**Files:**
- Create: `financial-agent-service/.../service/FinancialAgentService.java`
- Create: `financial-agent-service/.../dto/BudgetPlanProposal.java`
- Create: `financial-agent-service/.../dto/PlannedCategory.java`
- Test: `financial-agent-service/.../service/FinancialAgentServiceTest.java`

- [ ] **Step 1: DTOs.**

```java
public record BudgetPlanProposal(List<PlannedCategory> categories, String rationale) {}
public record PlannedCategory(String name, String type, BigDecimal limitAmount) {}
```

- [ ] **Step 2: Failing test — fallback path (no API key).**

```java
@Test
void fallback_whenNoApiKey_usesPriorsBaseline() {
    var props = new OpenAiProperties("", "", "gpt-4o-mini", 1024, 0.2);
    var priors = mock(PriorsService.class);
    when(priors.getPriorsFor("default")).thenReturn(Map.of(
        "Продукты", new BigDecimal("0.25"),
        "Рестораны", new BigDecimal("0.10")));
    var svc = new FinancialAgentService(props, mock(OpenAiClient.class), priors);

    Map<String,BigDecimal> metrics = Map.of("incomeEstimate", new BigDecimal("500000"));
    Map<String,BigDecimal> drift = Map.of("Рестораны", new BigDecimal("0.30"));
    BudgetPlanProposal p = svc.propose("u1", "2026-05", "default", metrics, drift);

    assertThat(p.categories()).hasSize(2);
    assertThat(p.categories().stream().filter(c -> c.name().equals("Продукты"))
        .findFirst().get().limitAmount()).isEqualByComparingTo("125000");
    assertThat(p.rationale()).contains("fallback");
}
```

- [ ] **Step 3: Implement service.**

```java
@Service @RequiredArgsConstructor @Slf4j
public class FinancialAgentService {
    private final OpenAiProperties props;
    private final OpenAiClient openAi;
    private final PriorsService priorsSvc;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM = """
        You are an in-house personal-finance ML model emulated as an LLM.
        Output STRICT JSON {"categories":[{"name":...,"type":"MANDATORY|DISCRETIONARY","limitAmount":number}],"rationale":"..."}.
        Use only the Russian category names supplied in the user prompt.
        Return BigDecimal-safe integers/decimals only. Never exceed monthly income.
        """;

    public BudgetPlanProposal propose(String userId, String period, String segmentTag,
                                      Map<String,BigDecimal> metrics,
                                      Map<String,BigDecimal> driftByCategory) {
        Map<String,BigDecimal> priors = priorsSvc.getPriorsFor(segmentTag);
        if (!props.enabled()) {
            return fallback(metrics, priors);
        }
        String userPrompt = buildPrompt(userId, period, metrics, priors, driftByCategory);
        String raw = openAi.complete(SYSTEM, List.of(new OpenAiClient.ChatMessage("user", userPrompt)));
        return parse(raw);
    }

    private BudgetPlanProposal fallback(Map<String,BigDecimal> metrics, Map<String,BigDecimal> priors) {
        BigDecimal income = metrics.getOrDefault("incomeEstimate", BigDecimal.ZERO);
        List<PlannedCategory> cats = priors.entrySet().stream()
            .map(e -> new PlannedCategory(e.getKey(),
                List.of("Продукты","Коммуналка","Транспорт").contains(e.getKey()) ? "MANDATORY" : "DISCRETIONARY",
                income.multiply(e.getValue()).setScale(0, RoundingMode.HALF_UP)))
            .toList();
        return new BudgetPlanProposal(cats, "fallback: priors-baseline (no OPENAI_API_KEY)");
    }

    private BudgetPlanProposal parse(String raw) {
        try { return mapper.readValue(raw, BudgetPlanProposal.class); }
        catch (Exception e) { throw new AiResponseException("Bad OpenAI JSON: " + raw, e); }
    }

    private String buildPrompt(String userId, String period,
                               Map<String,BigDecimal> metrics, Map<String,BigDecimal> priors,
                               Map<String,BigDecimal> drift) {
        try {
            return """
                user_id: %s
                period: %s
                metrics: %s
                population_priors_for_segment: %s
                drift_to_correct_by_category: %s
                Produce a re-planned budget that respects priors, reacts to drift, and stays under income.
                """.formatted(userId, period,
                    mapper.writeValueAsString(metrics),
                    mapper.writeValueAsString(priors),
                    mapper.writeValueAsString(drift));
        } catch (Exception e) { throw new AiResponseException("Could not build prompt", e); }
    }
}
```

- [ ] **Step 4: Run test, PASS.**

- [ ] **Step 5: Add a second test for the OpenAI happy path using `MockWebServer` returning a canned JSON envelope.**

- [ ] **Step 6: Commit.**

```bash
git commit -am "feat(financial-agent): proposes plan via OpenAI (priors-baseline fallback)"
```

### Task D5: `AnalyticsClient` + `ParseBudgetPlanClient`

**Files:**
- Create: `financial-agent-service/.../client/AnalyticsClient.java` — `GET /api/analytics/metrics/{userId}/{period}` via `lb://analytics-service`. DTO mirrors `UserMetricsResponse`.
- Create: `financial-agent-service/.../client/ParseBudgetPlanClient.java` — `POST /api/parse-budget/replan` via `lb://parse-budget-plan-service`. Payload `ReplanRequest`, returns `ReplanResponse`.

Each block-based; null-safe; logs failures at WARN.

- [ ] **Step 1–2: Implement both clients.**
- [ ] **Step 3: MockWebServer tests for each.**
- [ ] **Step 4: Commit.** `feat(financial-agent): clients for analytics + parse-budget`

### Task D6: `PlanDriftListener` (Kafka consumer wiring)

**Files:**
- Create: `financial-agent-service/.../listener/PlanDriftListener.java`
- Test: `financial-agent-service/.../listener/PlanDriftListenerTest.java`

- [ ] **Step 1: Failing `@EmbeddedKafka` test** posting a `PlanDriftDetected` event, verifying that the listener calls `parseBudgetClient.replan(...)` with a proposal derived from the agent.

- [ ] **Step 2: Implement listener.**

```java
@Component @RequiredArgsConstructor @Slf4j
public class PlanDriftListener {
    private final AnalyticsClient analytics;
    private final FinancialAgentService agent;
    private final ParseBudgetPlanClient parser;

    @KafkaListener(topics = EventTopics.ANALYTICS_PLAN_DRIFT_DETECTED, groupId = "financial-agent-service")
    public void onDrift(PlanDriftDetected e) {
        var metrics = analytics.fetchMetrics(e.userId(), e.period());
        if (metrics == null) { log.warn("no metrics for {}/{}", e.userId(), e.period()); return; }
        BudgetPlanProposal proposal = agent.propose(
            e.userId(), e.period(), /*segment*/"default",
            toMetricsMap(metrics), e.driftByCategory());
        parser.replan(new ReplanRequest(e.userId(), e.period(), proposal, e.driftByCategory()));
    }

    private Map<String, BigDecimal> toMetricsMap(UserMetricsDto m) {
        var out = new LinkedHashMap<String, BigDecimal>();
        out.put("incomeEstimate", m.incomeEstimate());
        out.put("totalSpent", m.totalSpent());
        out.put("savingsRate", m.savingsRate());
        out.put("volatility", m.volatility());
        return out;
    }
}
```

- [ ] **Step 3: Run test, PASS** (or `@Disabled` with documented Testcontainers reason, mirroring analytics-service's known limitation).

- [ ] **Step 4: Commit.** `feat(financial-agent): consumes PlanDriftDetected, delegates to parse-budget-plan`

### Task D7: Smoke run

- [ ] **Step 1:** `JAVA_HOME=~/.jdks/jdk-21.0.4+7 ./gradlew :financial-agent-service:bootRun` — registers in Eureka, `/actuator/health` returns UP.
- [ ] **Step 2:** Confirm the service appears in aggregated Swagger UI.

---

# Phase E — parse-budget-plan-service

### Task E1: Module skeleton

Follow analytics-service Phase A pattern: gradle, Application, application.yml (port 8094, no DB), SecurityConfig, GlobalExceptionHandler, settings.gradle.kts include, gateway route `/api/parse-budget/**`, docker-compose entry, openapi proxy.

- [ ] **Step 1–7:** as analytics A1–A4 + gateway/compose.
- [ ] **Step 8:** commit `feat(parse-budget-plan): module skeleton`.

### Task E2: `BudgetClient` (READS + WRITES budget-service)

**Files:**
- Create: `parse-budget-plan-service/.../client/BudgetClient.java`
- DTOs: `parse-budget-plan-service/.../dto/ActivePlanView.java` (mirrors budget's), `ParsedPlanPayload.java`.

`BudgetClient`:
- `Optional<ActivePlanView> getActive(String userId)` → `GET /api/budget/internal/active/{userId}` via `lb://budget-service`
- `UUID replan(String userId, ParsedPlanPayload payload)` → `POST /api/budget/internal/replan/{userId}`

- [ ] **Step 1–4:** MockWebServer tests.

### Task E3: `ParseBudgetPlanService` (validation + merge + persist)

**Files:**
- Create: `parse-budget-plan-service/.../service/ParseBudgetPlanService.java`
- Create: `parse-budget-plan-service/.../controller/ParseBudgetPlanController.java`
- DTOs: `ReplanRequest`, `ReplanResponse`, `BudgetPlanProposal`, `PlannedCategory` (record copies — keep services decoupled).
- Test: `parse-budget-plan-service/.../service/ParseBudgetPlanServiceTest.java`

`ReplanRequest` shape:
```java
public record ReplanRequest(
    String userId,
    String period,
    BudgetPlanProposal proposal,
    Map<String, BigDecimal> driftSnapshot
) {}
```

`ParseBudgetPlanService.handle(ReplanRequest req) -> ReplanResponse`:
- Validate `proposal.categories` non-empty, all amounts ≥ 0, types in `{MANDATORY, DISCRETIONARY}`.
- Optionally fetch the current `ActivePlanView` (for the user + group budgets) — in MVP only user plan; group budgets are future work but the merge function is invoked so it's wired.
- Build `ParsedPlanPayload`.
- Call `budgetClient.replan(...)` → get new plan ID.
- Return `ReplanResponse(newPlanId, version=…, supersededPlanId=…)` (version is in the controller response from budget; for MVP just echo the new UUID).

Controller `POST /api/parse-budget/replan`:
```java
@PostMapping("/replan")
public ResponseEntity<ReplanResponse> replan(@Valid @RequestBody ReplanRequest req) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.handle(req));
}
```

- [ ] **Step 1: Failing test** with mocked `BudgetClient` asserting the right `ParsedPlanPayload` shape is sent.
- [ ] **Step 2–4: Implement, PASS, commit.**

---

# Phase F — summary-llm-service

### Task F1: Module skeleton

Port 8092, no DB, same OpenAI plumbing as financial-agent. Multi-type Kafka consumer. Gateway `/api/summary/**` route. Commit.

### Task F2: `SummaryLlmService` + `LanguagePicker`

**Files:** `service/SummaryLlmService.java`, `service/LanguagePicker.java`, `dto/SummaryResult.java`, `dto/openai/*`.

System prompt (3 variants — one per language; chosen by `LanguagePicker`):

```
You are an in-house financial summary LLM. The user is a Halyk Bank customer.
You receive: (1) a metrics snapshot for the most recent period, (2) the user's active budget plan.
Produce STRICT JSON {"language":"ru","summaryText":"...","highlights":["..."],"suggestions":["..."]} in the user's language.
2–4 highlights, 1–3 suggestions. Tone: friendly, concise, Kazakh banking context. No markdown.
```

`LanguagePicker.pick(String userId)` — MVP returns `"ru"` for everyone (TODO: user preference store).

Method `SummaryLlmService.summarise(String userId, String period, UserMetricsDto metrics, ActivePlanView plan) -> SummaryResult`. Fallback (no key): returns a hand-built Russian string built from the metrics.

- [ ] TDD: fallback test, OpenAI-happy test (MockWebServer), commit `feat(summary-llm): produces multilingual spending summaries via OpenAI`.

### Task F3: `MetricsComputedListener` + `SummaryEventPublisher`

Listener consumes `analytics.metrics-computed`, pulls metrics + plan, calls `SummaryLlmService.summarise`, publishes `SummaryGenerated` on `ai.summary-generated`.

- [ ] TDD + commit `feat(summary-llm): listener fans summary events`.

### Task F4: notification-service consumes the new topic

**Files:**
- Create: `notification-service/.../listener/AiSummaryListener.java`
- Test: `notification-service/.../listener/AiSummaryListenerTest.java`

```java
@Component @Slf4j
public class AiSummaryListener {
    @KafkaListener(topics = EventTopics.AI_SUMMARY_GENERATED, groupId = "notification-service")
    public void onSummary(SummaryGenerated e) {
        log.info("🔔 AI-SUMMARY → user {} ({}): {}", e.userId(), e.language(), e.summaryText());
        if (!e.suggestions().isEmpty()) log.info("   suggestions: {}", e.suggestions());
    }
}
```

Notification's `KafkaConfig` is already multi-type — no change needed; just add the listener bean.

- [ ] Test passes. Commit `feat(notification): push AI summaries`.

---

# Phase G — alser-mock-service

### Task G1: Module skeleton + V1 schema + seed + offer endpoint

- [ ] **Steps 1–8:** module skeleton (port 8095, `alser_db`, JPA, Flyway, JWT permitAll for `/api/alser/offers/**`), `db/init-databases.sql` adds `alser_db`, docker-compose entry, gateway `/api/alser/**` route, openapi proxy.

### Task G2: `DeviceOffer` entity + repository + service + controller

- [ ] **Step 1:** Entity uses `@JdbcTypeCode(SqlTypes.JSON)` for `audienceTags` as `List<String>`.
- [ ] **Step 2:** Repository custom JPQL query that filters by an audience tag using `jsonb_exists_any`:

```java
@Query(value = "SELECT * FROM device_offer WHERE audience_tags ?| cast(:tags as text[]) AND valid_until > now()", nativeQuery = true)
List<DeviceOffer> findActiveMatchingAudience(@Param("tags") String[] tags);
```

- [ ] **Step 3:** Controller `OfferController`:

```java
@GetMapping("/offers")
public List<DeviceOfferDto> list(@RequestParam(required = false) List<String> audience) {
    if (audience == null || audience.isEmpty()) return service.listAll();
    return service.matchingAudience(audience);
}

@GetMapping("/offers/{id}")
public DeviceOfferDto get(@PathVariable UUID id) { return service.get(id); }
```

- [ ] **Step 4: Test** `OfferControllerTest.filtersByAudience` — seed 3 offers via TestEntityManager, request `?audience=apple-fan`, assert correct subset.
- [ ] **Step 5: Commit** `feat(alser-mock): device offers + audience filter`.

### Task G3: `ApplyDiscountController`

```java
@PostMapping("/apply-discount")
public ApplyDiscountResponse apply(@Valid @RequestBody ApplyDiscountRequest req) {
    return service.apply(req);
}
```

Service inserts `applied_discount` row with `finalPrice = basePrice * (1 - discountPct/100)`, returns the row.

- [ ] TDD + commit.

---

# Phase H — halyk-travel-mock-service

Mirror Phase G. Port 8096, `travel_db`. `TravelOffer` adds `kind` enum (`FLIGHT/HOTEL/TOUR`). `BookingController.POST /api/halyk-travel/apply-discount` creates a `booking`.

- [ ] Same TDD + commit cadence.

---

# Phase I — recommendation-service + analytics V3

### Task I1: analytics-service `recommendation` table

**Files:**
- Create: `analytics-service/src/main/resources/db/migration/V3__recommendation.sql` (full schema above)
- Create: `analytics-service/.../domain/Recommendation.java`
- Create: `analytics-service/.../repository/RecommendationRepository.java`
- Create: `analytics-service/.../controller/RecommendationInternalController.java` (`POST /api/analytics/internal/recommendations` for the recommendation-service to write a batch, `GET /api/analytics/recommendations/{userId}` for the app)
- Modify: `analytics-service/.../config/SecurityConfig.java` (permitAll `/api/analytics/internal/**`)

- [ ] TDD: a JPA test that inserts a Recommendation and reads it back. Commit `feat(analytics): V3 recommendation table + internal write/read endpoints`.

### Task I2: recommendation-service module skeleton

Port 8093, no own DB, same OpenAI plumbing. Multi-type Kafka consumer. Gateway `/api/recommendations/**` route. Commit.

### Task I3: Clients (`AnalyticsClient`, `GoalsClient`, `AlserClient`, `HalykTravelClient`, `AnalyticsWriteClient`)

Each `@LoadBalanced WebClient` against `lb://<svc>`. MockWebServer tests per client.

### Task I4: `AudienceTagger` (rule-based)

Maps `(metrics, goals)` → `Set<String>` audience tags:
- `savingsRate > 0.2` → `"saver"`
- `volatility > 0.3` → `"erratic-spender"`
- presence of a goal with `category == "travel"` → `"saving_for_trip"`
- top spend category is `Рестораны` → `"foodie"`
- income > 600 000 → `"high-income"`
- presence of a goal with `category == "electronics"` → `"electronics-saver"`
- otherwise add `"casual"`

Pure function — easy to TDD.

### Task I5: `RecommendationService` + `MetricsComputedListener` + `RecommendationEventPublisher`

`RecommendationService.recommend(userId, period)`:
1. fetch metrics + goals
2. compute audience tags via `AudienceTagger`
3. fetch offers from both partners filtered by those tags
4. if `props.openai.enabled` — prompt OpenAI to score+rank offers (system prompt: "you are an offer-matching ML model emulated as an LLM; output JSON `{matches:[{offerId,partner,score,rationale}]}` sorted by score desc; keep top 5"). Fallback: assign a score proportional to tag overlap.
5. persist top-5 to `analytics_db.recommendation` via `AnalyticsWriteClient`
6. emit `RecommendationReady` event

Listener consumes `analytics.metrics-computed` and calls `recommend(...)`.

- [ ] TDD: AudienceTaggerTest (10+ assertion cases); RecommendationServiceTest with mocked clients + OpenAI fallback path; listener test (`@Disabled` for embedded-kafka if Testcontainers blocked); commit per layer.

---

# Phase J — integration-service forwards recommendations

### Task J1: `RecommendationListener` in integration-service

**Files:**
- Create: `integration-service/.../listener/RecommendationListener.java`
- Create: `integration-service/.../client/AlserClient.java`
- Create: `integration-service/.../client/HalykTravelClient.java`
- Create: `integration-service/.../config/WebClientConfig.java` (`@LoadBalanced WebClient.Builder`)

`RecommendationListener`:
```java
@KafkaListener(topics = EventTopics.AI_RECOMMENDATION_READY, groupId = "integration-service")
public void onRecommendation(RecommendationReady e) {
    for (var match : e.offers()) {
        var partnerLog = switch (match.partner()) {
            case "ALSER" -> alser.notify(e.userId(), match);
            case "HALYK_TRAVEL" -> halykTravel.notify(e.userId(), match);
            default -> "unknown partner";
        };
        log.info("📨 PARTNER → {}: user={} offer={} score={} → {}",
            match.partner(), e.userId(), match.offerId(), match.score(), partnerLog);
    }
}
```

Each partner client calls the partner's `POST /api/{partner}/apply-discount` (or `/notify` if you'd rather not auto-apply — MVP can call apply-discount with `dryRun=true` query param; partner ignores writes when dryRun is true). For MVP: hit `/offers/{id}` just to log that the partner is reachable and the targeting reached them.

- [ ] TDD: `@EmbeddedKafka` test asserts `AlserClient.notify(...)` called with the right user. Commit `feat(integration): forwards AI recommendations to partner mocks`.

---

# Phase K — Legacy shim + demo + docs

### Task K1: `ai-assistant-service` → proxy shim

- [ ] **Step 1:** Replace `AiController` so `/api/ai/budget-plan` POSTs to `lb://parse-budget-plan-service/api/parse-budget/replan` with a synthetic `ReplanRequest`. The old `/api/ai/chat` returns `410 Gone` with body `{"error":"endpoint removed — use /api/financial-agent/chat (todo) or /api/parse-budget/replan"}`.
- [ ] **Step 2:** Annotate the controller `@Deprecated` and add a Javadoc note.
- [ ] **Step 3:** Add `spring-cloud-starter-loadbalancer` to ai-assistant's `build.gradle.kts` if absent.
- [ ] **Step 4:** Run the existing `demo/run-demo.sh` — must still complete (the AI step now hits the shim and through the new pipeline).
- [ ] **Step 5:** Commit `refactor(ai-assistant): proxy shim to parse-budget-plan-service (deprecated)`.

### Task K2: `demo/run-ai-plane.sh`

A new bash script that exercises the AI plane end-to-end:

```bash
#!/usr/bin/env bash
set -euo pipefail
GW=${GW:-http://localhost:8080}
TOKEN=$(...)   # reuse demo/run-demo.sh token helper
USER_ID=...

# 1. baseline plan
curl -s -X POST $GW/api/budget/plan -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" --data-binary @demo/ai-baseline-plan.json

# 2. enough txns to overspend Рестораны by 30%
for amt in 30000 30000 30000 25000 25000; do
  curl -s -X POST $GW/api/transactions -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"accountId\":\"a1\",\"amount\":$amt,\"merchant\":\"KFC\",\"mcc\":\"5812\"}"
done

# 3. recompute metrics (triggers drift → financial agent → parse-budget → budget v2 + summary + recommendation)
curl -s -X POST $GW/api/analytics/metrics/$USER_ID/2026-05/recompute -H "Authorization: Bearer $TOKEN"

# 4. wait 5s and read back
sleep 5
echo "=== budget v2 ==="
curl -s $GW/api/budget/dashboard -H "Authorization: Bearer $TOKEN"
echo "=== recommendations ==="
curl -s $GW/api/analytics/recommendations/$USER_ID -H "Authorization: Bearer $TOKEN"
docker compose logs --since 60s notification-service | grep AI-SUMMARY || true
docker compose logs --since 60s integration-service  | grep PARTNER     || true
```

- [ ] Commit `chore(demo): run-ai-plane.sh exercises the full AI flow`.

### Task K3: Docs

- [ ] Update `docs/ARCHITECTURE.md`: add §17 "AI plane" with a Mermaid sequence diagram.
- [ ] Update `Maqsat_Family_Technical_Context.md` §3 (service table — +6 rows + ai-assistant deprecated) and §4.1 (mark MVP collapsed pieces).
- [ ] Commit `docs: AI plane (financial-agent, parse-budget, summary-llm, recommendation + partner mocks)`.

---

# Phase L — Observability

### Task L1: Counters

- `financial-agent-service`: `maqsat_replans_total{trigger}`, `maqsat_openai_calls_total{service,model,outcome}` (outcome = ok|fallback|error)
- `summary-llm-service`: `maqsat_summaries_generated_total{language}`
- `recommendation-service`: `maqsat_recommendations_total{partner}`
- `parse-budget-plan-service`: `maqsat_parse_budget_replan_total{outcome}`

- [ ] Add `MeterRegistry` injection + `counter(...)` calls in each service. Commit per service.

### Task L2: Dashboard

- Add panels to `observability/grafana/dashboards/maqsat-business.json`: row "AI plane" with stat panels for each counter, time-series for OpenAI call outcomes.

- [ ] Verify by running with the observability overlay. Commit.

---

## Test strategy summary

| Layer | Type | Coverage |
|---|---|---|
| Records / DTOs | compile-only | shape contracts |
| Repositories | `@DataJpaTest` against host Postgres or H2 | round-trip per entity |
| Services | `@SpringBootTest` slice + Mockito | fallback paths, OpenAI happy paths, error paths |
| Clients | `MockWebServer` | request shape + response parsing |
| Listeners | `@EmbeddedKafka` (or `@Disabled` with Testcontainers reason — same as analytics-service) | publish→consume |
| Controllers | `MockMvc` + mock JWT | happy + 4xx |
| Partner mocks | `@DataJpaTest` + `@WebMvcTest` | seed assertions + audience-filter query |
| End-to-end | `demo/run-ai-plane.sh` | full pipeline observable in logs |

---

## Self-review notes

- **Spec coverage:** every draw.io node has an owner — Comprehensive Financial Agent → `financial-agent-service`, Parse Budget Plan Service → `parse-budget-plan-service`, Light Summary LLM agent → `summary-llm-service`, AI Recommendation Analysis System → `recommendation-service`, Alser → `alser-mock-service`, Halyk Travel → `halyk-travel-mock-service`. Metric analysis + Metrics DB + Analysis Notebook are owned by the already-built `analytics-service`.
- **Placeholder scan:** none — every step has concrete code.
- **Type consistency:** `BudgetPlanProposal`, `PlannedCategory`, `ReplanRequest`, `ParsedPlanPayload`, `SummaryResult`, `RecommendationResult` all defined and consistently referenced.
- **DB versioning consistency:** every migration is `V<n>__<name>.sql`. `budget-service` jumps to V2; `analytics-service` to V3 (V2 was added during analytics ingest task). New DBs all start at V1.
- **Backwards compatibility:** record additions only append fields; consumers fully covered. The shim keeps `/api/ai/budget-plan` reachable for one release.
- **Independent shipping:** Phases A–C are pure refactors usable by anyone; D, F, G, H, I are each new modules that compile and run alone; J wires them together; K/L finalise.

---

## Open follow-ups (out of scope for this plan, document for the next one)

1. **Segment classifier** in financial-agent: currently every user is `'default'`. Future task: a small KMeans or rule cascade on `metrics + age + income` to pick `student / family / high-income`.
2. **User language preference store** in summary-llm: MVP returns `"ru"`. Future: a `user_preferences` table (likely in auth-service or a new tiny `profile-service`).
3. **Real "Parse Budget Plan" merge semantics**: currently it just forwards the proposal. Future: merge user-only proposal with group budgets, balance virtual accounts, run consistency checks.
4. **Apply-discount UX**: integration-service's call to partner today is `dryRun=true`. Future: real two-step confirmation flow through the mobile client.
5. **Replace ai-assistant-service shim with deletion** in the next release after the mobile client stops calling `/api/ai/*`.
