# Assignment 2 — Recorded Sample Use Case Outputs

**Project:** SwiftEats — Delivery Failure Root-Cause Analytics  
**Recorded:** 2026-08-31 11:10:29 UTC  
**Environment:** Docker Compose + SwiftEats API at `http://localhost:8080`  
**Dataset:** `sample-data/` imported into PostgreSQL schema `analytics.*`  
**Import result:** success=True — see row counts in import API response below

## How to reproduce

```bash
docker compose up -d
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: local-dev-admin-key-change-me"

python3 scripts/refresh_analytics_outputs.py
```

Or refresh manually with the API calls listed under each use case below.

Raw JSON responses: [`raw-responses/`](./raw-responses/)

### Import response (this run)

```json
{
  "success": true,
  "datasetPath": "/data/third-assignment-sample-data-set",
  "rowCounts": {
    "client": 500,
    "warehouse": 50,
    "driver": 2000,
    "order": 10000,
    "warehouse_log": 10000,
    "fleet_log": 10000,
    "feedback": 10000,
    "external_factor": 10000
  },
  "durationMs": 4592,
  "message": "Sample dataset imported successfully"
}
```

---

## UC1: Why were deliveries delayed in city X yesterday?

**Business question:** Why were deliveries delayed in Pune on 2025-06-06?

**API:** `GET /api/v1/analytics/delays?city=Pune&date=2025-06-06`

### Narrative (human-readable insight)

In Pune on 2025-06-06, 5 deliveries were delayed or failed. Top cause: Unknown (80% of affected orders). 2 orders correlated with heavy traffic and 1 with slow warehouse packing. 3 customers left negative feedback mentioning lateness or service issues.

### Recommendations

- Review end-to-end SLA checkpoints from kitchen dispatch to last-mile handoff.
- Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.
- Enable real-time inventory sync and low-stock alerts at affected warehouses.

### Evidence / structured output

```json
{
  "city": "Pune",
  "date": "2025-06-06",
  "totalAffected": 5,
  "failureReasonCounts": {
    "Unknown": 4,
    "Stockout": 1
  },
  "heavyTrafficCount": 2,
  "slowPackingCount": 1,
  "negativeFeedbackCount": 3,
  "narrative": "In Pune on 2025-06-06, 5 deliveries were delayed or failed. Top cause: Unknown (80% of affected orders). 2 orders correlated with heavy traffic and 1 with slow warehouse packing. 3 customers left negative feedback mentioning lateness or service issues.",
  "recommendations": [
    "Review end-to-end SLA checkpoints from kitchen dispatch to last-mile handoff.",
    "Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.",
    "Enable real-time inventory sync and low-stock alerts at affected warehouses."
  ]
}
```

---

## UC2: Why did Client X's orders fail in the past week?

**Business question:** Why did client 337 (Ramesh-Choudhary) orders fail in 2025?

**API:** `GET /api/v1/analytics/failures?clientId=337&from=2025-01-01T00:00:00Z&to=2025-12-31T00:00:00Z`

### Narrative (human-readable insight)

Client Ramesh-Choudhary (id 337) recorded 6 failed orders from 2025-01-01T00:00:00Z to 2025-12-31T00:00:00Z. Primary failure reason: Stockout (3 orders). Fleet issues noted: Heavy congestion. Warehouse issues: Stock delay on item.

### Recommendations

- Enable real-time inventory sync and low-stock alerts at affected warehouses.
- Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.
- Add packing staff at the warehouse during peak hours.

### Evidence / structured output

