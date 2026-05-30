#!/usr/bin/env bash
# End-to-end demo for Maqsat & Family. Requires the stack to be up:
#   ./gradlew build
#   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d   # fast: runs prebuilt jars
#   # or: docker compose up --build                                        # builds images from source
#
# Then: bash demo/run-demo.sh
set -euo pipefail

GW=http://localhost:8080
KC=http://localhost:8081/realms/maqsat/protocol/openid-connect/token

token() { curl -s -X POST "$KC" -d grant_type=password -d client_id=maqsat-app \
  -d "username=$1" -d "password=$2" | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])"; }
sub() { python -c "import sys,base64,json;p='$1'.split('.')[1];p+='='*(-len(p)%4);print(json.loads(base64.urlsafe_b64decode(p))['sub'])"; }

PAPA=$(token papa papa)
CHILD=$(token child child)
CSUB=$(sub "$CHILD")

echo "############ 1. AI budget plan (income -> categories) ############"
curl -s -X POST "$GW/api/ai/budget-plan" -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" --data-binary '{"monthlyIncome":500000}'
echo

echo "############ 2. Budget: create plan + spend + dashboard ############"
curl -s -X POST "$GW/api/budget/plan" -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json; charset=utf-8" --data-binary @"$(dirname "$0")/plan.json"
echo
curl -s -X POST "$GW/api/transactions" -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" --data-binary '{"accountId":"acc-1","amount":5000,"merchant":"MAGNUM","mcc":"5411"}'
echo; sleep 5
echo "-- dashboard (Продукты spent should be 5000) --"
curl -s "$GW/api/budget/dashboard" -H "Authorization: Bearer $PAPA"
echo

echo "############ 3. Family group + invite a child via Keycloak ############"
GID=$(curl -s -X POST "$GW/api/family/groups" -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" --data-binary '{"name":"Maqsat Family"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['id'])")
echo "group=$GID"
INVITEE="bala-$RANDOM"   # unique so the demo is re-runnable
curl -s -X POST "$GW/api/auth/invite" -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" \
  --data-binary "{\"groupId\":\"$GID\",\"username\":\"$INVITEE\",\"role\":\"CHILD\",\"dailyLimit\":2500}"
echo

echo "############ 4. Conflict scenario (the headline flow) ############"
curl -s -X POST "$GW/api/family/groups/$GID/members" -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" --data-binary "{\"userId\":\"$CSUB\",\"role\":\"CHILD\",\"dailyLimit\":3000}" >/dev/null
echo "-- child buys 5000 over a 3000 limit -> PENDING_APPROVAL --"
TXN=$(curl -s -X POST "$GW/api/transactions" -H "Authorization: Bearer $CHILD" \
  -H "Content-Type: application/json" --data-binary '{"accountId":"child-acc","amount":5000,"merchant":"Sushi Bar","mcc":"5812"}')
echo "$TXN"
TXID=$(echo "$TXN" | python -c "import sys,json;print(json.load(sys.stdin)['id'])")
sleep 3
echo "-- parent SOS push (notification-service log): --"
docker logs maqsat-notification 2>&1 | grep -o 'SOS-PUSH[^\"]*' | tail -1 || true
echo "-- parent approves in one tap --"
curl -s -X POST "$GW/api/family/approvals/$TXID" -H "Authorization: Bearer $PAPA" \
  -H "Content-Type: application/json" --data-binary "{\"childUserId\":\"$CSUB\",\"approvedAmount\":5000}"
echo; sleep 3
echo "-- child transactions (held one is now POSTED) --"
curl -s "$GW/api/transactions" -H "Authorization: Bearer $CHILD"
echo
