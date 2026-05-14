import { useState } from 'react'
import { Generator } from './components/Generator'
import { BatchGenerator } from './components/BatchGenerator'
import './App.css'

type Mode = 'single' | 'batch'

const MODES: { value: Mode; label: string; hint: string }[] = [
  { value: 'single', label: 'One image', hint: 'POST /api/alt-text' },
  { value: 'batch', label: 'Batch', hint: 'POST /api/alt-text/batch' },
]

function App() {
  const [mode, setMode] = useState<Mode>('single')

  return (
    <main className="app">
      <header className="app__header">
        <div className="app__brand">
          <span className="app__logo" aria-hidden="true">A</span>
          <span className="app__title">AltForge</span>
        </div>
        <p className="app__tagline">
          AI alt text for your images — generated in seconds, WCAG-friendly.
        </p>
      </header>

      <nav className="app__modes" role="tablist" aria-label="Generation mode">
        {MODES.map((m) => (
          <button
            key={m.value}
            type="button"
            role="tab"
            aria-selected={mode === m.value}
            className={`app__mode ${mode === m.value ? 'app__mode--active' : ''}`}
            onClick={() => setMode(m.value)}
          >
            {m.label}
          </button>
        ))}
      </nav>

      <section className="app__main">
        {mode === 'single' ? <Generator /> : <BatchGenerator />}
      </section>

      <footer className="app__footer">
        <span className="app__footer-chip">v1 · live</span>
        <span>Wired to <code>{MODES.find((m) => m.value === mode)?.hint}</code></span>
      </footer>
    </main>
  )
}

export default App
