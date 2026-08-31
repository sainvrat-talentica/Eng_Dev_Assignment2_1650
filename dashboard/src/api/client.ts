const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1';

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, init);
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed (${response.status})`);
  }
  return response.json() as Promise<T>;
}

export interface InsightResult {
  narrative: string;
  recommendations: string[];
  evidence?: Record<string, unknown>;
  [key: string]: unknown;
}

export function queryDelays(city: string, date: string) {
  return apiFetch<InsightResult>(`/analytics/delays?city=${encodeURIComponent(city)}&date=${date}`);
}

export function queryFailures(clientId: number, from: string, to: string) {
  return apiFetch<InsightResult>(
    `/analytics/failures?clientId=${clientId}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
  );
}

export function queryWarehouseFailures(warehouseId: number, month: string) {
  return apiFetch<InsightResult>(
    `/analytics/failures/by-warehouse?warehouseId=${warehouseId}&monthParam=${month}`,
  );
}

export function compareCities(cityA: string, cityB: string, month: string) {
  return apiFetch<InsightResult>(
    `/analytics/failures/compare?cityA=${encodeURIComponent(cityA)}&cityB=${encodeURIComponent(cityB)}&monthParam=${month}`,
  );
}

export function capacityProjection(clientId: number, additionalMonthlyOrders: number) {
  return apiFetch<InsightResult>(
    `/analytics/capacity-projection?clientId=${clientId}&additionalMonthlyOrders=${additionalMonthlyOrders}`,
  );
}

export function insightQuery(queryType: string, parameters: Record<string, string | number>) {
  return apiFetch<InsightResult>('/analytics/insights/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ queryType, parameters }),
  });
}
