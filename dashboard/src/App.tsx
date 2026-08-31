import { FormEvent, useState } from 'react';
import {
  capacityProjection,
  compareCities,
  insightQuery,
  queryDelays,
  queryFailures,
  queryWarehouseFailures,
} from './api/client';
import { InsightPanel } from './components/InsightPanel';

type TabId = 'uc1' | 'uc2' | 'uc3' | 'uc4' | 'uc5' | 'uc6';

const TABS: { id: TabId; label: string; hint: string }[] = [
  { id: 'uc1', label: 'UC1 Delays', hint: 'Why were deliveries delayed in city X?' },
  { id: 'uc2', label: 'UC2 Client failures', hint: 'Why did Client X orders fail?' },
  { id: 'uc3', label: 'UC3 Warehouse', hint: 'Top failure reasons for Warehouse B' },
  { id: 'uc4', label: 'UC4 Compare cities', hint: 'Compare failure causes between cities' },
  { id: 'uc5', label: 'UC5 Festival', hint: 'Festival period risks & preparation' },
  { id: 'uc6', label: 'UC6 Capacity', hint: 'New client volume risk projection' },
];

export default function App() {
  const [tab, setTab] = useState<TabId>('uc1');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<Record<string, unknown> | null>(null);

  const [city, setCity] = useState('Pune');
  const [date, setDate] = useState('2025-06-06');
  const [clientId, setClientId] = useState('337');
  const [from, setFrom] = useState('2025-01-01T00:00:00Z');
  const [to, setTo] = useState('2025-12-31T00:00:00Z');
  const [warehouseId, setWarehouseId] = useState('2');
  const [month, setMonth] = useState('2025-08');
  const [cityA, setCityA] = useState('Pune');
  const [cityB, setCityB] = useState('Mumbai');
  const [festivalFrom, setFestivalFrom] = useState('2025-08-01');
  const [festivalTo, setFestivalTo] = useState('2025-09-01');
  const [additionalOrders, setAdditionalOrders] = useState('20000');

  async function runQuery(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      let data: Record<string, unknown>;
      switch (tab) {
        case 'uc1':
          data = (await queryDelays(city, date)) as Record<string, unknown>;
          break;
        case 'uc2':
          data = (await queryFailures(Number(clientId), from, to)) as Record<string, unknown>;
          break;
        case 'uc3':
          data = (await queryWarehouseFailures(Number(warehouseId), month)) as Record<string, unknown>;
          break;
        case 'uc4':
          data = (await compareCities(cityA, cityB, month)) as Record<string, unknown>;
          break;
        case 'uc5':
          data = (await insightQuery('FESTIVAL_ANALYSIS', { from: festivalFrom, to: festivalTo })) as Record<string, unknown>;
          break;
        case 'uc6':
          data = (await capacityProjection(Number(clientId), Number(additionalOrders))) as Record<string, unknown>;
          break;
        default:
          throw new Error('Unknown query');
      }
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Query failed');
    } finally {
      setLoading(false);
    }
  }

  const activeTab = TABS.find((t) => t.id === tab)!;

  return (
    <div className="shell">
      <header className="header">
        <h1>SwiftEats Analytics</h1>
        <p>Delivery failure root-cause analysis — Assignment 2 sample use cases (UC1–UC6).</p>
      </header>

      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            className={`tab ${tab === t.id ? 'active' : ''}`}
            onClick={() => { setTab(t.id); setResult(null); setError(null); }}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="panel">
        <form className="card" onSubmit={runQuery}>
          <h2 style={{ margin: '0 0 0.35rem', fontSize: '1.05rem' }}>{activeTab.label}</h2>
          <p style={{ color: 'var(--muted)', fontSize: '0.9rem', margin: '0 0 1rem' }}>{activeTab.hint}</p>

          {tab === 'uc1' && (
            <>
              <div className="form-row"><label>City</label><input value={city} onChange={(e) => setCity(e.target.value)} /></div>
              <div className="form-row"><label>Date</label><input type="date" value={date} onChange={(e) => setDate(e.target.value)} /></div>
            </>
          )}
          {tab === 'uc2' && (
            <>
              <div className="form-row"><label>Client ID</label><input value={clientId} onChange={(e) => setClientId(e.target.value)} /></div>
              <div className="form-row"><label>From (ISO)</label><input value={from} onChange={(e) => setFrom(e.target.value)} /></div>
              <div className="form-row"><label>To (ISO)</label><input value={to} onChange={(e) => setTo(e.target.value)} /></div>
            </>
          )}
          {tab === 'uc3' && (
            <>
              <div className="form-row"><label>Warehouse ID</label><input value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)} /></div>
              <div className="form-row"><label>Month (yyyy-MM)</label><input value={month} onChange={(e) => setMonth(e.target.value)} /></div>
            </>
          )}
          {tab === 'uc4' && (
            <>
              <div className="form-row"><label>City A</label><input value={cityA} onChange={(e) => setCityA(e.target.value)} /></div>
              <div className="form-row"><label>City B</label><input value={cityB} onChange={(e) => setCityB(e.target.value)} /></div>
              <div className="form-row"><label>Month</label><input value={month} onChange={(e) => setMonth(e.target.value)} /></div>
            </>
          )}
          {tab === 'uc5' && (
            <>
              <div className="form-row"><label>From</label><input type="date" value={festivalFrom} onChange={(e) => setFestivalFrom(e.target.value)} /></div>
              <div className="form-row"><label>To</label><input type="date" value={festivalTo} onChange={(e) => setFestivalTo(e.target.value)} /></div>
            </>
          )}
          {tab === 'uc6' && (
            <>
              <div className="form-row"><label>Client ID</label><input value={clientId} onChange={(e) => setClientId(e.target.value)} /></div>
              <div className="form-row"><label>Additional monthly orders</label><input value={additionalOrders} onChange={(e) => setAdditionalOrders(e.target.value)} /></div>
            </>
          )}

          <button type="submit" disabled={loading} style={{ width: '100%', marginTop: '0.5rem' }}>
            {loading ? 'Analyzing…' : 'Run analysis'}
          </button>
        </form>

        <div className="card">
          <h2 style={{ margin: '0 0 1rem', fontSize: '1.05rem' }}>Insights</h2>
          {error && <div className="error">{error}</div>}
          <InsightPanel result={result} />
        </div>
      </div>
    </div>
  );
}
