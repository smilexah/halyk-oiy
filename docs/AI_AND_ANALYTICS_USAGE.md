# Analytics + AI Plane — Usage Guide

What/why/how for the 7 services that turn raw transactions into per-user metrics, AI-driven budget re-plans, multilingual summaries, and partner-targeting recommendations.

---

## Services at a glance

| Service | Port | DB | Triggered by | Emits |
|---|---|---|---|---|
| **analytics-service** | 8090 | `analytics_db` | Kafka `transaction.categorized`, scheduled batch, REST `/recompute` | `analytics.metrics-computed`, `analytics.plan-drift-detected` |
| **financial-agent-service** | 8091 | `priors_db` | Kafka `analytics.plan-drift-detected` | (POSTs to parse-budget-plan-service) |
| **parse-budget-plan-service** | 8094 | — | REST `POST /api/parse-budget/replan` (from financial-agent) | (POSTs to budget-service replan endpoint) |
| **summary-llm-service** | 8092 | — | Kafka `analytics.metrics-computed` | `ai.summary-generated` |
| **recommendation-service** | 8093 | (writes to `analytics_db.recommendation`) | Kafka `analytics.metrics-computed` | `ai.recommendation-ready` |
| **alser-mock-service** | 8095 | `alser_db` | REST `GET /api/alser/offers`, `POST /api/alser/apply-discount` | — |
| **halyk-travel-mock-service** | 8096 | `travel_db` | REST `GET /api/halyk-travel/offers`, `POST /api/halyk-travel/apply-discount` | — |

All 7 are reachable from the host only via the gateway at `http://localhost:8080/api/<svc>/**`. None publish host ports (security: gateway is the sole external entry).

---

## Setup

### 1. Required env (`.env` at repo root)

```bash
cp .env.example .env
# then edit .env to set OPENAI_API_KEY
```

Without `OPENAI_API_KEY`, every AI service falls back to deterministic stubs — the pipeline still runs end-to-end, the summaries / recommendations just look mechanical.

### 2. Bring the stack up

```bash
./gradlew build
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
# wait ~60s for keycloak + eureka registration
curl -s http://localhost:8761/eureka/apps -H "Accept: application/json" \
  | jq '.applications.application | length'   # expect 16
```

### 3. Mint a token (for any curl that hits an authenticated route)

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/realms/maqsat/protocol/openid-connect/token \
  -d grant_type=password -d client_id=maqsat-app \
  -d username=papa -d password=papa | jq -r .access_token)
USER_ID=$(echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r .sub)
```

---

## Service-by-service

### analytics-service

**Job:** consumes every `transaction.categorized` event, snapshots into `txn_fact`, on demand (REST or scheduled) computes `user_metrics`, `category_stat`, `recurring_debit`, and a `drift_report`.

| REST | Auth | Body |
|---|---|---|
| `POST /api/analytics/etl/run` | Bearer | — — manual batch backfill from transaction-service |
| `GET /api/analytics/metrics/{userId}/{period}` | Bearer | — |
| `POST /api/analytics/metrics/{userId}/{period}/recompute` | Bearer | — — runs Java aggregates + drift, emits both events |
| `GET /api/analytics/drift/{userId}/{period}` | Bearer | — |
| `POST /api/analytics/drift/{userId}/{period}/recompute` | Bearer | — |
| `GET /api/analytics/recommendations/{userId}` | Bearer | — — read-only, populated by recommendation-service |

Period format is `YYYY-MM` (e.g. `2026-05`).

Internal endpoints (permitAll, called by other services in-network):
- `POST /api/analytics/internal/recommendations` — recommendation-service writes a batch here.

**Try it:**
```bash
PERIOD=$(date +%Y-%m)
curl -s -X POST "http://localhost:8080/api/analytics/metrics/$USER_ID/$PERIOD/recompute" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Inside the response: `totalSpent`, `incomeEstimate`, `savingsRate`, `volatility`, per-category rollups, recurring debits.

### financial-agent-service

**Job:** when drift > 15 % in any category, ask OpenAI for a re-plan that respects population priors (`priors_db`, segment `default` by default) and corrects the drift, then forward to parse-budget-plan-service.

**Triggered by:** Kafka `analytics.plan-drift-detected`. No public REST endpoints (orchestrator only).

**How to fire it:** trigger an analytics drift (post enough transactions to overspend a category by >15 %, then call `/api/analytics/drift/{userId}/{period}/recompute`). The event drops in Kafka → financial-agent consumes → OpenAI → parse-budget → budget-service inserts a `version+1` plan.

**Fallback (no API key):** scales population prior ratios by `incomeEstimate` per category. Produces a sane plan, no LLM call. Rationale field reads `"fallback: priors-baseline (no OPENAI_API_KEY)"`.

### parse-budget-plan-service

