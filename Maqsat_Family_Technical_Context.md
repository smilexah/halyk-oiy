# Maqsat & Family — Technical Context & Data-Flow Reference

*Context document for LLM coding assistants and engineers. It describes the existing services, how they exchange data, and the end-to-end data flow that is defined canonically in the draw.io architecture diagram.*

---

## 0. How to use this document

This is a **context file**, not a tutorial. It is written so that an AI assistant (or a new engineer) can load it and immediately reason about the system: what each service does, what data crosses each boundary, where compute physically runs, and how a request or a transaction propagates end to end.

Three things to keep in mind while reading:

1. **The application backend is a *simulation* of the banking application.** The microservices in the codebase stand in for the bank's real transactional systems so the product can be built and demoed without the live core. Treat them as the contract for behaviour, not as the bank's actual deployment.
2. **The real system spans three planes** — a transactional plane (the simulated banking backend), an **analytics & AI plane that runs on the bank's own physical GPU servers**, and an ecosystem plane (partner services). This document covers all three and, crucially, **the data exchanged between them**.
3. **The draw.io diagram is the canonical data flow.** Where this text and the diagram describe the same path, the diagram is the source of truth; Section 6 narrates it edge by edge.

---

## 1. System at a glance — three planes

```
                 ┌─────────────────────────────────────────────┐
   Mobile client │            TRANSACTIONAL PLANE               │
   (SuperApp) ───▶│   (simulated banking backend, microservices)│
                 │  gateway · transaction · budget · goals ·    │
                 │  family · auth · notification · integration  │
                 └───────┬───────────────────────┬─────────────┘
                         │ purchases + metadata   │ user + group budgets
                         ▼                         ▼
                 ┌─────────────────────────────────────────────┐
                 │      ANALYTICS & AI PLANE  (on-prem GPU)      │
                 │  Analysis Notebook · Metrics DB ·            │
                 │  Metric analysis · Comprehensive Financial   │
                 │  Agent · Parse Budget Plan · Light Summary    │
                 │  LLM agent · AI Recommendation System        │
                 └───────┬───────────────────────┬─────────────┘
        AI summary /      │                       │  sale/discount data
        suggestions ◀─────┘                       ▼  + target audience
                 ┌─────────────────────────────────────────────┐
                 │           ECOSYSTEM PLANE (partners)         │
                 │   Alser · Halyk Travel · bonus / discounts   │
                 └─────────────────────────────────────────────┘
```

- **Transactional plane** — records money movement, holds budgets/goals/family state, serves the app. This is what the backend codebase simulates.
- **Analytics & AI plane** — turns raw transactions into **user metrics**, validates and re-plans budgets, produces natural-language summaries, and computes **targeting signals** for offers. Its heavy models run on the bank's **physical GPU servers**.
- **Ecosystem plane** — partner commerce (devices, travel, discounts) that consumes targeting signals and returns offers.

---

## 2. Infrastructure context

**Physical GPU servers (on-prem).** The bank operates its own physical GPU servers. All heavy AI/ML inference runs there, on-premise — there is **no external cloud LLM API in the data path**. This is why the analytics/AI components are first-class services rather than a single SDK call: they are workloads scheduled onto GPU hardware the bank controls. Concretely, the GPU plane hosts the **Comprehensive Financial Agent**, the **Light Summary LLM agent**, and the **AI Recommendation Analysis System**. Lightweight, latency-sensitive models (e.g. the multilingual "why this amount" explanation model, Kazakh / Russian / English) may run on CPU; the large generative and analytical models run on GPU.

**Simulated transactional backend.** The microservices (Section 3) are a faithful simulation of the bank's transactional application: a Spring Cloud gateway with JWT, per-service databases, service discovery, Kafka for events, and an observability stack. In production these map onto the bank's real core; in development they are self-contained.

**Data residency.** Because compute is on-prem, customer transaction data and derived metrics never leave the bank's infrastructure. The analytics plane reads transaction data and budget state directly from the transactional plane's stores and writes derived artefacts (metrics, summaries, recommendations) back into the appropriate stores or message topics.

---

## 3. The transactional plane (simulated banking backend)

Every customer request enters through the **gateway**, which authenticates (JWT) and routes to a service. Services communicate **synchronously** over REST and **asynchronously** over Kafka. Each service owns its own database; no service reads another's tables.

