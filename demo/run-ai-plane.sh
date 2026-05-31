#!/usr/bin/env bash
# End-to-end demo for the AI plane (financial-agent, summary-llm, recommendation, parse-budget).
# Requires the full stack to be up:
#   ./gradlew build
#   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
# Then: bash demo/run-ai-plane.sh
set -euo pipefail

GW="${GW:-http://localhost:8080}"
USER="${USER:-papa}"
PASS="${PASS:-papa}"
PERIOD="${PERIOD:-$(date +%Y-%m)}"

# ---------------------------------------------------------------------------
# 1. Mint a token via Keycloak password grant
# ---------------------------------------------------------------------------
echo "→ minting token via Keycloak"
TOKEN=$(curl -s -X POST "http://localhost:8081/realms/maqsat/protocol/openid-connect/token" \
  -d "grant_type=password" -d "client_id=maqsat-app" \
  -d "username=$USER" -d "password=$PASS" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

USER_ID=$(echo "$TOKEN" \
  | python3 -c "
import sys, base64, json
p = sys.stdin.read().strip().split('.')[1]
p += '=' * (-len(p) % 4)
print(json.loads(base64.urlsafe_b64decode(p))['sub'])
")
echo "  user_id=$USER_ID"

AUTH=(-H "Authorization: Bearer $TOKEN")

# ---------------------------------------------------------------------------
# 2. Seed a baseline budget plan for the period (idempotent — may return 409)
# ---------------------------------------------------------------------------
echo "→ creating baseline budget plan for $PERIOD"
PERIOD_START="${PERIOD}-01"
PERIOD_END="${PERIOD}-28"
cat >/tmp/baseline-plan.json <<JSON
{
  "periodStart": "$PERIOD_START",
  "periodEnd":   "$PERIOD_END",
  "categories": [
    {"name": "Продукты",   "type": "MANDATORY",     "limitAmount": 80000},
    {"name": "Рестораны",  "type": "DISCRETIONARY", "limitAmount": 40000},
    {"name": "Транспорт",  "type": "MANDATORY",     "limitAmount": 30000}
  ]
}
JSON
curl -s -X POST "$GW/api/budget/plan" "${AUTH[@]}" \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @/tmp/baseline-plan.json | python3 -m json.tool || true
echo

# ---------------------------------------------------------------------------
# 3. Blast 5 large Рестораны purchases (MCC 5812) — 30 % overspend trigger
# ---------------------------------------------------------------------------
echo "→ overspending Рестораны (5 × 15 000 = 75 000 vs limit 40 000)"
for amt in 15000 15000 15000 15000 15000; do
  curl -s -X POST "$GW/api/transactions" "${AUTH[@]}" \
    -H "Content-Type: application/json" \
    -d "{\"accountId\":\"a1\",\"amount\":$amt,\"merchant\":\"KFC ALMATY KZ\",\"mcc\":\"5812\"}" > /dev/null
done
echo "  done"

# ---------------------------------------------------------------------------
# 4. Recompute analytics metrics → triggers drift → full AI plane
# ---------------------------------------------------------------------------
echo "→ recomputing analytics metrics for user=$USER_ID period=$PERIOD"
curl -s -X POST "$GW/api/analytics/metrics/$USER_ID/$PERIOD/recompute" \
  "${AUTH[@]}" | python3 -m json.tool || true
echo

echo "→ waiting 8 s for downstream services (financial-agent → parse-budget → summary → recommendation)"
sleep 8

# ---------------------------------------------------------------------------
# 5. Check results
# ---------------------------------------------------------------------------
echo "=== current budget dashboard ==="
curl -s "$GW/api/budget/dashboard" "${AUTH[@]}" | python3 -m json.tool || echo "(no response)"
echo

echo "=== recommendations for user ==="
curl -s "$GW/api/analytics/recommendations/$USER_ID" "${AUTH[@]}" | python3 -m json.tool || echo "(no response)"
echo

echo "=== notification-service AI-SUMMARY log lines ==="
docker compose logs --since 90s notification-service 2>/dev/null | grep AI-SUMMARY || echo "(none yet)"

echo "=== integration-service PARTNER log lines ==="
docker compose logs --since 90s integration-service 2>/dev/null | grep PARTNER || echo "(none yet)"