**Job:** narrow validator + persister. Receives a `BudgetPlanProposal`, validates shape (non-empty categories, non-negative limits, types ∈ {MANDATORY, DISCRETIONARY}), then POSTs to `budget-service.POST /api/budget/internal/replan/{userId}`. Budget inserts a new row with `version = previous + 1`, `created_by_ai = true`, and marks the predecessor's `superseded_by`.

| REST | Auth | Body |
|---|---|---|
| `POST /api/parse-budget/replan` | permitAll (internal) | `{userId, period, proposal: {categories[], rationale}, driftSnapshot}` → 202 + `{newPlanId, version, supersededPlanId}` |

You normally don't call this directly — financial-agent does. Direct call shape:
```bash
curl -s -X POST http://localhost:8080/api/parse-budget/replan \
  -H "Content-Type: application/json" -d '{
    "userId":"'$USER_ID'", "period":"2026-05",
    "proposal":{
      "categories":[
        {"name":"Продукты","type":"MANDATORY","limitAmount":80000},
        {"name":"Рестораны","type":"DISCRETIONARY","limitAmount":30000}
      ],
      "rationale":"manual test"
    },
    "driftSnapshot":{}
  }' | jq .
```

### summary-llm-service

**Job:** turn `MetricsComputed` into a natural-language `{summaryText, highlights, suggestions}` in Kazakh / Russian / English (MVP: always `ru`, picked by `LanguagePicker`). Pulls user's active plan from budget for prompt context. Emits `ai.summary-generated` → `notification-service.AiSummaryListener` logs it as `🔔 AI-SUMMARY → user <id> (ru, period <p>): <text>`.

**Triggered by:** Kafka `analytics.metrics-computed`.

**Fallback (no key):** hard-built Russian sentence `"За YYYY-MM потрачено NNN ₸. (fallback, OPENAI_API_KEY не задан)"`.

Check it ran:
```bash
docker logs maqsat-notification --since 60s | grep AI-SUMMARY
```

### recommendation-service

**Job:** assemble an audience profile (rule-based `AudienceTagger` over metrics + goals), fetch offers from both partner mocks filtered by those tags, rank via OpenAI (or tag-overlap fallback), persist top-5 to `analytics_db.recommendation`, emit `ai.recommendation-ready`.

**Triggered by:** Kafka `analytics.metrics-computed`.

**Audience tag rules (pure function, `AudienceTagger`):**
- `savingsRate > 0.2` → `saver`
- `volatility > 0.3` → `erratic-spender`
- goal category `travel` → `saving_for_trip`
- top spend category `Рестораны` → `foodie`
- `incomeEstimate > 600000` → `high-income`
- goal category contains `electronics` → `electronics-saver`
- otherwise → `casual`

Read back recommendations:
```bash
curl -s "http://localhost:8080/api/analytics/recommendations/$USER_ID" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Each row: `{offerId, partner: ALSER|HALYK_TRAVEL, score 0..1, audienceTags, rationale}`. `integration-service.RecommendationListener` separately forwards the same event to the partner mocks (logs `📨 PARTNER → ALSER: ...`).

### alser-mock-service

Mock electronics storefront. 10 seeded devices with jsonb `audience_tags`.

| REST | Auth | Notes |
|---|---|---|
| `GET /api/alser/offers` | open | full catalog |
| `GET /api/alser/offers?audience=apple-fan&audience=high-income` | open | jsonb `?\|` filter (Postgres) |
| `GET /api/alser/offers/{id}` | open | one offer by UUID |
| `POST /api/alser/apply-discount` | open | body `{userId, offerId}`, returns `{finalPrice = base × (1 − discountPct/100), appliedAt}` |

```bash
curl -s "http://localhost:8080/api/alser/offers?audience=apple-fan" | jq '.[0]'
```

### halyk-travel-mock-service

Mock travel storefront. 10 seeded `FLIGHT|HOTEL|TOUR` offers. Same shape as alser-mock; endpoints under `/api/halyk-travel/`. Apply-discount creates a `booking` row.

```bash
curl -s "http://localhost:8080/api/halyk-travel/offers?audience=saving_for_trip" | jq '.[0]'
```

---

## End-to-end flows

### Flow A — drift → AI re-plan

```
client → POST /api/transactions × N (overspend Рестораны)
       → transaction-service publishes transaction.categorized × N
analytics-service consumes → POST /api/analytics/metrics/{u}/{p}/recompute
       → MetricsService stores rollups
       → DriftService compares vs active plan, > 15% deviation found
       → emits analytics.plan-drift-detected
financial-agent-service consumes
       → fetches metrics via lb://analytics-service
       → reads PopulationPrior segment "default" from priors_db
       → OpenAI gpt-4o-mini: BudgetPlanProposal
       → POST lb://parse-budget-plan-service/api/parse-budget/replan
