# Assignment 2 — Focused Demo Script (8–10 min)

Record this walkthrough with voice-over. The assignment asks for a **simple program** that aggregates data and demos outcomes — emphasize the **pipeline**, not the optional UI.

## Demo story arc (say this upfront)

> "Operations see failure counts but not root causes. Our sample program: **import multi-domain CSV → join on order_id → rule-based correlation → template narrative → recommendations**. All queries go through the **analytics REST API on port 8080**."

```
CSV files → POST /admin/analytics/import → PostgreSQL analytics.*
         → REST query (UC1–UC6) → CorrelationEngine → InsightGenerator
         → JSON: narrative + recommendations + evidence
```

---

## Option A — REST API (recommended for video)

### Setup (before recording)

From the repository root:

```bash
docker compose up --build -d
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"
```

Verify all endpoints: `./scripts/verify_analytics_gateway.sh`

### 1. Problem & architecture (1 min)

- Siloed data: orders, warehouse logs, fleet GPS notes, feedback, traffic/weather.
- Show `sample-data/` (8 CSV files).
- Mention `CorrelationEngine` rules (traffic triple-confirm, stockout+warehouse, festival volume, etc.).
- Optional: show `deliverables/architecture-diagram.png` from the write-up.

### 2. Import — aggregation step (1 min)

```bash
curl -s -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me" | python3 -m json.tool
```

- Point out `rowCounts`: orders, warehouse_log, fleet_log, feedback, external_factor.
- **Say:** "This is the aggregate step — all domains land in `analytics.*` schema keyed by `order_id`."

### 3–8. Use cases — correlation → narrative → recommendations (6 min)

For **each** UC, show the curl output and read aloud:
1. **`narrative`** — human-readable insight  
2. **`recommendations`** — actionable ops changes  
3. One field from **evidence** — proof of correlation  

| UC | API URL (localhost:8080) |
|----|--------------------------|
| UC1 | `GET /api/v1/analytics/delays?city=Pune&date=2025-06-06` |
| UC2 | `GET /api/v1/analytics/failures?clientId=337&from=2025-01-01T00:00:00Z&to=2025-12-31T00:00:00Z` |
| UC3 | `GET /api/v1/analytics/failures/by-warehouse?warehouseId=2&monthParam=2025-08` |
| UC4 | `GET /api/v1/analytics/failures/compare?cityA=Pune&cityB=Mumbai&monthParam=2025-08` |
| UC5 | `POST /api/v1/analytics/insights/query` body: `{"queryType":"FESTIVAL_ANALYSIS","parameters":{"from":"2025-08-01T00:00:00Z","to":"2025-09-01T00:00:00Z"}}` |
| UC6 | `GET /api/v1/analytics/capacity-projection?clientId=337&additionalMonthlyOrders=20000` |

**UC1 example:**

```bash
curl -s "http://localhost:8080/api/v1/analytics/delays?city=Pune&date=2025-06-06" | python3 -m json.tool
```

### 9. Wrap-up (1 min)

- Rule-based (deterministic, testable) — not a black-box LLM.
- Deliverables: `deliverables/ASSIGNMENT-2-WRITEUP.docx`, `deliverables/SAMPLE-USE-CASE-OUTPUTS.docx`, GitHub repo.
- Optional: analytics dashboard at http://localhost:3002 (same APIs).

---

## Option B — CLI demo runner (no curl)

Runs all six use cases in **analytics-service** logs on startup:

```bash
docker compose stop analytics-service
ANALYTICS_IMPORT_ON_STARTUP=true ANALYTICS_DEMO_ON_STARTUP=true \
  docker compose up analytics-service
```

In another terminal:

```bash
docker compose logs -f analytics-service
```

Look for log blocks:

```
=== SwiftEats Assignment 2 Analytics Demo ===
--- UC1 — Delays in Pune yesterday ---
Narrative: ...
Recommendations: ...
Evidence: ...
```

**Say on camera:** "This is the same query engine as the REST API — `AnalyticsDemoRunner` invokes `AnalyticsQueryService` directly after CSV import."

---

## Option C — Analytics dashboard (optional, 30 sec)

1. Open http://localhost:3002  
2. Run one query (e.g. city delay or festival analysis).  
3. **Say:** "The UI calls the same analytics APIs — the sample program is the backend analytics module."

---

## Pre-recording checklist

- [ ] `./scripts/verify_analytics_gateway.sh` — 6/6 pass  
- [ ] `python3 scripts/refresh_analytics_outputs.py` — outputs current  
- [ ] Microphone tested  
- [ ] Terminal font size readable on video  

## Upload checklist

- [ ] MP4 with voice (8–10 min)  
- [ ] OneDrive folder: write-up `.docx`, sample outputs `.docx`, video  
- [ ] Email via `SUBMISSION-EMAIL-TEMPLATE.md`
