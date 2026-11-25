#!/usr/bin/env bash
# Checks the running stack through nginx, the way a browser reaches it: the app is served, and
# the API behind it can register a user and track an application. Used by CI.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="smoke-$(date +%s)-$RANDOM@example.com"

echo "Waiting for the API behind $BASE_URL"
for _ in $(seq 1 90); do
    # 401 means the API is up and enforcing authentication
    code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/me" || true)
    [ "$code" = "401" ] && break
    sleep 2
done
[ "$code" = "401" ] || { echo "API did not come up (last status $code)" >&2; exit 1; }

# the single-page app is served for any route
curl -fsS "$BASE_URL/applications/42" | grep -q '<div id="root">'

TOKEN=$(curl -fsS -X POST "$BASE_URL/api/auth/register" -H 'Content-Type: application/json' \
    -d "{\"email\": \"$EMAIL\", \"password\": \"smoke-test-password\", \"displayName\": \"Smoke\"}" | jq -r .token)

ID=$(curl -fsS -X POST "$BASE_URL/api/applications" -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
    -d '{"company": "Smoke Ltd", "roleTitle": "Engineer", "status": "APPLIED"}' | jq -r .id)

STATUS=$(curl -fsS -X PATCH "$BASE_URL/api/applications/$ID/status" -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $TOKEN" -d '{"status": "INTERVIEWING"}' | jq -r .status)

TOTAL=$(curl -fsS "$BASE_URL/api/applications/stats" -H "Authorization: Bearer $TOKEN" | jq -r .total)

echo "Application $ID is $STATUS; user has $TOTAL application(s)"
if [ "$STATUS" != "INTERVIEWING" ] || [ "$TOTAL" != "1" ]; then
    echo "Smoke test failed" >&2
    exit 1
fi
echo "Smoke test passed"
