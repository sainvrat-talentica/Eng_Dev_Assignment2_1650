#!/usr/bin/env bash
# Reproducible scalability example for Assignment 2 — UC6 capacity projection.
# Demonstrates how the system models onboarding +20,000 monthly orders and flags warehouse risk.
#
# Usage:
#   docker compose up -d
#   curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
#     -H "X-Admin-Api-Key: local-dev-admin-key-change-me"
#   ./scripts/scalability_capacity_demo.sh
#
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
CLIENT_ID="${CLIENT_ID:-337}"
ADMIN_KEY="${ADMIN_API_KEY:-local-dev-admin-key-change-me}"

echo "=== Assignment 2 — Scalability / Capacity Demo (UC6) ==="
echo "API: ${BASE_URL}"
echo "Client: ${CLIENT_ID}"
echo

# Ensure data is loaded (idempotent — safe to re-run)
import_json=$(curl -s -X POST "${BASE_URL}/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: ${ADMIN_KEY}")
import_ms=$(python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('durationMs','n/a'))" <<<"$import_json" 2>/dev/null || echo "n/a")
echo "Import duration: ${import_ms} ms"
echo

run_projection() {
  local label="$1"
  local extra_orders="$2"
  local outfile="/tmp/scalability-uc6-${extra_orders}.json"

  curl -s "${BASE_URL}/api/v1/analytics/capacity-projection?clientId=${CLIENT_ID}&additionalMonthlyOrders=${extra_orders}" \
    > "$outfile"

  python3 - <<PY
import json
from pathlib import Path

data = json.loads(Path("$outfile").read_text())
print(f"--- ${label} (+{extra_orders:,} orders/month) ---")
print(f"Client: {data.get('clientName')} (id={data.get('clientId')})")
print(f"Historical failure rate: {data.get('failureRate', 0):.1%}")
risks = data.get("warehouseRisks") or []
high = [r for r in risks if r.get("highRisk")]
print(f"Warehouses analyzed: {len(risks)} | High-risk (>=80% util): {len(high)}")
for r in sorted(risks, key=lambda x: x.get("projectedUtilizationPercent", 0), reverse=True)[:3]:
    print(
        f"  WH {r['warehouseId']} {r['warehouseName']} ({r['city']}): "
        f"capacity={r['capacity']}, current={r['currentMonthlyOrders']}, "
        f"projected={r['projectedMonthlyOrders']}, util={r['projectedUtilizationPercent']:.1f}%"
        + (" [HIGH RISK]" if r.get("highRisk") else "")
    )
recs = data.get("recommendations") or []
if recs:
    print("Top recommendation:", recs[0])
print()
PY
}

echo "Scenario A — baseline (no additional volume)"
run_projection "Baseline" 0

echo "Scenario B — assignment UC6 (+20,000 orders/month)"
run_projection "UC6 growth" 20000

echo "Scenario C — stress (+50,000 orders/month)"
run_projection "Stress test" 50000

echo "=== Interpretation ==="
echo "• Utilization is projected by distributing new volume across warehouses proportional to current share."
echo "• Warehouses at >=80% projected utilization are flagged highRisk — ops should pre-scale capacity."
echo "• Full JSON saved under /tmp/scalability-uc6-*.json"
echo "• See SCALABILITY_PLAN.md for ingestion/query scaling beyond this capacity model."
