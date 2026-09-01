# Scalability Plan — Assignment 2: Delivery Failure Analytics

**Project:** SwiftEats Delivery Failure Root-Cause Analytics  
**Assignment:** [2026-H1-AI-Assignment-2.txt](./2026-H1-AI-Assignment-2.txt)  
**Version:** 1.0  
**Last updated:** 2026-09-01

---

## 1. Executive summary

Assignment 2 asks for a **sample program** that aggregates siloed logistics data and explains *why* deliveries fail — not a production-scale platform. This plan documents:

1. **Current capacity** of the implemented solution (batch import, indexed PostgreSQL, rule-based correlation).
2. **How we scale operationally** when order volume, clients, or warehouses grow — directly answering **UC6** (*“If we onboard Client Y with ~20,000 extra monthly orders…”*).
3. **A phased evolution path** from the assignment demo to a multi-region analytics platform.

A **reproducible capacity demo** is included in [§6](#6-reproducible-example-uc6-capacity-projection) and [`scripts/scalability_capacity_demo.sh`](./scripts/scalability_capacity_demo.sh).

---

## 2. Scalability dimensions

| Dimension | Assignment need | Current implementation | Scale target (12–18 months) |
|-----------|-----------------|------------------------|----------------------------|
| **Data volume** | Multi-domain CSV (orders, fleet, warehouse, feedback, context) | ~10k orders / demo import; batch size 500 | 10M+ orders/month |
| **Ingestion throughput** | Aggregate once, query many times | Single-threaded JDBC batch insert | Streaming ingest (Kafka) + idempotent loaders |
| **Query latency** | UC1–UC6 ad-hoc ops questions | SQL aggregations + 200-row correlation sample | Sub-second dashboards; pre-aggregated rollups |
| **Concurrent users** | Demo / ops manager | Single `analytics-service` instance | Horizontally scaled read API + cache |
| **Business growth** | UC6 — new client +20k orders/month | `projectCapacityRisk()` warehouse utilization model | Automated capacity alerts + what-if API |

---

## 3. Current architecture baseline

```
┌─────────────┐     batch (500 rows)      ┌──────────────────┐
│ 8 × CSV     │ ────────────────────────► │ PostgreSQL       │
│ sample-data │   SampleDataImportService │ analytics.*      │
└─────────────┘                           │ + B-tree indexes │
                                          └────────┬─────────┘
                                                   │ SQL JOINs
                                          ┌────────▼─────────┐
                                          │ AnalyticsQuery   │
                                          │ Service          │
                                          │ CorrelationEngine│
                                          │ (200-row sample) │
                                          └────────┬─────────┘
                                                   │ REST JSON
                                          ┌────────▼─────────┐
                                          │ analytics-service│
                                          │ :8080            │
                                          └──────────────────┘
```

### 3.1 What already scales reasonably

| Mechanism | Location | Effect |
|-----------|----------|--------|
| **Batch JDBC inserts** | `SampleDataImportService.batchInsert()` | Reduces round-trips; default `batch-size: 500` |
| **Indexed query paths** | `db/migration/V2__analytics_schema.sql` | Indexes on `city`, `client_id`, `order_date`, `failure_reason`, log `order_id` |
| **Dedicated analytics schema** | `analytics.*` | Isolates read workload from operational OLTP |
| **Deterministic correlation** | `CorrelationEngine` | O(1) per order; no external LLM latency |
| **Bounded correlation sample** | `CORRELATION_SAMPLE_LIMIT = 200` | Caps narrative generation cost on large result sets |
| **Import lock** | `ImportLock` | Prevents concurrent full reloads corrupting state |

### 3.2 Known limits (honest assessment)

| Limit | Symptom at scale | Mitigation (see §4–§5) |
|-------|------------------|------------------------|
| Full CSV read into memory | OOM on multi-GB files | Streaming CSV parser + chunked commits |
| Single import thread | Long reload windows | Parallel table loaders; incremental CDC |
| Synchronous REST import | HTTP timeout on huge datasets | Async job queue + status endpoint |
| Correlation on 200-row sample | Narrative may miss rare patterns | Stratified sampling or pre-computed rule hits |
| Single Postgres instance | Write/read contention | Read replicas; monthly partition tables |
| No result cache | Repeated UC queries hit DB | Redis cache keyed by query parameters |

---

## 4. Scaling strategy by layer

### 4.1 Ingestion (aggregate multi-domain data)

**Today:** Admin `POST /api/v1/admin/analytics/import` loads all eight CSVs in dependency order (clients → warehouses → drivers → orders → logs).

**Phase 1 — Optimize batch path (weeks)**

- Tune `swifteats.analytics.batch-size` (500 → 1,000–2,000) per hardware.
- Use `COPY ... FROM STDIN` for bulk load (5–10× faster than batched INSERT for millions of rows).
- Disable synchronous commit during bulk load windows (ops-controlled maintenance).

**Phase 2 — Incremental ingest (months)**

```
Source systems          Message bus              Analytics store
─────────────          ─────────────            ───────────────
Order service    ──►   Kafka topic: orders  ──►  streaming loader
WMS / warehouse  ──►   Kafka topic: wh_log  ──►  (Flink / Spark /
Fleet telematics ──►   Kafka topic: fleet       Spring Kafka batch)
```

- **Idempotency key:** `order_id` + `event_type` + `source_timestamp`.
- **Late-arriving data:** Upsert into `analytics.*`; refresh daily rollups.

**Phase 3 — Multi-tenant isolation**

- Schema-per-client or `client_id` partition pruning for enterprise B2B clients.

### 4.2 Storage (PostgreSQL)

**Indexing (already in place):**

```sql
-- analytics."order" — supports UC1 (city+date), UC2 (client), UC4 (city compare)
CREATE INDEX idx_sample_order_city   ON analytics."order"(city);
CREATE INDEX idx_sample_order_client ON analytics."order"(client_id);
CREATE INDEX idx_sample_order_dates  ON analytics."order"(order_date);
```

**Recommended additions at 1M+ orders:**

```sql
-- Monthly partition example (orders)
CREATE TABLE analytics.order_2025_08 PARTITION OF analytics."order"
  FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

-- Pre-aggregated failure counts for UC3/UC4
CREATE MATERIALIZED VIEW analytics.mv_failure_by_warehouse_month AS
SELECT warehouse_id, date_trunc('month', o.order_date) AS month,
       o.failure_reason, COUNT(*) AS cnt
FROM analytics."order" o
JOIN analytics.warehouse_log wl ON wl.order_id = o.order_id
WHERE o.is_failed
GROUP BY 1, 2, 3;

CREATE UNIQUE INDEX ON analytics.mv_failure_by_warehouse_month (warehouse_id, month, failure_reason);
-- Refresh: nightly or after incremental load
REFRESH MATERIALIZED VIEW CONCURRENTLY analytics.mv_failure_by_warehouse_month;
```

**Read scaling:** Route UC1–UC6 read queries to **read replicas**; keep import on primary.

### 4.3 Query & correlation

| Use case | Query pattern | Scale technique |
|----------|---------------|-----------------|
| UC1 Delays by city/date | Filter + aggregate | Index `(city, order_date)`; optional daily rollup table |
| UC2 Client failures | Range scan on `client_id` + dates | Partition by `order_date` |
| UC3 Warehouse month | Join order ↔ warehouse_log | Materialized view per warehouse/month |
| UC4 City compare | Two filtered aggregations | Cache by `(cityA, cityB, month)` |
| UC5 Festival | Event-type filter + time range | Partial index on `external_factor.event_type` |
| UC6 Capacity | Warehouse load + projection | In-memory model (cheap); extend with simulation |

**Correlation engine:** Rules are CPU-cheap. At scale, **pre-compute rule hits at ingest time**:

```sql
ALTER TABLE analytics."order" ADD COLUMN correlation_rules JSONB;
-- Populate during import/stream processing
CREATE INDEX idx_order_correlation_rules ON analytics."order" USING GIN (correlation_rules);
```

Narratives then aggregate pre-tagged rules instead of re-scanning enriched rows.

### 4.4 Application tier

```
                    ┌─────────────────┐
                    │  Load balancer  │
                    └────────┬────────┘
           ┌─────────────────┼─────────────────┐
           ▼                 ▼                 ▼
   analytics-service   analytics-service   analytics-service
   (stateless)         (stateless)         (stateless)
           └─────────────────┬─────────────────┘
                             ▼
                    PostgreSQL (primary + replicas)
                             ▼
                    Redis (query result cache)
```

- **Horizontal scale:** `analytics-service` is stateless; scale pods on CPU/latency.
- **Cache:** Key = hash(queryType, parameters); TTL 5–15 min for ops dashboards.
- **Async import:** `POST /import` → `202 Accepted` + `jobId`; poll `GET /import/{jobId}`.

### 4.5 Insight generation

Template-based `InsightGenerator` scales linearly with **aggregated counts**, not raw row volume. Keep narratives driven by:

- Top-N failure reasons (N ≤ 10)
- Pre-summarized correlation rule frequencies
- Capacity projection outputs (warehouse list per client ≪ order count)

For very large fleets, optional **LLM summarization** runs on *aggregated evidence JSON*, not raw CSV.

---

## 5. Business scalability — UC6 capacity model

Assignment **UC6** is the business-facing scalability question:

> *If we onboard Client Y with ~20,000 extra monthly orders, what new failure risks should we expect and how do we mitigate them?*

### 5.1 How the sample program answers it

`AnalyticsQueryService.projectCapacityRisk()`:

1. Loads historical **failure rate** for the client.
2. Computes each warehouse’s **share** of the client’s current order volume.
3. Distributes `additionalMonthlyOrders` proportionally across warehouses.
4. Calculates **projected utilization** = `projectedOrders / warehouse.capacity`.
5. Flags warehouses with **≥ 80% utilization** as `highRisk`.
6. Returns narrative + recommendations (pre-scale capacity, reroute volume, driver buffer).

This is intentionally **lightweight** — O(warehouses per client), not O(orders) — so it remains fast even when order history grows.

### 5.2 Extending the model

| Input | Extension |
|-------|-----------|
| Seasonality | Multiply projection by festival/holiday factors from UC5 |
| Failure rate | Project `additionalOrders × failureRate` failed deliveries |
| Lead time | “Weeks until 80% util” based on growth slope |
| Mitigation cost | Rank recommendations by cost vs. risk reduction |

---

## 6. Reproducible example — UC6 capacity projection

This section directly addresses the scalability criterion with a **copy-paste demo** you can run after `docker compose up`.

### 6.1 One-command demo

```bash
docker compose up -d

curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"

./scripts/scalability_capacity_demo.sh
```

The script runs three scenarios for client `337`:

| Scenario | `additionalMonthlyOrders` | Purpose |
|----------|---------------------------|---------|
| A — Baseline | 0 | Current warehouse load |
| B — UC6 | 20,000 | Assignment growth case |
| C — Stress | 50,000 | Shows non-linear risk emergence |

### 6.2 Manual curl (single scenario)

```bash
curl -s "http://localhost:8080/api/v1/analytics/capacity-projection?clientId=337&additionalMonthlyOrders=20000" \
  | python3 -m json.tool
```

**Expected response shape:**

```json
{
  "clientId": 337,
  "clientName": "...",
  "additionalMonthlyOrders": 20000,
  "failureRate": 0.08,
  "warehouseRisks": [
    {
      "warehouseId": 2,
      "warehouseName": "...",
      "city": "Pune",
      "capacity": 500,
      "currentMonthlyOrders": 120,
      "projectedMonthlyOrders": 840,
      "projectedUtilizationPercent": 168.0,
      "highRisk": true
    }
  ],
  "narrative": "Onboarding ... 20,000 additional monthly orders ...",
  "recommendations": [
    "Increase capacity or reroute volume at ... — projected 168% utilization.",
    "Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks."
  ]
}
```

### 6.3 Interpreting results for ops

1. **`highRisk: true`** — warehouse projected above 80% capacity; expect SLA breaches and warehouse-delay failures (UC3 pattern).
2. **`failureRate`** — historical client quality; high rates amplify revenue leakage at larger volume.
3. **`recommendations`** — actionable mitigations tied to assignment strategic need #4.

Recorded golden output: [`deliverables/raw-responses/uc6.json`](./deliverables/raw-responses/uc6.json).

### 6.4 Ingestion throughput snapshot

The import API returns `durationMs` — use it to baseline ingest scalability:

```bash
curl -s -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(f\"Rows: {sum(d['rowCounts'].values())}, Duration: {d['durationMs']}ms\")"
```

Typical demo dataset (~65k total rows): **4–10 seconds** on Docker Desktop Postgres. Throughput ≈ **6,500–16,000 rows/sec** with `batch-size: 500`.

---

## 7. Phased roadmap

| Phase | Timeline | Deliverable | Scale target |
|-------|----------|-------------|--------------|
| **0 — Assignment demo** | Now | CSV import + REST UC1–UC6 | 10k orders |
| **1 — Harden batch** | 1–2 months | COPY loader, async import job, metrics | 1M orders |
| **2 — Real-time** | 3–6 months | Kafka ingest, rule pre-compute at write | 100k orders/day |
| **3 — Enterprise** | 6–12 months | Partitioning, read replicas, cache, multi-tenant | 10M+ orders |
| **4 — Predictive** | 12–18 months | ML anomaly detection on top of rule engine | Proactive failure prevention |

---

## 8. Metrics and SLOs

| Metric | Demo baseline | Production SLO |
|--------|---------------|----------------|
| Import duration (full dataset) | < 15 s | < 30 min for 10M rows (async) |
| UC1–UC5 API p95 latency | < 2 s | < 500 ms (cached) / < 2 s (cold) |
| UC6 capacity projection p95 | < 500 ms | < 200 ms |
| Import availability | Single instance | 99.9% (queued jobs, retry) |
| Data freshness | Batch on demand | < 15 min lag (streaming) |

**Observability additions:**

- Prometheus: `analytics_import_duration_seconds`, `analytics_query_duration_seconds{uc=...}`
- Grafana dashboards: import throughput, query latency by UC, warehouse high-risk count over time

---

## 9. Failure modes under load

| Failure mode | Detection | Auto-mitigation |
|--------------|-----------|-----------------|
| Import timeout | `durationMs` spike / HTTP 504 | Chunked async job |
| DB connection exhaustion | Hikari pool metrics | Scale pool + read replicas |
| Hot warehouse in UC6 | `highRisk` count ↑ | Alert ops; trigger capacity playbook |
| Festival surge (UC5) | Event volume threshold | Recommend buffer staffing (already in engine) |
| Stale cache | TTL + version on dataset import | Invalidate cache on import complete |

---

## 10. Design trade-offs (scalability vs. assignment scope)

| Choice | Why (assignment) | Scale implication |
|--------|------------------|-------------------|
| Rule-based correlation | Deterministic, testable, offline demo | Scales linearly; extend with pre-compute |
| Batch CSV vs. streaming | Matches provided dataset | Streaming needed for production freshness |
| 200-row correlation sample | Fast narratives on laptop | Stratified sampling or pre-aggregation at scale |
| Single Postgres | Simple `docker compose` | Partition + replicas when rows > 10M |
| JDBC not JPA in analytics-service | Lighter runtime, no OLTP schema deps | Easier horizontal stateless scaling |

---

## 11. Traceability

| Assignment element | Scalability artifact |
|--------------------|----------------------|
| Aggregate multi-domain data | §4.1 ingestion phases, batch-size tuning |
| Correlate at scale | §4.3 pre-computed rules, bounded samples |
| UC6 (+20k orders) | §5, §6, `scripts/scalability_capacity_demo.sh` |
| Actionable recommendations | Capacity recommendations in UC6 response |
| Demo from local system | Reproducible script + `deliverables/raw-responses/uc6.json` |

---

## 12. References

| Document | Path |
|----------|------|
| Assignment brief | `2026-H1-AI-Assignment-2.txt` |
| Test strategy | `TEST_STRATEGY.md` |
| Architecture diagram | `deliverables/architecture-diagram.png` |
| UC6 recorded output | `deliverables/raw-responses/uc6.json` |
| Capacity demo script | `scripts/scalability_capacity_demo.sh` |
| Import configuration | `platform-lib/src/main/resources/application-platform-defaults.yml` (`batch-size: 500`) |