| Service | Responsibility | Reads (in) | Writes / emits (out) |
|---|---|---|---|
| **gateway** | Single entry; JWT auth; routing; aggregated API docs | HTTP from app | routed HTTP to services |
| **transaction-service** | Records transactions; runs the categorisation engine; soft-blocks over-limit child purchases | `POST /api/transactions`; `family.limit-override-approved` | writes `transaction_db`; emits `transaction.categorized`, `transaction.limit-exceeded` |
| **budget-service** | Budget plans, categories, limits, live spend tracking, dashboard | `POST /api/budget/plan`, `GET /api/budget/dashboard`; `transaction.categorized` | writes `budget_db` (plan + `spent +=`) |
| **goals-service** | Goals and their virtual accounts; contributions | `POST /api/goals`, `GET /api/goals`, `POST /api/goals/{id}/contribute` | writes `goals_db` |
| **family-service** | Groups, roles (adult/child), child limits, SOS approval | groups / members / limits / approvals; sync limit checks from transaction-service | writes `family_db`; emits `family.limit-override-approved` |
| **auth-service** | Onboarding & invites; creates child accounts | invite / onboarding requests | Keycloak Admin API; membership writes |
| **notification-service** | Push (limit warnings, SOS, AI summaries) | `transaction.*`, `family.*` events; AI summaries | push to device |
| **integration-service** | Partner offers / bonuses; receives targeting signals | `transaction.categorized`; recommendation signals; `GET /api/integration/offers` | calls to ecosystem partners |
| **ai-assistant-service** | **(deprecated — shim to parse-budget-plan-service)** `POST /api/ai/budget-plan` proxies to `parse-budget-plan-service`; `POST /api/ai/chat` returns 410 | `POST /api/ai/budget-plan` | forwards to `lb://parse-budget-plan-service/api/parse-budget/replan` |
| **analytics-service** | Computes per-user metrics; detects budget drift; persists recommendations | `POST /api/analytics/metrics/{userId}/{period}/recompute` | writes `analytics_db`; emits `analytics.metrics-computed`, `analytics.plan-drift-detected` |
| **financial-agent-service** | Consumes drift event; prompts OpenAI for re-plan proposal; forwards to parse-budget | `analytics.plan-drift-detected` | calls `parse-budget-plan-service /api/parse-budget/replan` |
| **parse-budget-plan-service** | Validates `BudgetPlanProposal`; merges with user+group budgets; calls budget-service replan | `POST /api/parse-budget/replan` | calls `budget-service /api/budget/internal/replan/{userId}` |
| **summary-llm-service** | Prompts OpenAI for multilingual spending summary; emits event | `analytics.metrics-computed` | emits `ai.summary-generated` |
| **recommendation-service** | Matches partner offers to user audience via OpenAI; persists and emits | `analytics.metrics-computed` | writes `analytics_db.recommendation`; emits `ai.recommendation-ready` |
| **alser-mock-service** | Catalogue of electronics offers (seeded 10 rows); apply-discount endpoint | `GET /api/alser/offers`, `GET /api/alser/offers/{id}`, `POST /api/alser/apply-discount` | writes `alser_db.applied_discount` |
| **halyk-travel-mock-service** | Catalogue of travel offers (seeded 10 rows); booking endpoint | `GET /api/halyk-travel/offers`, `GET /api/halyk-travel/offers/{id}`, `POST /api/halyk-travel/apply-discount` | writes `travel_db.booking` |

**Key point for LLMs:** `ai-assistant-service` is now a **deprecated proxy shim** — it forwards budget-plan requests to `parse-budget-plan-service`. The actual AI inference is in `financial-agent-service`, `summary-llm-service`, and `recommendation-service`, all using OpenAI `gpt-4o-mini` with a deterministic fallback when `OPENAI_API_KEY` is absent.

---

## 4. The analytics & AI plane (data-analytics integration)

This is the layer that was previously under-documented. It is a separate bounded context (labelled **"Kyzylorda / Analytics service"** in the diagram) plus the GPU-hosted model services. Its job is to convert raw transactions into understanding and to keep budgets aligned with reality.

### 4.1 Components and what they do