```json
{
  "clientId": 337,
  "from": "2025-01-01T00:00:00Z",
  "to": "2025-12-31T00:00:00Z",
  "totalFailed": 6,
  "breakdown": [
    {
      "failureReason": "Stockout",
      "count": 3,
      "fleetIssues": "Heavy congestion",
      "warehouseIssues": "Stock delay on item"
    },
    {
      "failureReason": "Traffic congestion",
      "count": 3,
      "fleetIssues": "Breakdown",
      "warehouseIssues": "Stock delay on item, System issue"
    },
    {
      "failureReason": "Warehouse delay",
      "count": 2,
      "fleetIssues": "Address not found",
      "warehouseIssues": "System issue"
    }
  ],
  "narrative": "Client Ramesh-Choudhary (id 337) recorded 6 failed orders from 2025-01-01T00:00:00Z to 2025-12-31T00:00:00Z. Primary failure reason: Stockout (3 orders). Fleet issues noted: Heavy congestion. Warehouse issues: Stock delay on item.",
  "recommendations": [
    "Enable real-time inventory sync and low-stock alerts at affected warehouses.",
    "Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.",
    "Add packing staff at the warehouse during peak hours."
  ]
}
```

---

## UC3: Top reasons for failures linked to Warehouse B in August

**Business question:** Explain the top reasons for delivery failures linked to Warehouse 2 in August 2025

**API:** `GET /api/v1/analytics/failures/by-warehouse?warehouseId=2&monthParam=2025-08`

### Narrative (human-readable insight)

Warehouse Warehouse 2 (id 2) had 6 failed orders in 2025-08. Leading failure reason: Weather disruption. Warehouse log patterns: System issue (5), Slow packing (1).

### Recommendations

- Add packing staff at Warehouse 2 during peak hours.
- Pause SLA clock during severe weather and pre-position drivers beforehand.
- Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.

### Evidence / structured output

```json
{
  "warehouseId": 2,
  "warehouseName": "Warehouse 2",
  "month": 8,
  "year": 2025,
  "totalFailed": 6,
  "failureReasonCounts": {
    "Weather disruption": 3,
    "Warehouse delay": 2,
    "Stockout": 1
  },
  "warehouseNoteCounts": {
    "System issue": 5,
    "Slow packing": 1
  },
  "narrative": "Warehouse Warehouse 2 (id 2) had 6 failed orders in 2025-08. Leading failure reason: Weather disruption. Warehouse log patterns: System issue (5), Slow packing (1).",
  "recommendations": [
    "Add packing staff at Warehouse 2 during peak hours.",
    "Pause SLA clock during severe weather and pre-position drivers beforehand.",
    "Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks."
  ]
}
```

---

## UC4: Compare delivery failure causes between City A and City B

**Business question:** Compare delivery failure causes between Pune and Mumbai in August 2025

**API:** `GET /api/v1/analytics/failures/compare?cityA=Pune&cityB=Mumbai&monthParam=2025-08`

### Narrative (human-readable insight)

During 2025-08, Pune had 16 failed deliveries (top cause: Incorrect address) while Mumbai had 19 (top cause: Stockout). Compare operational focus: address verification in cities with high incorrect-address rates, and warehouse staffing where warehouse-delay failures dominate.

### Recommendations

- Mandatory address pin validation before dispatch.

### Evidence / structured output

```json
{
  "cityA": "Pune",
  "cityB": "Mumbai",
  "month": "2025-08",
  "cityAFailures": {
    "Incorrect address": 6,
    "Stockout": 5,
    "Weather disruption": 3,
    "Warehouse delay": 2
  },
  "cityBFailures": {
    "Stockout": 7,
    "Warehouse delay": 4,
    "Weather disruption": 4,
    "Incorrect address": 2,
    "Traffic congestion": 2
  },
  "narrative": "During 2025-08, Pune had 16 failed deliveries (top cause: Incorrect address) while Mumbai had 19 (top cause: Stockout). Compare operational focus: address verification in cities with high incorrect-address rates, and warehouse staffing where warehouse-delay failures dominate.",
  "recommendations": [
    "Mandatory address pin validation before dispatch."
  ]
}
```

---

## UC5: Festival period failures and preparation

**Business question:** What caused delivery failures during festival/holiday periods (Aug 2025)?

**API:** `POST /api/v1/analytics/insights/query`

