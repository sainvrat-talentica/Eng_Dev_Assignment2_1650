function BarChart({ data, title }: { data: Record<string, number>; title?: string }) {
  const entries = Object.entries(data).sort((a, b) => b[1] - a[1]);
  const max = Math.max(...entries.map(([, v]) => v), 1);

  if (entries.length === 0) return null;

  return (
    <div>
      {title && <h3 style={{ margin: '1rem 0 0.5rem', fontSize: '0.95rem' }}>{title}</h3>}
      <div className="bar-chart">
        {entries.map(([label, value]) => (
          <div key={label} className="bar-row">
            <span>{label}</span>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${(value / max) * 100}%` }} />
            </div>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

function Recommendations({ items }: { items?: string[] }) {
  if (!items?.length) return null;
  return (
    <div style={{ marginTop: '1.25rem' }}>
      <h3 style={{ fontSize: '0.95rem', marginBottom: '0.5rem' }}>Recommendations</h3>
      <ul className="rec-list">
        {items.map((r) => (
          <li key={r}>{r}</li>
        ))}
      </ul>
    </div>
  );
}

export function InsightPanel({ result }: { result: Record<string, unknown> | null }) {
  if (!result) {
    return <p className="placeholder">Run a query to see narrative insights and charts.</p>;
  }

  const narrative = String(result.narrative ?? '');
  const recommendations = result.recommendations as string[] | undefined;

  const reasonCounts =
    (result.failureReasonCounts as Record<string, number>) ??
    (result.reasonCounts as Record<string, number>);

  const cityAFailures = result.cityAFailures as Record<string, number> | undefined;
  const cityBFailures = result.cityBFailures as Record<string, number> | undefined;

  return (
    <>
      <p className="narrative">{narrative}</p>

      {'totalAffected' in result && (
        <div className="stats">
          <div className="stat">
            <strong>{String(result.totalAffected)}</strong>
            <span>Affected orders</span>
          </div>
          {'heavyTrafficCount' in result && (
            <div className="stat">
              <strong>{String(result.heavyTrafficCount)}</strong>
              <span>Traffic correlated</span>
            </div>
          )}
          {'slowPackingCount' in result && (
            <div className="stat">
              <strong>{String(result.slowPackingCount)}</strong>
              <span>Slow packing</span>
            </div>
          )}
          {'negativeFeedbackCount' in result && (
            <div className="stat">
              <strong>{String(result.negativeFeedbackCount)}</strong>
              <span>Negative feedback</span>
            </div>
          )}
        </div>
      )}

      {reasonCounts && Object.keys(reasonCounts).length > 0 && (
        <BarChart data={reasonCounts} title="Failure / delay reasons" />
      )}

      {cityAFailures && (
        <BarChart data={cityAFailures} title={`${result.cityA} failures`} />
      )}
      {cityBFailures && (
        <BarChart data={cityBFailures} title={`${result.cityB} failures`} />
      )}

      <Recommendations items={recommendations} />

      <details style={{ marginTop: '1.25rem' }}>
        <summary style={{ cursor: 'pointer', color: 'var(--muted)' }}>Raw JSON</summary>
        <pre style={{ overflow: 'auto', fontSize: '0.75rem', background: 'var(--surface-2)', padding: '0.75rem', borderRadius: 8 }}>
          {JSON.stringify(result, null, 2)}
        </pre>
      </details>
    </>
  );
}