- **Analysis Notebook** — scheduled analytics jobs that read raw transactions (purchases + metadata) and compute **user metrics**: average spend per category, income estimate, recurring debits, spend volatility, savings rate, trend/weighted-usage over time. Output is written to the **Metrics DB**.
- **Metrics DB** — the analytics store of computed per-user (and per-group) metrics. This is distinct from the transactional `budget_db`; it holds *derived* data, not source-of-truth balances.
- **Metric analysis** — reads metrics + the current budget plan and answers the decision **"does the budget plan match actual data?"** This is the drift check.
- **Comprehensive Financial Agent** (GPU LLM) — invoked when the plan **does not** match reality. It reasons over metrics + budgets and produces a re-planned / adjusted budget proposal.
- **Parse Budget Plan Service** — takes the agent's (or the validated) plan together with the **user + group budgets** and emits a strictly structured plan object.
- **Light Summary LLM agent** (GPU LLM) — turns the structured plan and metrics into a plain-language **AI summary of spending + suggestions** for the customer (multilingual).
- **AI Recommendation Analysis System** (GPU LLM) — analyses budgets + metrics + goals to produce **targeting signals** (interests, goal categories, audience tags) used for ecosystem offers.

### 4.2 How it integrates with the transactional plane — and the data exchanged

| Direction | From → To | Data exchanged |
|---|---|---|
| Ingest | `transaction_db` → Analysis Notebook | **Purchases + metadata**: transactions with amount, merchant, MCC, type, timestamp, account/user ids |
| Compute | Analysis Notebook → Metrics DB | **User metrics**: aggregates, recurring-debit detection, income estimate, volatility, savings rate |
| Validate | Metrics DB + `budget_db` → Metric analysis | metrics + current **budget plan** → boolean match + per-category drift |
| Re-plan (drift) | Metric analysis → Comprehensive Financial Agent → Parse Budget Plan Service | drift report + metrics + budgets → adjusted **structured plan** |
| Read budgets | `budget_db` → Parse Budget Plan Service | **User + group budgets** (plans, limits, owners) |
| Summarise | Parse Budget Plan Service → Light Summary LLM agent → Mobile client | structured plan + metrics → **AI summary of spendings / suggestions** (text, kk/ru/en) |
| Recommend | `budget_db` + Metrics DB → AI Recommendation Analysis System | budgets + metrics + goal categories → **sale/discount data + target audience** signal |
| Notify | AI Recommendation / agents → notification-service → Mobile client | suggestion / alert payload → push |

The analytics plane **reads** from the transactional stores (transactions, budgets) and **writes back** three kinds of artefact: derived metrics (to Metrics DB), customer-facing summaries (to the app via notification), and targeting signals (to the ecosystem via integration). It does not own transactional truth.

---

## 5. The ecosystem plane (partners)

Partner commerce systems sit behind an integration boundary and are out of Maqsat's ownership. In the diagram they appear as bounded contexts: **Alser** (Discounts/Sales store, "Buy device + Apply Discount" service, other services) and **Halyk Travel** (Discounts/Sales store, "Buy Ticket/Tour + Apply Discount" service, other services).

The exchange is one primary signal and one return:

- **In:** the **AI Recommendation Analysis System** sends **"Sale/Discount Data + Target Audience"** through the integration gateway to partner services — i.e. which customers/segments to target and with what kind of offer, derived from their budgets, metrics, and goal categories (a travel goal → travel offers).
- **Out:** partners expose discounts/sales and apply them at purchase ("Apply Discount" services), surfaced back to the customer (e.g. via `integration-service`'s offers endpoint and the goal "on-track" rewards).

Partners are external: Maqsat sends targeting signals and receives offer data; it does not execute partner commerce itself.

---

## 6. End-to-end data flow (canonical — the draw.io diagram)

This section narrates the diagram. Each line is **source → target : data on the edge.**

**A. Customer actions (transactional plane)**

- Mobile client → Make a Transaction / Look at Budgets view / Edit Budget / Finish Goal : user intents
- Make a Transaction → Central Halyk Transaction Processing Service : new transaction
- Central Halyk Transaction Processing Service → Transactions DB : **Transaction Data**
- Central Halyk Transaction Processing Service → Transaction service : transaction for categorisation
- Transaction service → Transactions DB : **Transaction Data** (categorised)
- Look at Budgets view → /api/budget : read request
- /api/budget → Budget plans DB : **read plan + spent**
- /api/budget → Mobile client : **budgets dashboard**
- Edit Budget → Budget plans DB : **Budget edit / new goal**

**B. Analytics & AI plane**

- Transactions DB → Analysis Notebook : **Purchases + metadata**
- Analysis Notebook → Metrics DB : **User Metrics**
- Metrics DB → Metric analysis : computed metrics
- Metric analysis → "Budget plan matches actual data?" : plan-vs-actual check
- decision **No** → Comprehensive Financial Agent : drift → re-plan request
- decision **Yes** → Parse Budget Plan Service : plan is valid → proceed
- Comprehensive Financial Agent → Parse Budget Plan Service : adjusted plan
- Budget plans DB → Parse Budget Plan Service : **User + Group Budgets**
- Parse Budget Plan Service → Light Summary LLM agent : structured plan
- Light Summary LLM agent → Mobile client : **AI Summary of Spendings / Suggestions**
- Budget plans DB + Metrics DB → AI Recommendation Analysis System : budgets + metrics

**C. Ecosystem + notifications**

- AI Recommendation Analysis System → integration gateway → Alser / Halyk Travel : **Sale/Discount Data + Target Audience**
- AI Recommendation Analysis System → user_notifications : suggestion
- user_notifications → Mobile client : push

The shape of the flow: **transactions flow in and down** (transactional → analytics), **understanding flows back up and out** (summaries to the user, targeting signals to partners). The "matches actual data?" decision is the hinge — a passing plan goes straight to summarisation; a failing plan is re-planned by the GPU financial agent first.

---

## 7. Event / message contracts (Kafka)

Three topics carry the asynchronous, fan-out interactions in the transactional plane.

| Topic | Producer | Consumers | Purpose |
|---|---|---|---|
| `transaction.categorized` | transaction-service | budget-service, integration-service, notification-service, analytics ingest | a purchase was sorted → update tracking, surface offers, feed analytics |
| `transaction.limit-exceeded` | transaction-service | notification-service, family-service | a child purchase breached a limit → SOS |
| `family.limit-override-approved` | family-service | transaction-service, notification-service | parent approved → complete the held transaction |

Budget tracking in the transactional plane is **event-driven and real-time** (`categorized` → `spent +=`). The analytics plane's metric computation is a **separate, scheduled** process that also consumes/ reads the same transaction data but on its own clock — the two should not be conflated.

---

## 8. Representative data contracts

Illustrative shapes (the authoritative schemas live in code). Provided so an LLM can reason about payloads.

```jsonc
// Transaction (ingested by transaction-service)
{ "transactionId":"...", "userId":"...", "accountId":"...",
  "amount": 12990, "currency":"KZT", "merchant":"...", "mcc":"5411",
  "timestamp":"2026-05-31T10:00:00Z", "type":"PURCHASE", "status":"POSTED" }

// transaction.categorized (event)
{ "transactionId":"...", "userId":"...", "amount":12990, "mcc":"5411",
  "merchant":"...", "categoryId":"food", "categoryType":"discretionary",
  "timestamp":"..." }

// transaction.limit-exceeded (event) — drives the SOS
{ "holdId":"...", "transactionId":"...", "childUserId":"...", "groupId":"...",
  "amount":25000, "limitType":"daily", "limitAmount":2000,
  "attemptedTotal":27000, "timestamp":"..." }

// family.limit-override-approved (event)
{ "holdId":"...", "transactionId":"...", "approvedByUserId":"...",
  "childUserId":"...", "newLimitDelta":25000, "timestamp":"..." }

// BudgetPlan (budget_db; produced by Parse Budget Plan Service)
{ "planId":"...", "ownerType":"user|group", "ownerId":"...", "period":"2026-06",
  "createdByAi":true, "version":3,
  "categories":[ { "categoryId":"food","name":"Food","type":"discretionary","limit":120000 } ] }

// UserMetrics (Metrics DB; produced by Analysis Notebook)
{ "userId":"...", "period":"2026-05",
  "avgSpendByCategory":{ "food":118000,"taxi":24000 },
  "incomeEstimate":470000, "recurringDebits":[ {"label":"utilities","amount":32590,"dayOfMonth":1} ],
  "volatility":0.18, "savingsRate":0.22, "computedAt":"..." }

// Plan-vs-actual check (Metric analysis output)
{ "userId":"...", "planId":"...", "matches":false,
  "driftByCategory":{ "restaurants":+0.25 }, "recommendAdjustment":true }

// Targeting signal (AI Recommendation → ecosystem)
{ "userId|segmentId":"...", "goalCategories":["travel"],
  "interests":["flights","hotels"], "targetAudienceTags":["saving_for_trip"],
  "suggestedOfferTypes":["travel_discount"] }

// AI summary (Light Summary LLM agent → app)
{ "userId":"...", "period":"2026-05", "language":"ru",
  "summaryText":"...", "highlights":["..."], "suggestions":["..."] }
```

---

## 9. Data model (entities and ownership)

- **transaction_db** (transaction-service): `Transaction` { amount, merchant, mcc, date, categoryId, status }.
- **budget_db** (budget-service): `BudgetPlan` { ownerType, ownerId, period, version }, `BudgetCategory` { name, type, limit, period, planId }, spend-tracking { categoryId, period, spent, limit }.
- **goals_db** (goals-service): `Goal` { name, category, target, deadline, monthlyContribution, virtualAccountId }, `VirtualAccount` { balance, ownerType }.
- **family_db** (family-service): `Group` { name, type }, `Membership` { userId, groupId, role }, `ChildLimit` { membershipId, monthly, daily, context }.
- **Metrics DB** (analytics): `UserMetrics` (derived; see §8) — *not* transactional truth.

Group budgets reuse the personal model: a group plan is a `BudgetPlan` with `ownerType = group`. No parallel "group" tables.

---

## 10. Cross-cutting principles

- **Virtual overlay.** Maqsat plans, tracks, and proposes; real money movement (transfers, goal funding, limit overrides) always routes back through the bank core and an explicit customer confirmation (Face ID). Maqsat never holds or moves money itself.
- **Compute placement.** Heavy AI/ML inference runs on the bank's **physical GPU servers** (on-prem). `ai-assistant-service` is the orchestration door to that plane; the diagram's agents (Financial Agent, Summary LLM, Recommendation) are the GPU workloads. Small explanation models may run on CPU; all customer data stays on-prem.
- **Languages.** Customer-facing AI output is multilingual: Kazakh, Russian, English.
- **Security.** Centralised at the gateway via JWT; Keycloak is the identity provider; `auth-service` exists specifically to provision child accounts on invite.
- **Source-of-truth discipline.** Transactional stores own balances and state; the analytics plane only derives. Events (`transaction.*`, `family.*`) are the contract for real-time fan-out; scheduled analytics is a separate path.

---

## 11. Glossary

- **Transactional plane** — the simulated banking backend (microservices) that records money movement and serves the app.
- **Analytics & AI plane** — the data-analytics service plus GPU-hosted model services that derive metrics, re-plan budgets, summarise, and recommend.
- **Ecosystem plane** — partner commerce (Alser, Halyk Travel, bonuses) reached via integration.
- **User metrics** — derived per-user aggregates computed by the Analysis Notebook and stored in the Metrics DB.
- **Plan-vs-actual check** — the "Budget plan matches actual data?" decision; the hinge of the analytics flow.
- **Targeting signal** — "Sale/Discount Data + Target Audience" sent to partners for offers.
- **SOS / limit override** — the real-time held-transaction + parent-approval flow (`limit-exceeded` → `limit-override-approved`).
- **Virtual overlay** — Maqsat as a brain on top of the account; never a wallet.
- **GPU plane** — the bank's on-prem physical GPU servers hosting the AI agents.

---

## 12. Appendix — node / edge index of the draw.io diagram

**Nodes.** Mobile client; Make a Transaction; Look at Budgets view; Edit Budget; Finish Goal; /api/budget; Central Halyk Transaction Processing Service; Transaction service; Transactions DB; Analysis Notebook; Metrics DB; Metric analysis; "Budget plan matches actual data?" (decision); Comprehensive Financial Agent; Parse Budget Plan Service; Budget plans DB; Light Summary LLM agent; AI Recommendation Analysis System; user_notifications; integration gateway; Alser (Discounts/Sales, Buy device + Apply Discount, other services); Halyk Travel (Discounts/Sales, Buy Ticket/Tour + Apply Discount, other services).

**Bounded contexts (containers).** Halyk Maqsat (outer); Halyk Maqsat MiniApp; Kyzylorda / Analytics service; Halyk Core; Alser; Halyk Travel.

**Edges:** see Section 6 for the full source → target : data listing. The diagram is the canonical reference; this document narrates and contracts it.

---

*Reconciliation note: the backend codebase simulates the transactional plane; the draw.io diagram defines the full data flow including the on-prem GPU analytics/AI plane and the ecosystem. Where they overlap, the diagram is canonical for flow and the codebase is canonical for service behaviour and schemas.*
