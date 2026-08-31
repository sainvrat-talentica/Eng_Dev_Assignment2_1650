# SwiftEats — Assignment 2 (Delivery Failure Analytics)

Standalone submission package for **H1 Assignment 2**. See [2026-H1-AI-Assignment-2.txt](./2026-H1-AI-Assignment-2.txt).

## Quick start

```bash
docker compose up --build
```

Import sample CSV, then query insights:

```bash
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"

./scripts/verify_analytics_gateway.sh
```

| Surface | URL |
|---------|-----|
| Analytics API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Analytics UI | http://localhost:3002 |

Postgres runs on host port **5433** (to avoid clashing with Assignment 1 if both run locally).

## Layout

```
Eng_Dev_Assignment2_1650/       # ← repository root
├── pom.xml                     # Maven parent (platform-lib + analytics-lib + service)
├── platform-lib/               # Shared Spring infrastructure (vendored subset)
├── analytics-lib/              # Import, correlation engine, insight APIs
├── analytics-service/          # Runnable Spring Boot app (:8080)
├── sample-data/                # 8 CSV files from assignment dataset
├── dashboard/                  # Optional analytics UI (:3002)
├── deliverables/               # Write-up, demo script, recorded UC outputs
├── scripts/                    # refresh outputs, gateway verify, doc generation
└── docker-compose.yml
```

## Maven

```bash
mvn -pl analytics-service -am test -Dtest='!SampleDataImportServiceIntegrationTest'
mvn -pl analytics-service -am package
```

## Deliverables

| Item | Location |
|------|----------|
| Sample program | `analytics-service` + REST APIs + `AnalyticsDemoRunner` |
| Sample data | `sample-data/` |
| Recorded UC outputs | `deliverables/SAMPLE-USE-CASE-OUTPUTS.md` |
| Demo script | `deliverables/DEMO-SCRIPT.md` |
| Word write-up (local) | `python3 scripts/generate_architecture_diagram.py && python3 scripts/generate_assignment2_writeup.py` → `deliverables/*.docx` |

Assignment 1 (food delivery platform) is a separate project when submitted independently.