parse-budget-plan-service
       → validates proposal
       → POST lb://budget-service/api/budget/internal/replan/{u}
budget-service
       → inserts new budget_plan row (version+1, created_by_ai=true)
       → marks predecessor.superseded_by = new.id
client → GET /api/budget/dashboard → sees the new plan
```

### Flow B — metrics → AI summary → push

```
POST /api/analytics/metrics/{u}/{p}/recompute
       → emits analytics.metrics-computed
summary-llm-service consumes
       → fetches metrics + active plan
       → OpenAI gpt-4o-mini: SummaryResult (kk|ru|en)
       → emits ai.summary-generated
notification-service.AiSummaryListener
       → logs "🔔 AI-SUMMARY → user <id> (ru, period 2026-05): <text>"
```

### Flow C — metrics → recommendation → partner targeting

```
analytics.metrics-computed
recommendation-service consumes
       → fetches metrics + goals
       → AudienceTagger → tags
       → fetches partner offers filtered by tags
       → OpenAI ranks → top 5 MatchedOffers
       → POST /api/analytics/internal/recommendations (persist)
       → emits ai.recommendation-ready
integration-service.RecommendationListener consumes
       → for each match: AlserClient.notifyTargeting or HalykTravelClient.notifyTargeting
       → logs "📨 PARTNER → ALSER: user=<u> offer=<id> score=<s> → alser-ok offer=<name>"
```

---

## Full demo (one command)

```bash
bash demo/run-ai-plane.sh
```

Steps:
1. mints a token via Keycloak
2. seeds a baseline budget plan
3. blasts 5 × 15 000 ₸ purchases at MCC 5812 (Рестораны)
4. calls `/api/analytics/metrics/$USER_ID/$PERIOD/recompute`
5. waits 8 s, then prints budget dashboard + recommendations + notification logs + integration logs

Within ~10 s after step 4 expect:
- new `budget_plan` row with `version = 2, created_by_ai = true`
- `AI-SUMMARY` push log in notification-service
- `PARTNER` targeting logs in integration-service
- one or more rows in `analytics_db.recommendation`

---

## Observability

Prometheus counters (scrape via `docker exec maqsat-<svc> wget -qO- http://localhost:<port>/actuator/prometheus`):

| Metric | Tags | Where |
|---|---|---|
| `maqsat_openai_calls_total` | `service`, `outcome=ok\|fallback\|error` | financial-agent, summary-llm, recommendation |
| `maqsat_replans_total` | `trigger=drift` | financial-agent |
| `maqsat_summaries_generated_total` | `language=ru\|kk\|en` | summary-llm |
| `maqsat_recommendations_total` | `partner=ALSER\|HALYK_TRAVEL` | recommendation |
| `maqsat_parse_budget_replan_total` | `outcome=ok\|invalid\|error` | parse-budget-plan |

Grafana board: `Maqsat / business` → row "AI plane".

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `outcome=fallback` always, no LLM output | `OPENAI_API_KEY` empty in compose env | check `.env` exists at repo root, `OPENAI_API_KEY=sk-…`, then `docker compose up -d --build` to pick up the change |
| `outcome=error` after a real call | bad/expired key, network blocked, rate-limited | inspect log; rotate the key; lower `OPENAI_MAX_TOKENS` |
| analytics endpoint returns 404 for metrics | metrics never computed for that `(userId, period)` | call `/recompute` first |
| budget dashboard still shows old plan | replan failed silently | check `maqsat_parse_budget_replan_total{outcome="error"}` and look at financial-agent + parse-budget logs |
| `notification-service` never logs `AI-SUMMARY` | summary-llm not consuming events | confirm Kafka topic exists: `docker exec maqsat-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list` and grep `ai.summary-generated`; confirm summary-llm container `UP` in `docker compose ps` |
| partner offers endpoint returns `[]` for audience tag | tag doesn't match any seeded offer's `audience_tags` jsonb | call without `?audience=` first to see the catalog; pick a tag from a real offer |
| Postgres `?\|` operator error in tests | running against H2, not Postgres | the `?\|` jsonb query is gated behind a `@Disabled` test for H2; production runs against the real DB |

---

## Disabling AI per service (without removing it)

Unset `OPENAI_API_KEY` (or set it to empty) and restart the 3 AI containers. Each service falls back to deterministic logic — no external calls, lower fidelity, zero spend.

---

## Out of scope (documented follow-ups)

- segment classifier for `priors_db` (currently every user is `"default"`)
- per-user language preference (currently all `"ru"`)
- group-budget merging in `parse-budget-plan-service` (currently user-plan only)
- real apply-discount UX through the mobile client (today: `integration-service` only logs the targeting)
- removing the `ai-assistant-service` shim once mobile stops calling `/api/ai/*`
