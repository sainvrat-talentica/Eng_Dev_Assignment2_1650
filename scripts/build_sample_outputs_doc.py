#!/usr/bin/env python3
"""Build SAMPLE-USE-CASE-OUTPUTS.md from captured live API JSON responses."""
from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "assignment-2-deliverables" / "raw-responses"
OUT = ROOT / "assignment-2-deliverables" / "SAMPLE-USE-CASE-OUTPUTS.md"

USE_CASES = [
    {
        "id": "UC1",
        "title": "Why were deliveries delayed in city X yesterday?",
        "question": "Why were deliveries delayed in Pune on 2025-06-06?",
        "api": "GET /api/v1/analytics/delays?city=Pune&date=2025-06-06",
        "file": "uc1.json",
        "note": "Date chosen from imported sample data (10,000-order subset) where Pune had the highest delay/failure volume.",
    },
    {
        "id": "UC2",
        "title": "Why did Client X's orders fail in the past week?",
        "question": "Why did client 337 (Ramesh-Choudhary) orders fail in 2025?",
        "api": "GET /api/v1/analytics/failures?clientId=337&from=2025-01-01T00:00:00Z&to=2025-12-31T00:00:00Z",
        "file": "uc2.json",
    },
    {
        "id": "UC3",
        "title": "Top reasons for delivery failures linked to Warehouse B in August",
        "question": "Explain failure reasons for Warehouse 2 in August 2025",
        "api": "GET /api/v1/analytics/failures/by-warehouse?warehouseId=2&monthParam=2025-08",
        "file": "uc3.json",
    },
    {
        "id": "UC4",
        "title": "Compare delivery failure causes between City A and City B",
        "question": "Compare Pune vs Mumbai failure causes, August 2025",
        "api": "GET /api/v1/analytics/failures/compare?cityA=Pune&cityB=Mumbai&monthParam=2025-08",
        "file": "uc4.json",
    },
    {
        "id": "UC5",
        "title": "Festival period failures and preparation",
        "question": "What caused failures during festival/holiday periods (Aug 2025)?",
        "api": 'POST /api/v1/analytics/insights/query — queryType FESTIVAL_ANALYSIS',
        "file": "uc5.json",
        "body": '{"queryType":"FESTIVAL_ANALYSIS","parameters":{"from":"2025-08-01","to":"2025-09-01"}}',
    },
    {
        "id": "UC6",
        "title": "Onboard Client Y (+20,000 orders/month) — capacity risk",
        "question": "If client 337 adds 20,000 monthly orders, what risks emerge?",
        "api": "GET /api/v1/analytics/capacity-projection?clientId=337&additionalMonthlyOrders=20000",
        "file": "uc6.json",
    },
]


def load(name: str) -> dict:
    return json.loads((RAW / name).read_text(encoding="utf-8"))


def narrative(data: dict) -> str:
    return data.get("narrative", "")


def recommendations(data: dict) -> list[str]:
    return data.get("recommendations", [])


def evidence_block(data: dict) -> dict:
    skip = {"narrative", "recommendations"}
    return {k: v for k, v in data.items() if k not in skip}


def format_uc(uc: dict) -> str:
    data = load(uc["file"])
    lines = [
        f"## {uc['id']}: {uc['title']}",
        "",
        f"**Business question:** {uc['question']}",
        "",
        f"**API:** `{uc['api']}`",
    ]
    if uc.get("body"):
        lines.extend(["", "**Request body:**", "", "```json", uc["body"], "```"])
    if uc.get("note"):
        lines.extend(["", f"*{uc['note']}*"])
    lines.extend([
        "",
        "### Narrative (human-readable insight)",
        "",
        narrative(data),
        "",
        "### Recommendations",
        "",
    ])
    recs = recommendations(data)
    if recs:
        lines.extend(f"- {r}" for r in recs)
    else:
        lines.append("- _(none generated for this query)_")
    lines.extend([
        "",
        "### Evidence / structured output",
        "",
        "```json",
        json.dumps(evidence_block(data), indent=2),
        "```",
        "",
        "---",
        "",
    ])
    return "\n".join(lines)


def main() -> None:
    recorded = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    header = f"""# Assignment 2 — Recorded Sample Use Case Outputs

**Project:** SwiftEats — Delivery Failure Root-Cause Analytics  
**Recorded:** {recorded}  
**Environment:** Docker (PostgreSQL 15 + Redis 7) + SwiftEats API on `localhost:8080`  
**Dataset:** `third-assignment-sample-data-set/` imported into PostgreSQL schema `analytics.*`  
**Import result:** 10,000 orders, 10,000 warehouse/fleet/feedback/external-factor rows (per-table import cap in demo run)

## How to reproduce

```bash
# From repo root
docker compose up -d postgres redis

# Import CSV (admin key from .env.example)
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \\
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"

# Or enable import on startup:
# ANALYTICS_IMPORT_ON_STARTUP=true docker compose up -d --build
```

Raw JSON responses are saved under [`raw-responses/`](./raw-responses/).

---

"""
    body = "\n".join(format_uc(uc) for uc in USE_CASES)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(header + body, encoding="utf-8")
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()
