import { useCallback, useEffect, useState } from 'react'
import { fetchStats, ApiError } from '../services/api'
import type { StatsResponse } from '../types/api'
import './StatsPanel.css'

const WINDOWS: { label: string; hours: number }[] = [
  { label: '24h', hours: 24 },
  { label: '7d', hours: 24 * 7 },
  { label: '30d', hours: 24 * 30 },
]

export function StatsPanel() {
  const [windowHours, setWindowHours] = useState<number>(24)
  const [stats, setStats] = useState<StatsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async (hours: number) => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchStats(hours)
      setStats(data)
    } catch (err) {
      if (err instanceof ApiError || err instanceof Error) {
        setError(err.message)
      } else {
        setError('Unexpected error.')
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load(windowHours)
  }, [load, windowHours])

  const empty = stats !== null && stats.totalRequests === 0

  return (
    <div className="stats">
      <header className="stats__header">
        <div className="stats__window" role="group" aria-label="Time window">
          {WINDOWS.map((w) => (
            <button
              key={w.hours}
              type="button"
              className={`stats__window-btn ${w.hours === windowHours ? 'stats__window-btn--active' : ''}`}
              onClick={() => setWindowHours(w.hours)}
              aria-pressed={w.hours === windowHours}
            >
              {w.label}
            </button>
          ))}
        </div>
        <button
          type="button"
          className="btn btn--ghost stats__refresh"
          onClick={() => load(windowHours)}
          disabled={loading}
        >
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </header>

      {error && <div className="alert alert--error" role="alert">{error}</div>}

      {empty && !error && (
        <div className="stats__empty">
          <strong>No requests yet in this window.</strong>
          <span>Generate an alt text from the single or batch tab — stats appear here.</span>
        </div>
      )}

      {stats && stats.totalRequests > 0 && (
        <>
          <div className="stats__kpis">
            <Kpi label="Total" value={stats.totalRequests.toString()} />
            <Kpi label="Succeeded" value={stats.succeededRequests.toString()} accent="ok" />
            <Kpi label="Failed" value={stats.failedRequests.toString()} accent={stats.failedRequests > 0 ? 'error' : undefined} />
            <Kpi label="Success rate" value={`${stats.successRatePct.toFixed(1)} %`} />
            <Kpi
              label="Avg latency"
              value={stats.avgLatencyMs == null ? '—' : `${Math.round(stats.avgLatencyMs)} ms`}
            />
          </div>

          <div className="stats__breakdowns">
            <Breakdown title="By language" entries={stats.byLanguage} total={stats.totalRequests} />
            <Breakdown title="By endpoint" entries={stats.byEndpoint} total={stats.totalRequests} />
          </div>
        </>
      )}
    </div>
  )
}

function Kpi({ label, value, accent }: { label: string; value: string; accent?: 'ok' | 'error' }) {
  return (
    <div className={`stats__kpi ${accent ? `stats__kpi--${accent}` : ''}`}>
      <span className="stats__kpi-label">{label}</span>
      <span className="stats__kpi-value">{value}</span>
    </div>
  )
}

function Breakdown({
  title,
  entries,
  total,
}: {
  title: string
  entries: Record<string, number>
  total: number
}) {
  const rows = Object.entries(entries).sort((a, b) => b[1] - a[1])
  return (
    <section className="stats__breakdown">
      <h3 className="stats__breakdown-title">{title}</h3>
      {rows.length === 0 ? (
        <p className="stats__breakdown-empty">No data.</p>
      ) : (
        <ul className="stats__breakdown-list">
          {rows.map(([key, count]) => {
            const pct = total === 0 ? 0 : (count / total) * 100
            return (
              <li key={key} className="stats__breakdown-row">
                <span className="stats__breakdown-key">{key || '—'}</span>
                <span className="stats__breakdown-bar" aria-hidden="true">
                  <span className="stats__breakdown-bar-fill" style={{ width: `${pct.toFixed(1)}%` }} />
                </span>
                <span className="stats__breakdown-count">
                  {count} <span className="stats__breakdown-pct">({pct.toFixed(1)}%)</span>
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
