# Test Strategy — Assignment 2: Delivery Failure Analytics

**Project:** SwiftEats Delivery Failure Root-Cause Analytics  
**Assignment:** [2026-H1-AI-Assignment-2.txt](./2026-H1-AI-Assignment-2.txt)  
**Version:** 1.0  
**Last updated:** 2026-09-01

---

## 1. Purpose and scope

This document defines how we test the Assignment 2 solution: a sample program that **aggregates multi-domain logistics data**, **correlates events automatically**, and **produces human-readable insights with actionable recommendations**.

### 1.1 Business capabilities under test

| Strategic need (assignment) | System capability | Primary test focus |
|----------------------------|-------------------|-------------------|
| Aggregate multi-domain data | CSV import → PostgreSQL `analytics.*` schema | Data completeness, referential integrity, derived flags |
| Correlate events automatically | `CorrelationEngine` rule matching on enriched orders | Rule accuracy, confidence scoring, edge cases |
| Generate human-readable insights | `InsightGenerator` narrative templates | Narrative structure, counts, top causes |
| Surface actionable recommendations | Recommendation lists per query type | Presence, relevance, ops-facing language |

### 1.2 In scope

- `analytics-lib` — import pipeline, SQL repository, correlation engine, insight generation, REST controllers
- `analytics-service` — Spring Boot runtime, configuration, Docker deployment
- Assignment sample use cases UC1–UC6 (see §5)
- Verification scripts and recorded outputs under `deliverables/`

### 1.3 Out of scope (for this assignment)

- Full Assignment 1 operational platform (order, payment, tracking microservices)
- `platform-lib` business logic beyond shared security/config used by analytics-service
- Optional `dashboard/` UI (smoke-tested manually only)
- ML/LLM-based inference (solution uses deterministic rule-based correlation)
- Performance/load testing at production scale

---

## 2. Test objectives

1. **Correctness** — Each use case returns a coherent `narrative`, `recommendations`, and supporting `evidence` grounded in imported data.
2. **Data integrity** — All eight CSV domains load into `analytics.*` with expected row counts and join keys (`order_id`, `client_id`, `warehouse_id`, `driver_id`).
3. **Correlation fidelity** — Known multi-signal patterns (e.g. traffic triple-confirm, stockout + warehouse note) fire the intended rules.
4. **API contract** — REST endpoints accept valid parameters, reject bad input, and enforce admin auth on import.
5. **Reproducibility** — Demo, recorded outputs, and CI-friendly commands produce the same outcomes on a clean environment.
6. **Regression safety** — Unit tests guard core logic; integration tests guard import + query against real PostgreSQL.

---

## 3. Test pyramid

```
                    ┌─────────────────────┐
                    │  Acceptance / Demo  │  UC1–UC6 via live API + recorded outputs
                    │  (scripts, manual)  │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │    Integration      │  Testcontainers import test, repository SQL
                    │  (analytics-lib)    │
                    └──────────┬──────────┘
                               │
         ┌─────────────────────▼─────────────────────┐
         │              Unit tests                    │
         │  CorrelationEngine, InsightGenerator,      │
         │  CsvParsingUtils, AnalyticsQueryService,   │
         │  AnalyticsOrderRepository (mocked JDBC),   │
         │  controllers (MockMvc), ImportLock         │
         └────────────────────────────────────────────┘
```

| Layer | Tooling | Location | Approx. count |
|-------|---------|----------|---------------|
| Unit | JUnit 5, Mockito, AssertJ | `analytics-lib/src/test/java` | ~90 tests |
| Integration | Spring Boot Test, Testcontainers PostgreSQL | `SampleDataImportServiceIntegrationTest` | 1 (Docker required) |
| Acceptance | Bash + curl, Python refresh scripts | `scripts/verify_analytics_gateway.sh`, `scripts/refresh_analytics_outputs.py` | 6 use cases + import |
| Manual / demo | Docker Compose, voice-over walkthrough | `deliverables/DEMO-SCRIPT.md` | UC1–UC6 |

