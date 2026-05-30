# Maqsat & Family

Microservices backend for a Halyk SuperApp add-on:

1. **AI budgeting** — income is split into categories; spending is auto-categorized and tracked against the budget.
2. **Goals / Maqsat** — savings held on virtual accounts.
3. **Family** — a shared layer on top: shared budgets and goals, adult/child roles, child daily limits, and the **SOS approval** flow (a child hits a daily limit at the register → the parent gets a push → one-tap approval).

## Stack

Java 21 · Gradle (Kotlin DSL) multi-module monorepo · Spring Boot 3.4.1 · Spring Cloud 2024.0.0 ·
PostgreSQL + Flyway (database-per-service) · Kafka (KRaft) · Keycloak (OIDC) · Eureka ·
optional Prometheus + Grafana + Loki + Tempo via Micrometer/OTel.

## Modules & ports

| Module                 | Port | DB             | Purpose                                       |
|------------------------|------|----------------|-----------------------------------------------|
| gateway                | 8080 | —              | Routing `/api/<svc>/**` + JWT validation      |
| keycloak               | 8081 | keycloak_db    | Identity provider                             |
| budget-service         | 8082 | budget_db      | Plans, categories, limits, tracking           |
| transaction-service    | 8083 | transaction_db | Transactions + categorization engine          |
| goals-service          | 8084 | goals_db       | Goals, virtual accounts                        |
| family-service         | 8085 | family_db      | Groups, roles, child limits, SOS approval     |
| ai-assistant-service   | 8086 | —              | LLM orchestrator                              |
| notification-service   | 8087 | —              | Push imitation / Smart-Push                   |
| integration-service    | 8088 | —              | Bonuses / offers (stub)                       |
| auth-service           | 8089 | —              | Onboarding / invites via Keycloak Admin API   |
| eureka-server          | 8761 | —              | Service discovery                             |

## Architecture principles

- **Virtual layer.** Money physically sits on the main account; family/goal accounts are virtual with a limit mask applied through the API. The bank processing is never touched.
- **No credential sharing.** Members are added by invite and each logs in themselves. An invitee gets a temporary password with a forced reset (`requiredActions: UPDATE_PASSWORD`).
- **Roles live in family-service.** `adult`/`child` are tied to a family group, not to Keycloak. Keycloak only answers "who are you".
- **Thin `common` module.** Only event contracts, `ApiError`, `CurrentUser`. No business logic.
- **Cross-service state via Kafka**; synchronous reads via REST/WebClient through Eureka (`lb://`).

## Build

```bash
./gradlew build            # all modules
./gradlew :budget-service:build
./gradlew :budget-service:test --tests "Categorization*"
```

## Run the stack

```bash
cp .env.example .env
docker compose up --build              # builds an image per service from source
```

**Fast dev mode** (run locally-built jars in JRE containers — much quicker to iterate):

```bash
./gradlew build
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

- Eureka dashboard: http://localhost:8761
- Keycloak: http://localhost:8081 (admin/admin)
- Gateway: http://localhost:8080

Optional observability overlay (Grafana at http://localhost:3000):

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

## Demo

Run the whole thing at once:

```bash
bash demo/run-demo.sh
```

Or step through it (all calls go through the gateway on :8080). Get a token first:

```bash
# password grant via the public maqsat-app client
PAPA=$(curl -s -X POST http://localhost:8081/realms/maqsat/protocol/openid-connect/token \
  -d grant_type=password -d client_id=maqsat-app -d username=papa -d password=papa \
  | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
```

### 1. Budget tracking

```bash
# Category names are Cyrillic — send them from a UTF-8 file so the shell can't mangle them.
curl -s -X POST http://localhost:8080/api/budget/plan \
  -H "Authorization: Bearer $PAPA" -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @demo/plan.json

curl -s -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $PAPA" -H "Content-Type: application/json" \
  --data-binary '{"accountId":"acc-1","amount":5000,"merchant":"MAGNUM","mcc":"5411"}'

curl -s http://localhost:8080/api/budget/dashboard -H "Authorization: Bearer $PAPA"
# -> "Продукты" spent grows to 5000 (transaction -> Kafka -> budget consumer)
```

### 2. Family group + invite (no password sharing)

```bash
GID=$(curl -s -X POST http://localhost:8080/api/family/groups \
  -H "Authorization: Bearer $PAPA" -H "Content-Type: application/json" \
  --data-binary '{"name":"Maqsat Family"}' | python -c "import sys,json;print(json.load(sys.stdin)['id'])")

# Creates the child in Keycloak (temp password + forced UPDATE_PASSWORD) and a CHILD membership.
curl -s -X POST http://localhost:8080/api/auth/invite \
  -H "Authorization: Bearer $PAPA" -H "Content-Type: application/json" \
  --data-binary "{\"groupId\":\"$GID\",\"username\":\"bala-jr\",\"role\":\"CHILD\",\"dailyLimit\":2500}"
```

### 3. Conflict scenario (the headline flow)

```bash
CHILD=$(curl -s -X POST http://localhost:8081/realms/maqsat/protocol/openid-connect/token \
  -d grant_type=password -d client_id=maqsat-app -d username=child -d password=child \
  | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
CSUB=$(python -c "import base64,json;p='$CHILD'.split('.')[1];p+='='*(-len(p)%4);print(json.loads(base64.urlsafe_b64decode(p))['sub'])")

# Put the child in the group with a 3000 daily limit
curl -s -X POST http://localhost:8080/api/family/groups/$GID/members \
  -H "Authorization: Bearer $PAPA" -H "Content-Type: application/json" \
  --data-binary "{\"userId\":\"$CSUB\",\"role\":\"CHILD\",\"dailyLimit\":3000}"

# Child tries to spend 5000 at the register -> held, not declined
curl -s -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $CHILD" -H "Content-Type: application/json" \
  --data-binary '{"accountId":"child-acc","amount":5000,"merchant":"Sushi Bar","mcc":"5812"}'
# -> status PENDING_APPROVAL; family-service emits LimitExceeded;
#    notification-service logs an SOS push (docker logs maqsat-notification)

# Parent approves in one tap (use the transaction id from above)
curl -s -X POST http://localhost:8080/api/family/approvals/<transactionId> \
  -H "Authorization: Bearer $PAPA" -H "Content-Type: application/json" \
  --data-binary "{\"childUserId\":\"$CSUB\",\"approvedAmount\":5000}"
# -> LimitOverrideApproved -> transaction-service posts the payment (status POSTED)
```

### 4. Goals & AI

```bash
curl -s -X POST http://localhost:8080/api/goals -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" \
  --data-binary '{"name":"iPhone","targetAmount":600000,"monthlyContribution":50000}'

# Income -> category plan. Falls back to a rule-based split when ANTHROPIC_API_KEY is unset.
curl -s -X POST http://localhost:8080/api/ai/budget-plan -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" --data-binary '{"monthlyIncome":500000}'
```

## Notes & gotchas

- **Keycloak issuer.** `KC_HOSTNAME=http://keycloak:8080` fixes the token `iss` so tokens minted via
  the host (`localhost:8081`) are still validated by in-network services. From an IDE, services default
  to `issuer-uri=http://localhost:8081/realms/maqsat`.
- **Kafka host port.** Services use the in-network listener `kafka:9092`; the host can reach the broker
  on `localhost:29092` (external listener). Listeners bind to the routable `kafka` host because
  apache/kafka's KRaft format step rejects a `0.0.0.0` advertised address.
- **Cyrillic over curl.** Send Cyrillic JSON bodies from a UTF-8 file (`--data-binary @file`); a Windows
  shell may otherwise turn them into `?`.