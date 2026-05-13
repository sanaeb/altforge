import { Generator } from './components/Generator'
import './App.css'

function App() {
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

      <section className="app__main">
        <Generator />
      </section>

      <footer className="app__footer">
        <span className="app__footer-chip">v0 · live</span>
        <span>Wired to <code>POST /api/alt-text</code></span>
      </footer>
    </main>
  )
}

export default App