`platform-lib` ships with its own unit tests (vendored shared infrastructure). They are **not** part of Assignment 2 acceptance criteria but run when building the full Maven reactor.

---

## 4. Test environment

### 4.1 Local development

| Component | Version / notes |
|-----------|-----------------|
| Java | 17 |
| Maven | 3.9+ |
| PostgreSQL | 15 (Testcontainers or Docker Compose) |
| Docker | Required for Compose stack and optional integration test |

### 4.2 Docker Compose stack (recommended for demo and acceptance)

```bash
docker compose up --build
```

| Service | Port | Role in testing |
|---------|------|-----------------|
| `postgres` | 5433 (host) | Persistent `analytics.*` schema via Flyway |
| `db-migrate` | — | Applies `db/migration/V2__analytics_schema.sql` |
| `analytics-service` | 8080 | REST API under test |
| `analytics-dashboard` | 3002 | Optional UI smoke test |

### 4.3 Test data

**Primary dataset:** `sample-data/` (eight CSV files from the assignment dataset)

| File | Target table | Join key |
|------|--------------|----------|
| `clients.csv` | `analytics.client` | `client_id` |
| `warehouses.csv` | `analytics.warehouse` | `warehouse_id` |
| `drivers.csv` | `analytics.driver` | `driver_id` |
| `orders.csv` | `analytics.order` | `order_id` |
| `warehouse_logs.csv` | `analytics.warehouse_log` | `order_id` |
| `fleet_logs.csv` | `analytics.fleet_log` | `order_id` |
| `feedback.csv` | `analytics.feedback` | `order_id` |
| `external_factors.csv` | `analytics.external_factor` | `order_id` |

**Derived columns at import:** `is_delayed`, `is_failed` on `analytics.order` — validated in integration and repository tests.

**Fixture data for unit tests:** In-memory `EnrichedOrder` builders and mocked JDBC rows in `AnalyticsOrderRepositoryTest`; no CSV required.

---

## 5. Use-case test mapping

Each assignment sample use case maps to an API endpoint, automated check, and expected response shape.

| UC | Question | API | Automated check |
|----|----------|-----|-----------------|
| **UC1** | Why were deliveries delayed in city X yesterday? | `GET /api/v1/analytics/delays?city={city}&date={date}` | `verify_analytics_gateway.sh` — `narrative` or `totalAffected` |
| **UC2** | Why did Client X's orders fail in the past week? | `GET /api/v1/analytics/failures?clientId={id}&from=&to=` | `totalFailed`, failure breakdown |
| **UC3** | Top failure reasons for Warehouse B in August? | `GET /api/v1/analytics/failures/by-warehouse?warehouseId={id}&monthParam=yyyy-MM` | `narrative`, ranked reasons |
| **UC4** | Compare failure causes City A vs City B last month? | `GET /api/v1/analytics/failures/compare?cityA=&cityB=&monthParam=` | comparative `evidence` |
| **UC5** | Festival-period failures and preparation? | `POST /api/v1/analytics/insights/query` (`FESTIVAL_ANALYSIS`) | `narrative`, festival breakdown |
| **UC6** | Client Y +20k orders — new failure risks? | `GET /api/v1/analytics/capacity-projection?clientId=&additionalMonthlyOrders=20000` | `warehouseRisks`, recommendations |

**Recorded golden outputs:** `deliverables/SAMPLE-USE-CASE-OUTPUTS.md` and `deliverables/raw-responses/` — refreshed via:

```bash
python3 scripts/refresh_analytics_outputs.py
```

---

## 6. Component-level test plan

### 6.1 Data import (`SampleDataImportService`)

| Test | Type | What we verify |
|------|------|----------------|
| `CsvParsingUtilsTest` | Unit | Date/decimal parsing, delay/failure flag derivation |
| `SampleDataImportServiceTest` | Unit | Path resolution, lock interaction, error on missing directory |
| `SampleDataImportServiceIntegrationTest` | Integration | Full import into PostgreSQL; row counts; `is_failed` / `is_delayed` aggregates |
| `ImportLockTest` | Unit | Concurrent import rejected while lock held |

