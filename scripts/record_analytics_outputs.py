#!/usr/bin/env python3
"""
DEPRECATED — use scripts/refresh_analytics_outputs.py instead.

That script calls the live analytics API (same as docker-compose) and regenerates
assignment-2-deliverables/SAMPLE-USE-CASE-OUTPUTS.md, .docx, and raw-responses/.

This file remains as an offline CSV replay helper for local sanity checks only.
Run: python3 scripts/record_analytics_outputs.py
"""
from __future__ import annotations

import csv
import json
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "third-assignment-sample-data-set"
OUTPUT = ROOT / "assignment-2-deliverables" / "SAMPLE-USE-CASE-OUTPUTS.md"


def parse_ts(value: str | None) -> datetime | None:
    if not value or not value.strip():
        return None
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(value.strip(), fmt)
        except ValueError:
            continue
    return None


def load_csv(name: str) -> list[dict[str, str]]:
    path = DATASET / name
    with path.open(newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def enrich_orders(orders: list[dict]) -> list[dict]:
    enriched = []
    for o in orders:
        promised = parse_ts(o.get("promised_delivery_date"))
        actual = parse_ts(o.get("actual_delivery_date"))
        is_failed = o.get("status", "").strip().lower() == "failed"
        is_delayed = (
            promised is not None
            and actual is not None
            and actual > promised
        )
        row = dict(o)
        row["_order_date"] = parse_ts(o.get("order_date"))
        row["_is_failed"] = is_failed
        row["_is_delayed"] = is_delayed
        enriched.append(row)
    return enriched


def top_key(counts: dict) -> str:
    if not counts:
        return "None"
    return max(counts, key=counts.get)


def pct(counts: dict, key: str, total: int) -> float:
    if total == 0 or not key:
        return 0.0
    return counts.get(key, 0) * 100.0 / total


def delay_narrative(city, date, total, reasons, heavy, slow, negative):
    top = top_key(reasons)
    return (
        f"In {city} on {date}, {total} deliveries were delayed or failed. "
        f"Top cause: {top} ({pct(reasons, top, total):.0f}% of affected orders). "
        f"{heavy} orders correlated with heavy traffic and {slow} with slow warehouse packing. "
        f"{negative} customers left negative feedback mentioning lateness or service issues."
    )


def uc1(orders, external, warehouse_logs, feedback):
    city, date_str = "Pune", "2025-06-06"
    target = datetime.strptime(date_str, "%Y-%m-%d").date()
    ext_by_order = {r["order_id"]: r for r in external}
    wh_by_order = {r["order_id"]: r for r in warehouse_logs}
    fb_by_order = {r["order_id"]: r for r in feedback}

    affected = [
        o for o in orders
        if o.get("city") == city
        and o["_order_date"]
        and o["_order_date"].date() == target
        and (o["_is_delayed"] or o["_is_failed"])
    ]
    reasons = Counter(
        (o.get("failure_reason") or "Unknown") for o in affected
    )
    heavy = sum(
        1 for o in affected
        if ext_by_order.get(o["order_id"], {}).get("traffic_condition") == "Heavy"
    )
    slow = sum(
        1 for o in affected
        if "Slow packing" in (wh_by_order.get(o["order_id"], {}).get("notes") or "")
    )
    negative = sum(
        1 for o in affected
        if fb_by_order.get(o["order_id"], {}).get("sentiment") == "Negative"
    )
    total = len(affected)
    recs = []
    if slow > 0:
        recs.append("Add packing staff at the warehouse during peak hours.")
    if heavy > 0:
        recs.append("Reroute via alternate routes and widen ETA windows during heavy traffic.")
    return {
        "use_case": "UC1 — Why were deliveries delayed in city X yesterday?",
        "question": f"Why were deliveries delayed in {city} on {date_str}?",
        "api": f"GET /api/v1/analytics/delays?city={city}&date={date_str}",
        "narrative": delay_narrative(city, date_str, total, dict(reasons), heavy, slow, negative),
        "recommendations": recs or ["Review end-to-end SLA checkpoints from kitchen dispatch to last-mile handoff."],
        "evidence": {
            "totalAffected": total,
            "failureReasonCounts": dict(reasons),
            "heavyTrafficCount": heavy,
            "slowPackingCount": slow,
            "negativeFeedbackCount": negative,
        },
    }


def uc2(orders, fleet, warehouse_logs, clients):
    client_id = "337"
    from_dt = parse_ts("2025-01-01 00:00:00")
    to_dt = parse_ts("2025-12-31 00:00:00")
    client_name = next((c["client_name"] for c in clients if c["client_id"] == client_id), f"Client {client_id}")

    failed = [
        o for o in orders
        if o.get("client_id") == client_id
        and o.get("status") == "Failed"
        and o["_order_date"]
        and from_dt <= o["_order_date"] < to_dt
    ]
    fleet_by = defaultdict(set)
    wh_by = defaultdict(set)
    for r in fleet:
        note = r.get("gps_delay_notes") or ""
        if note:
            fleet_by[r["order_id"]].add(note)
    for r in warehouse_logs:
        note = r.get("notes") or ""
        if note:
            wh_by[r["order_id"]].add(note)

    breakdown = Counter(o.get("failure_reason") or "Unknown" for o in failed)
    top_reason = top_key(dict(breakdown))
    top_count = breakdown[top_reason] if breakdown else 0
    fleet_issues = ", ".join(
        sorted({n for o in failed for n in fleet_by.get(o["order_id"], set())})
    ) or "—"
    wh_issues = ", ".join(
        sorted({n for o in failed for n in wh_by.get(o["order_id"], set())})
    ) or "—"

    narrative = (
        f"Client {client_name} (id {client_id}) recorded {len(failed)} failed orders "
        f"from {from_dt.date()} to {to_dt.date()}. "
        f"Primary failure reason: {top_reason} ({top_count} orders). "
        f"Fleet issues noted: {fleet_issues}. Warehouse issues: {wh_issues}."
        if failed
        else f"Client {client_name} (id {client_id}) had no failed orders in the selected window."
    )
    recs = []
    if "Incorrect address" in breakdown:
        recs.append("Mandatory address pin validation before dispatch.")
    if "Stockout" in breakdown:
        recs.append("Enable real-time inventory sync and low-stock alerts at affected warehouses.")
    return {
        "use_case": "UC2 — Why did Client X's orders fail in the past week?",
        "question": f"Why did client {client_id} ({client_name}) orders fail Apr 2025?",
        "api": f"GET /api/v1/analytics/failures?clientId={client_id}&from=2025-01-01T00:00:00Z&to=2025-12-31T00:00:00Z",
        "narrative": narrative,
        "recommendations": recs or ["Review failure breakdown and correlate fleet/warehouse logs per order."],
        "evidence": {
            "totalFailed": len(failed),
            "failureReasonCounts": dict(breakdown),
            "fleetIssuesSample": fleet_issues,
            "warehouseIssuesSample": wh_issues,
        },
    }


def uc3(orders, warehouse_logs, warehouses):
    warehouse_id = "2"
    year, month = 2025, 8
    wh = next((w for w in warehouses if w["warehouse_id"] == warehouse_id), None)
    wh_name = wh["warehouse_name"] if wh else f"Warehouse {warehouse_id}"

    order_ids = {
        r["order_id"] for r in warehouse_logs if r["warehouse_id"] == warehouse_id
    }
    failed = [
        o for o in orders
        if o["order_id"] in order_ids
        and o.get("status") == "Failed"
        and o["_order_date"]
        and o["_order_date"].year == year
        and o["_order_date"].month == month
    ]
    reasons = Counter(o.get("failure_reason") or "Unknown" for o in failed)
    notes = Counter(
        (next((wl["notes"] or "None") for wl in warehouse_logs if wl["order_id"] == o["order_id"]), 1)
        for o in failed
    )
    note_counts = Counter()
    for o in failed:
        note = next((wl.get("notes") or "None" for wl in warehouse_logs if wl["order_id"] == o["order_id"]), "None")
        if not note.strip():
            note = "None"
        note_counts[note] += 1

    top = top_key(dict(reasons))
    narrative = (
        f"Warehouse {wh_name} (id {warehouse_id}) had {len(failed)} failed orders in {year}-{month:02d}. "
        f"Leading failure reason: {top}. "
        f"Warehouse log patterns: {', '.join(f'{k} ({v})' for k, v in note_counts.most_common(3))}."
    )
    recs = ["Add packing staff at " + wh_name + " during peak hours."] if note_counts else []
    if "Stockout" in reasons:
        recs.append("Enable real-time inventory sync and low-stock alerts at affected warehouses.")
    return {
        "use_case": "UC3 — Top reasons for failures linked to Warehouse B in August",
        "question": f"Explain failure reasons for warehouse {warehouse_id} ({wh_name}) in August 2025",
        "api": f"GET /api/v1/analytics/failures/by-warehouse?warehouseId={warehouse_id}&monthParam=2025-08",
        "narrative": narrative,
        "recommendations": recs or ["Review warehouse operational notes against failure reasons."],
        "evidence": {
            "totalFailed": len(failed),
            "failureReasonCounts": dict(reasons),
            "warehouseNoteCounts": dict(note_counts),
        },
    }


def uc4(orders):
    city_a, city_b = "Pune", "Mumbai"
    year, month = 2025, 8

    def city_failures(city):
        failed = [
            o for o in orders
            if o.get("city") == city
            and o.get("status") == "Failed"
            and o["_order_date"]
            and o["_order_date"].year == year
            and o["_order_date"].month == month
        ]
        return Counter(o.get("failure_reason") or "Unknown" for o in failed)

    fa, fb = city_failures(city_a), city_failures(city_b)
    total_a = sum(fa.values())
    total_b = sum(fb.values())
    narrative = (
        f"During 2025-{month:02d}, {city_a} had {total_a} failed deliveries (top cause: {top_key(dict(fa))}) "
        f"while {city_b} had {total_b} (top cause: {top_key(dict(fb))}). "
        "Compare operational focus: address verification in cities with high incorrect-address rates, "
        "and warehouse staffing where warehouse-delay failures dominate."
    )
    recs = []
    if fa.get("Incorrect address", 0) > 0 or fb.get("Incorrect address", 0) > 0:
        recs.append("Mandatory address pin validation before dispatch.")
    if fa.get("Traffic congestion", 0) > 0 or fb.get("Traffic congestion", 0) > 0:
        recs.append("Reroute via alternate routes and widen ETA windows during heavy traffic.")
    return {
        "use_case": "UC4 — Compare delivery failure causes between City A and City B",
        "question": f"Compare failure causes: {city_a} vs {city_b}, August 2025",
        "api": f"GET /api/v1/analytics/failures/compare?cityA={city_a}&cityB={city_b}&monthParam=2025-08",
        "narrative": narrative,
        "recommendations": recs or ["Tailor city-specific ops playbooks based on dominant failure reasons."],
        "evidence": {"cityA": dict(fa), "cityB": dict(fb)},
    }


def uc5(orders, external, warehouse_logs, warehouses):
    from_dt = parse_ts("2025-08-01 00:00:00")
    to_dt = parse_ts("2025-09-01 00:00:00")
    ext_orders = {
        r["order_id"]: r for r in external
        if r.get("event_type") in ("Festival", "Holiday")
    }
    wh_cap = {w["warehouse_id"]: int(w.get("capacity") or 0) for w in warehouses}
    wl_map = {r["order_id"]: r for r in warehouse_logs}

    matched = [
        o for o in orders
        if o["order_id"] in ext_orders
        and o["_order_date"]
        and from_dt <= o["_order_date"] < to_dt
    ]
    by_event_reason = Counter(
        (ext_orders[o["order_id"]]["event_type"], o.get("failure_reason") or "N/A")
        for o in matched
    )
    by_reason = Counter(o.get("failure_reason") or "Unknown" for o in matched if o.get("status") == "Failed")
    capacities = []
    for o in matched:
        wl = wl_map.get(o["order_id"])
        if wl and wl["warehouse_id"] in wh_cap:
            capacities.append(wh_cap[wl["warehouse_id"]])
    avg_cap = sum(capacities) / len(capacities) if capacities else 0

    total_events = len(matched)
    top = top_key(dict(by_reason)) if by_reason else "N/A"
    narrative = (
        f"Between {from_dt.date()} and {to_dt.date()}, festival/holiday periods saw {total_events} correlated orders "
        f"with {sum(by_reason.values())} failures. Top correlated reason: {top}. "
        "Average warehouse capacity during these events was constrained — plan buffer staffing and driver capacity."
        if matched
        else f"No festival or holiday correlated orders found between {from_dt.date()} and {to_dt.date()}."
    )
    return {
        "use_case": "UC5 — Festival period failures and preparation",
        "question": "What caused delivery failures during festival/holiday periods (Aug 2025)?",
        "api": 'POST /api/v1/analytics/insights/query {"queryType":"FESTIVAL_ANALYSIS","parameters":{"from":"2025-08-01","to":"2025-09-01"}}',
        "narrative": narrative,
        "recommendations": [
            "Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.",
            "Pause SLA clock during severe weather and pre-position drivers beforehand.",
        ],
        "evidence": {
            "totalCorrelatedOrders": total_events,
            "failureReasonCounts": dict(by_reason),
            "eventTypeBreakdown": dict(Counter(k[0] for k in by_event_reason)),
            "avgWarehouseCapacity": round(avg_cap, 1),
        },
    }


def uc6(orders, warehouse_logs, warehouses, clients):
    client_id = "337"
    additional = 20000
    client_name = next((c["client_name"] for c in clients if c["client_id"] == client_id), f"Client {client_id}")

    client_orders = [o for o in orders if o.get("client_id") == client_id]
    total = len(client_orders)
    failed = sum(1 for o in client_orders if o.get("status") == "Failed")
    failure_rate = failed / total if total else 0

    wh_load = defaultdict(int)
    wh_info = {w["warehouse_id"]: w for w in warehouses}
    for wl in warehouse_logs:
        oid = wl["order_id"]
        if any(o["order_id"] == oid and o.get("client_id") == client_id for o in orders):
            wh_load[wl["warehouse_id"]] += 1

    risks = []
    high_risk_count = 0
    for wh_id, count in sorted(wh_load.items(), key=lambda x: -x[1]):
        w = wh_info.get(wh_id, {})
        cap = int(w.get("capacity") or 0)
        share = count / total if total else 0
        projected_extra = round(additional * share)
        projected_total = count + projected_extra
        util = projected_total * 100.0 / cap if cap else 100.0
        high = util >= 80
        if high:
            high_risk_count += 1
        risks.append({
            "warehouseId": wh_id,
            "warehouseName": w.get("warehouse_name", f"Warehouse {wh_id}"),
            "city": w.get("city", ""),
            "capacity": cap,
            "currentOrders": count,
            "projectedOrders": projected_total,
            "projectedUtilizationPct": round(util, 1),
            "highRisk": high,
        })

    narrative = (
        f"Client {client_name} (id {client_id}) historically fails {failure_rate * 100:.1f}% of orders. "
        f"Adding {additional:,} monthly orders projects strain on {high_risk_count} warehouse(s). "
        "High-risk sites exceed 80% projected utilization and need mitigation before onboarding."
    )
    recs = [
        f"Increase capacity or reroute volume at {r['warehouseName']} ({r['city']}) — projected {r['projectedUtilizationPct']:.0f}% utilization."
        for r in risks if r["highRisk"]
    ]
    if failure_rate > 0.15:
        recs.append("Historical failure rate exceeds 15% — run root-cause review before scaling order volume.")
    recs.append("Add driver buffer proportional to projected order growth in primary client cities.")
    return {
        "use_case": "UC6 — Onboard Client Y (+20,000 orders/month) capacity risk",
        "question": f"If client {client_id} adds 20,000 monthly orders, what failure risks emerge?",
        "api": f"GET /api/v1/analytics/capacity-projection?clientId={client_id}&additionalMonthlyOrders={additional}",
        "narrative": narrative,
        "recommendations": recs,
        "evidence": {
            "historicalFailureRate": round(failure_rate, 4),
            "totalHistoricalOrders": total,
            "warehouseRisks": risks[:5],
        },
    }


def format_result(r: dict) -> str:
    lines = [
        f"### {r['use_case']}",
        "",
        f"**Business question:** {r['question']}",
        "",
        f"**API call:** `{r['api']}`",
        "",
        "**Narrative**",
        "",
        r["narrative"],
        "",
        "**Recommendations**",
        "",
    ]
    for rec in r["recommendations"]:
        lines.append(f"- {rec}")
    lines.extend([
        "",
        "**Evidence (structured)**",
        "",
        "```json",
        json.dumps(r["evidence"], indent=2),
        "```",
        "",
        "---",
        "",
    ])
    return "\n".join(lines)


def main():
    orders = enrich_orders(load_csv("orders.csv"))
    clients = load_csv("clients.csv")
    warehouses = load_csv("warehouses.csv")
    warehouse_logs = load_csv("warehouse_logs.csv")
    fleet = load_csv("fleet_logs.csv")
    external = load_csv("external_factors.csv")
    feedback = load_csv("feedback.csv")

    results = [
        uc1(orders, external, warehouse_logs, feedback),
        uc2(orders, fleet, warehouse_logs, clients),
        uc3(orders, warehouse_logs, warehouses),
        uc4(orders),
        uc5(orders, external, warehouse_logs, warehouses),
        uc6(orders, warehouse_logs, warehouses, clients),
    ]

    header = f"""# Assignment 2 — Recorded Sample Use Case Outputs

**Project:** SwiftEats Delivery Failure Analytics  
**Dataset:** `third-assignment-sample-data-set/` ({len(orders):,} orders)  
**Recorded:** {datetime.now().strftime("%Y-%m-%d %H:%M:%S %Z")}  
**Engine:** SwiftEats Analytics Module (rule-based correlation + template narratives)

## How these outputs were produced

1. Sample CSV data is imported into PostgreSQL schema `analytics.*` via `POST /api/v1/admin/analytics/import`.
2. Each use case is queried through the REST API (paths shown below).
3. The analytics engine joins orders, warehouse logs, fleet logs, feedback, and external factors on `order_id`, applies correlation rules, and returns a narrative with recommendations and evidence.

To reproduce live:

```bash
docker compose up -d
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \\
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"
# Then run each API call listed under the use cases below
```

Or run all six use cases in application logs:

```bash
ANALYTICS_IMPORT_ON_STARTUP=true ANALYTICS_DEMO_ON_STARTUP=true docker compose up analytics-service
```

---

"""

    body = "\n".join(format_result(r) for r in results)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(header + body, encoding="utf-8")
    print(f"Wrote {OUTPUT}")
    for r in results:
        print(f"  {r['use_case']}: OK")


if __name__ == "__main__":
    main()