**Request body:**

```json
{
  "queryType": "FESTIVAL_ANALYSIS",
  "parameters": {
    "from": "2025-08-01T00:00:00Z",
    "to": "2025-09-01T00:00:00Z"
  }
}
```

### Narrative (human-readable insight)

Between 2025-08-01T00:00:00Z and 2025-09-01T00:00:00Z, festival/holiday periods saw 187 failure events. Top correlated reason: Weather disruption. Average warehouse capacity during these events was constrained — plan buffer staffing and driver capacity.

### Recommendations

- Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.
- Add packing staff at the warehouse during peak hours.
- Review end-to-end SLA checkpoints from kitchen dispatch to last-mile handoff.
- Mandatory address pin validation before dispatch.
- Pause SLA clock during severe weather and pre-position drivers beforehand.
- Enable real-time inventory sync and low-stock alerts at affected warehouses.

### Evidence / structured output

```json
{
  "narrative": "Between 2025-08-01T00:00:00Z and 2025-09-01T00:00:00Z, festival/holiday periods saw 187 failure events. Top correlated reason: Weather disruption. Average warehouse capacity during these events was constrained \u2014 plan buffer staffing and driver capacity.",
  "recommendations": [
    "Pre-scale warehouse capacity by 15% and add a 15% driver buffer before festival peaks.",
    "Add packing staff at the warehouse during peak hours.",
    "Review end-to-end SLA checkpoints from kitchen dispatch to last-mile handoff.",
    "Mandatory address pin validation before dispatch.",
    "Pause SLA clock during severe weather and pre-position drivers beforehand.",
    "Enable real-time inventory sync and low-stock alerts at affected warehouses."
  ],
  "evidence": {
    "failureReasons": {
      "Warehouse delay": 40,
      "Stockout": 46,
      "Weather disruption": 43,
      "Traffic congestion": 35,
      "Incorrect address": 23
    },
    "correlatedRules": [
      "FESTIVAL_VOLUME",
      "WAREHOUSE_OPS",
      "SLA_BREACH",
      "ADDRESS_MISMATCH",
      "WEATHER_IMPACT",
      "STOCKOUT_WAREHOUSE"
    ],
    "eventBreakdown": {
      "Festival": 399,
      "Holiday": 479
    }
  }
}
```

---

## UC6: Onboard Client Y (+20,000 orders/month) — capacity risk

**Business question:** If client 337 adds 20,000 monthly orders, what failure risks emerge?

**API:** `GET /api/v1/analytics/capacity-projection?clientId=337&additionalMonthlyOrders=20000`

### Narrative (human-readable insight)

Client Ramesh-Choudhary (id 337) historically fails 27.3% of orders. Adding 20,000 monthly orders projects strain on 15 warehouse(s). High-risk sites exceed 80% projected utilization and need mitigation before onboarding.

### Recommendations

- Increase capacity or reroute volume at Warehouse 45 (Chennai) — projected 421% utilization.
- Increase capacity or reroute volume at Warehouse 38 (Mumbai) — projected 124% utilization.
- Increase capacity or reroute volume at Warehouse 39 (Mysuru) — projected 109% utilization.
- Increase capacity or reroute volume at Warehouse 11 (Ahmedabad) — projected 143% utilization.
- Increase capacity or reroute volume at Warehouse 12 (Bengaluru) — projected 129% utilization.
- Increase capacity or reroute volume at Warehouse 14 (Pune) — projected 104% utilization.
- Increase capacity or reroute volume at Warehouse 25 (New Delhi) — projected 118% utilization.
- Increase capacity or reroute volume at Warehouse 29 (Ahmedabad) — projected 117% utilization.
- Increase capacity or reroute volume at Warehouse 30 (New Delhi) — projected 118% utilization.
- Increase capacity or reroute volume at Warehouse 31 (Coimbatore) — projected 159% utilization.
- Increase capacity or reroute volume at Warehouse 36 (New Delhi) — projected 123% utilization.
- Increase capacity or reroute volume at Warehouse 37 (Nagpur) — projected 135% utilization.
- Increase capacity or reroute volume at Warehouse 47 (Chennai) — projected 104% utilization.
- Increase capacity or reroute volume at Warehouse 3 (Ahmedabad) — projected 148% utilization.
- Increase capacity or reroute volume at Warehouse 5 (New Delhi) — projected 118% utilization.
- Historical failure rate exceeds 15% — run root-cause review before scaling order volume.
- Add driver buffer proportional to projected order growth in primary client cities.