**Pass criteria (integration):** All eight entity counts match expected dataset size; failed-order count is consistent with CSV `status` values.

### 6.2 Data access (`AnalyticsOrderRepository`)

| Test | Type | What we verify |
|------|------|----------------|
| `AnalyticsOrderRepositoryTest` | Unit (mocked `JdbcTemplate`) | SQL delegation, row mapping, null handling, aggregation queries for each UC |

**Pass criteria:** Every public query method returns correctly mapped DTOs; empty results do not throw.

### 6.3 Correlation engine (`CorrelationEngine`)

| Test | Type | Rules covered |
|------|------|---------------|
| `CorrelationEngineTest` | Unit | `STOCKOUT_WAREHOUSE`, `WAREHOUSE_OPS`, `TRAFFIC_TRIPLE_CONFIRM`, `ADDRESS_MISMATCH`, `WEATHER_IMPACT`, `SLA_BREACH`, `FESTIVAL_VOLUME` |

**Pass criteria:** Single-order correlation returns expected rule IDs and confidence scores; unrelated signals do not produce false positives.

### 6.4 Insight generation (`InsightGenerator`)

| Test | Type | What we verify |
|------|------|----------------|
| `InsightGeneratorTest` | Unit | Narrative templates for delays, client failures, warehouse breakdown, city comparison, festival analysis, capacity projection |

**Pass criteria:** Narratives mention key counts and top causes; recommendations list is non-empty for actionable scenarios.

### 6.5 Query orchestration (`AnalyticsQueryService`)

| Test | Type | What we verify |
|------|------|----------------|
| `AnalyticsQueryServiceTest` | Unit | End-to-end orchestration per UC with mocked repository + engines |
| `AnalyticsDataNotLoadedException` | Unit | Queries fail fast when `hasSampleData()` is false |

### 6.6 REST API

| Test | Type | What we verify |
|------|------|----------------|
| `AnalyticsControllerTest` | Unit (`@WebMvcTest`) | Parameter binding, HTTP status, response delegation |
| `AnalyticsExceptionHandlerTest` | Unit | `412 PRECONDITION_FAILED` when data not loaded |
| Admin import | Acceptance | `POST /api/v1/admin/analytics/import` requires `X-Admin-Api-Key` |

### 6.7 Runtime (`analytics-service`)

| Test | Type | What we verify |
|------|------|----------------|
| Docker Compose smoke | Manual / acceptance | Service starts without JPA schema errors; listens on 8080 |
| `verify_analytics_gateway.sh` | Acceptance | All six UCs return HTTP 200 with valid JSON bodies |

---

## 7. Non-functional testing

| Area | Approach | Current status |
|------|----------|----------------|
| **Security** | Admin API key on `/api/v1/admin/*`; insecure defaults blocked unless `SWIFTEATS_ALLOW_INSECURE_DEFAULTS=true` | Covered by `AdminApiKeyFilter` (platform-lib) |
| **Concurrency** | `ImportLock` prevents parallel imports | Unit tested |
| **Performance** | Import duration logged in `ImportResult.durationMs` | Observed in demo; no formal SLA |
| **Resilience** | Graceful error when dataset missing or queries run before import | `AnalyticsDataNotLoadedException` + handler test |
| **Portability** | Docker Compose from clean clone | Documented in README |

---

## 8. Test execution

### 8.1 Fast unit test suite (no Docker)

```bash
mvn -pl analytics-service -am test -Dtest='!SampleDataImportServiceIntegrationTest'
```

Runs all `analytics-lib` unit tests and builds dependencies. Skips the Testcontainers integration test (requires Docker).

### 8.2 Full test suite (Docker required)

```bash
mvn -pl analytics-service -am test
```

Includes `SampleDataImportServiceIntegrationTest`.

### 8.3 Acceptance / use-case verification

```bash
docker compose up --build -d
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"
./scripts/verify_analytics_gateway.sh
```

