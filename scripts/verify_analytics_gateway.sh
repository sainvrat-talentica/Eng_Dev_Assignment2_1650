#!/usr/bin/env bash
# Verify Assignment 2 analytics APIs are reachable via backend-service gateway (:8080).
# Usage: ./scripts/verify_analytics_gateway.sh [baseUrl]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
ADMIN_KEY="${ADMIN_API_KEY:-local-dev-admin-key-change-me}"

pass=0
fail=0

check_get() {
  local name="$1"
  local path="$2"
  local code
  code=$(curl -s -o /tmp/analytics-verify.json -w "%{http_code}" "${BASE_URL}${path}")
  if [[ "$code" == "200" ]]; then
    if python3 -c "import json; d=json.load(open('/tmp/analytics-verify.json')); exit(0 if d.get('narrative') or d.get('totalAffected') is not None or d.get('totalFailed') is not None or d.get('evidence') or d.get('warehouseRisks') is not None else 1)" 2>/dev/null; then
      echo "  OK   $name (HTTP 200, body valid)"
      pass=$((pass + 1))
    else
      echo "  FAIL $name (HTTP 200 but unexpected body)"
      fail=$((fail + 1))
    fi
  else
    echo "  FAIL $name (HTTP $code)"
    fail=$((fail + 1))
  fi
}

check_post() {
  local name="$1"
  local path="$2"
  local body="$3"
  local code
  code=$(curl -s -o /tmp/analytics-verify.json -w "%{http_code}" \
    -X POST "${BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    -d "$body")
  if [[ "$code" == "200" ]]; then
    if python3 -c "import json; d=json.load(open('/tmp/analytics-verify.json')); exit(0 if d.get('narrative') or d.get('evidence') else 1)" 2>/dev/null; then
      echo "  OK   $name (HTTP 200, body valid)"
      pass=$((pass + 1))
    else
      echo "  FAIL $name (HTTP 200 but unexpected body)"
      fail=$((fail + 1))
    fi
  else
    echo "  FAIL $name (HTTP $code)"
    fail=$((fail + 1))
  fi
}

echo "=== Analytics gateway verification ==="
echo "Gateway: $BASE_URL (analytics-service REST API)"
echo

echo "Import sample dataset (admin)..."
import_code=$(curl -s -o /tmp/analytics-import.json -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: ${ADMIN_KEY}")
if [[ "$import_code" == "200" ]]; then
  echo "  OK   import (HTTP 200)"
else
  echo "  WARN import returned HTTP $import_code (continuing if data already loaded)"
fi
echo

echo "UC1 GET /analytics/delays"
check_get "UC1 delays" "/api/v1/analytics/delays?city=Pune&date=2025-06-06"

echo "UC2 GET /analytics/failures"
check_get "UC2 failures" "/api/v1/analytics/failures?clientId=337&from=2025-01-01T00:00:00Z&to=2025-12-31T00:00:00Z"

echo "UC3 GET /analytics/failures/by-warehouse"
check_get "UC3 warehouse" "/api/v1/analytics/failures/by-warehouse?warehouseId=2&monthParam=2025-08"

echo "UC4 GET /analytics/failures/compare"
check_get "UC4 compare" "/api/v1/analytics/failures/compare?cityA=Pune&cityB=Mumbai&monthParam=2025-08"

echo "UC5 POST /analytics/insights/query (FESTIVAL_ANALYSIS)"
check_post "UC5 festival" "/api/v1/analytics/insights/query" \
  '{"queryType":"FESTIVAL_ANALYSIS","parameters":{"from":"2025-08-01T00:00:00Z","to":"2025-09-01T00:00:00Z"}}'

echo "UC6 GET /analytics/capacity-projection"
check_get "UC6 capacity" "/api/v1/analytics/capacity-projection?clientId=337&additionalMonthlyOrders=20000"

echo
echo "=== Summary: $pass passed, $fail failed ==="
[[ "$fail" -eq 0 ]]