### Evidence / structured output

```json
{
  "clientId": 337,
  "clientName": "Ramesh-Choudhary",
  "additionalMonthlyOrders": 20000,
  "historicalFailureRate": 0.2727272727272727,
  "warehouseRisks": [
    {
      "warehouseId": 45,
      "warehouseName": "Warehouse 45",
      "city": "Chennai",
      "capacity": 649,
      "currentMonthlyOrders": 3,
      "projectedMonthlyOrders": 2730,
      "projectedUtilizationPct": 420.64714946070876,
      "highRisk": true
    },
    {
      "warehouseId": 38,
      "warehouseName": "Warehouse 38",
      "city": "Mumbai",
      "capacity": 1463,
      "currentMonthlyOrders": 2,
      "projectedMonthlyOrders": 1820,
      "projectedUtilizationPct": 124.40191387559808,
      "highRisk": true
    },
    {
      "warehouseId": 39,
      "warehouseName": "Warehouse 39",
      "city": "Mysuru",
      "capacity": 1668,
      "currentMonthlyOrders": 2,
      "projectedMonthlyOrders": 1820,
      "projectedUtilizationPct": 109.11270983213429,
      "highRisk": true
    },
    {
      "warehouseId": 8,
      "warehouseName": "Warehouse 8",
      "city": "Chennai",
      "capacity": 1435,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 63.41463414634146,
      "highRisk": false
    },
    {
      "warehouseId": 11,
      "warehouseName": "Warehouse 11",
      "city": "Ahmedabad",
      "capacity": 636,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 143.0817610062893,
      "highRisk": true
    },
    {
      "warehouseId": 12,
      "warehouseName": "Warehouse 12",
      "city": "Bengaluru",
      "capacity": 707,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 128.7128712871287,
      "highRisk": true
    },
    {
      "warehouseId": 13,
      "warehouseName": "Warehouse 13",
      "city": "Nagpur",
      "capacity": 1250,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 72.8,
      "highRisk": false
    },
    {
      "warehouseId": 14,
      "warehouseName": "Warehouse 14",
      "city": "Pune",
      "capacity": 879,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 103.52673492605233,
      "highRisk": true
    },
    {
      "warehouseId": 20,
      "warehouseName": "Warehouse 20",
      "city": "Coimbatore",
      "capacity": 1250,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 72.8,
      "highRisk": false
    },
    {
      "warehouseId": 25,
      "warehouseName": "Warehouse 25",
      "city": "New Delhi",
      "capacity": 770,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 118.18181818181819,
      "highRisk": true
    },
    {
      "warehouseId": 26,
      "warehouseName": "Warehouse 26",
      "city": "Pune",
      "capacity": 1974,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 46.09929078014184,
      "highRisk": false
    },
    {
      "warehouseId": 29,
      "warehouseName": "Warehouse 29",
      "city": "Ahmedabad",
      "capacity": 775,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 117.41935483870968,
      "highRisk": true
    },
    {
      "warehouseId": 30,
      "warehouseName": "Warehouse 30",
      "city": "New Delhi",
      "capacity": 771,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 118.02853437094683,
      "highRisk": true
    },
    {
      "warehouseId": 31,
      "warehouseName": "Warehouse 31",
      "city": "Coimbatore",
      "capacity": 574,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 158.53658536585365,
      "highRisk": true
    },
    {
      "warehouseId": 35,
      "warehouseName": "Warehouse 35",
      "city": "Bengaluru",
      "capacity": 1190,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 76.47058823529412,
      "highRisk": false
    },
    {
      "warehouseId": 36,
      "warehouseName": "Warehouse 36",
      "city": "New Delhi",
      "capacity": 740,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 122.97297297297297,
      "highRisk": true
    },
    {
      "warehouseId": 37,
      "warehouseName": "Warehouse 37",
      "city": "Nagpur",
      "capacity": 673,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 135.21545319465082,
      "highRisk": true
    },
    {
      "warehouseId": 47,
      "warehouseName": "Warehouse 47",
      "city": "Chennai",
      "capacity": 873,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 104.23825887743413,
      "highRisk": true
    },
    {
      "warehouseId": 3,
      "warehouseName": "Warehouse 3",
      "city": "Ahmedabad",
      "capacity": 613,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 148.45024469820555,
      "highRisk": true
    },
    {
      "warehouseId": 50,
      "warehouseName": "Warehouse 50",
      "city": "Surat",
      "capacity": 1620,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 56.17283950617284,
      "highRisk": false
    },
    {
      "warehouseId": 5,
      "warehouseName": "Warehouse 5",
      "city": "New Delhi",
      "capacity": 769,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 118.33550065019506,
      "highRisk": true
    },
    {
      "warehouseId": 6,
      "warehouseName": "Warehouse 6",
      "city": "Bengaluru",
      "capacity": 1347,
      "currentMonthlyOrders": 1,
      "projectedMonthlyOrders": 910,
      "projectedUtilizationPct": 67.55753526354863,
      "highRisk": false
    }
  ],
  "narrative": "Client Ramesh-Choudhary (id 337) historically fails 27.3% of orders. Adding 20,000 monthly orders projects strain on 15 warehouse(s). High-risk sites exceed 80% projected utilization and need mitigation before onboarding.",
  "recommendations": [
    "Increase capacity or reroute volume at Warehouse 45 (Chennai) \u2014 projected 421% utilization.",
    "Increase capacity or reroute volume at Warehouse 38 (Mumbai) \u2014 projected 124% utilization.",
    "Increase capacity or reroute volume at Warehouse 39 (Mysuru) \u2014 projected 109% utilization.",
    "Increase capacity or reroute volume at Warehouse 11 (Ahmedabad) \u2014 projected 143% utilization.",
    "Increase capacity or reroute volume at Warehouse 12 (Bengaluru) \u2014 projected 129% utilization.",
    "Increase capacity or reroute volume at Warehouse 14 (Pune) \u2014 projected 104% utilization.",
    "Increase capacity or reroute volume at Warehouse 25 (New Delhi) \u2014 projected 118% utilization.",
    "Increase capacity or reroute volume at Warehouse 29 (Ahmedabad) \u2014 projected 117% utilization.",
    "Increase capacity or reroute volume at Warehouse 30 (New Delhi) \u2014 projected 118% utilization.",
    "Increase capacity or reroute volume at Warehouse 31 (Coimbatore) \u2014 projected 159% utilization.",
    "Increase capacity or reroute volume at Warehouse 36 (New Delhi) \u2014 projected 123% utilization.",
    "Increase capacity or reroute volume at Warehouse 37 (Nagpur) \u2014 projected 135% utilization.",
    "Increase capacity or reroute volume at Warehouse 47 (Chennai) \u2014 projected 104% utilization.",
    "Increase capacity or reroute volume at Warehouse 3 (Ahmedabad) \u2014 projected 148% utilization.",
    "Increase capacity or reroute volume at Warehouse 5 (New Delhi) \u2014 projected 118% utilization.",
    "Historical failure rate exceeds 15% \u2014 run root-cause review before scaling order volume.",
    "Add driver buffer proportional to projected order growth in primary client cities."
  ]
}
```

---