Expected: `=== Summary: 6 passed, 0 failed ===`

### 8.4 Refresh recorded outputs (submission artifact)

```bash
python3 scripts/refresh_analytics_outputs.py
# → deliverables/SAMPLE-USE-CASE-OUTPUTS.md
```

### 8.5 Optional coverage report

JaCoCo is configured in `platform-lib/pom.xml`. Generate after tests:

```bash
mvn -pl platform-lib test jacoco:report
# → platform-lib/target/site/jacoco/index.html
```

---

## 9. Entry and exit criteria

### 9.1 Entry criteria (before a test run)

- [ ] JDK 17 and Maven available
- [ ] For integration/acceptance: Docker daemon running
- [ ] `sample-data/` present with all eight CSV files
- [ ] For acceptance: `analytics-service` healthy on port 8080

### 9.2 Exit criteria (release / submission ready)

- [ ] All `analytics-lib` unit tests pass
- [ ] `verify_analytics_gateway.sh` reports 6/6 passed after import
- [ ] `deliverables/SAMPLE-USE-CASE-OUTPUTS.md` reflects current API responses
- [ ] Demo script (`deliverables/DEMO-SCRIPT.md`) walkthrough completes without errors
- [ ] Each UC response includes **narrative**, **recommendations**, and **evidence** (where applicable)

---

## 10. Defect classification

| Severity | Definition | Example |
|----------|------------|---------|
| **Critical** | Service will not start or import fails entirely | Schema migration failure, startup crash |
| **High** | UC returns wrong counts or empty narrative with data loaded | UC2 shows zero failures when client has failures |
| **Medium** | Correlation rule misfires on edge cases | False positive `TRAFFIC_TRIPLE_CONFIRM` |
| **Low** | Cosmetic narrative wording, non-blocking script warnings | Import re-run returns 409 while data exists |

---

## 11. Risks and mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Sample data changes | UC assertions break | Pin row-count checks in integration test; version dataset |
| Rule-based engine limits | Misses novel failure patterns | Document scope; UC outputs show evidence either way |
| Docker not available in CI | Integration test skipped | Default Maven command excludes Testcontainers test |
| Stale recorded outputs | Submission docs diverge from code | `refresh_analytics_outputs.py` in demo prep checklist |
| Shared `platform-lib` JPA entities | Analytics service startup conflicts | JPA disabled in `analytics-service`; JDBC-only path |

---

## 12. Traceability matrix (assignment → tests)

| Assignment requirement | Test artifact |
|------------------------|---------------|
| Aggregate multi-domain data | `SampleDataImportServiceIntegrationTest`, import acceptance curl |
| Correlate events automatically | `CorrelationEngineTest`, UC responses with `evidence.correlationRules` |
| Human-readable insights | `InsightGeneratorTest`, UC `narrative` fields in recorded outputs |
| Actionable recommendations | `InsightGeneratorTest`, UC `recommendations` arrays |
| Sample program demo | `DEMO-SCRIPT.md`, `verify_analytics_gateway.sh` |
| Record output for sample use cases | `SAMPLE-USE-CASE-OUTPUTS.md`, `refresh_analytics_outputs.py` |

---

## 13. Future improvements

If this were extended beyond the assignment scope:

- Contract tests (OpenAPI / JSON Schema) for stable API evolution
- End-to-end test module with Testcontainers running full `docker compose`
- Property-based tests for CSV parsing edge cases
- Performance baseline for 100k+ order imports
- Dashboard Playwright smoke tests for UC navigation

---

## 14. References

| Document | Path |
|----------|------|
| Assignment brief | `2026-H1-AI-Assignment-2.txt` |
| README / quick start | `README.md` |
| Demo script | `deliverables/DEMO-SCRIPT.md` |
| Recorded UC outputs | `deliverables/SAMPLE-USE-CASE-OUTPUTS.md` |
| Gateway verification | `scripts/verify_analytics_gateway.sh` |
| Output refresh | `scripts/refresh_analytics_outputs.py` |
