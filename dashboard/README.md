# SwiftEats Analytics Dashboard

Visual UI for Assignment 2 sample use cases (UC1–UC6): delay analysis, failure root causes, city comparison, festival prep, and capacity projection.

## Prerequisites

Import sample CSV via Admin Dashboard or:

```bash
curl -X POST http://localhost:8080/api/v1/admin/analytics/import \
  -H "X-Admin-Api-Key: $ADMIN_API_KEY"
```

## Dev

```bash
npm install
cp .env.example .env
npm run dev
```

Open http://localhost:3002

## Docker

```bash
docker compose up --build
```

Analytics UI: http://localhost:3002
