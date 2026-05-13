import { useCallback, useEffect, useRef, useState } from 'react'
import { generateAltText, ApiError, type AltTextLanguage } from '../services/api'
import type { AltTextResponse } from '../types/api'
import './Generator.css'

const ACCEPTED_TYPES = 'image/png,image/jpeg,image/webp,image/gif'
const MAX_BYTES = 10 * 1024 * 1024

const LANGUAGES: { code: AltTextLanguage; label: string }[] = [
  { code: 'en', label: 'EN' },
  { code: 'fr', label: 'FR' },
]

export function Generator() {
  const [file, setFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [result, setResult] = useState<AltTextResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  const [copied, setCopied] = useState(false)
  const [language, setLanguage] = useState<AltTextLanguage>('en')
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(file)
    setPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  const handleFile = useCallback((next: File | null) => {
    setError(null)
    setResult(null)
    setCopied(false)
    if (!next) {
      setFile(null)
      return
    }
    if (!next.type.startsWith('image/')) {
      setError('Only image files are supported.')
      return
    }
    if (next.size > MAX_BYTES) {
      setError('Image must be 10 MB or less.')
      return
    }
    setFile(next)
  }, [])

  const handleDrop = useCallback(
    (event: React.DragEvent<HTMLLabelElement>) => {
      event.preventDefault()
      setDragging(false)
      const dropped = event.dataTransfer.files?.[0] ?? null
      handleFile(dropped)
    },
    [handleFile],
  )

  const handleGenerate = useCallback(async () => {
    if (!file) return
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const response = await generateAltText(file, language)
      setResult(response)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else if (err instanceof Error) {
        setError(err.message)
      } else {
        setError('Unexpected error.')
      }
    } finally {
      setLoading(false)
    }
  }, [file, language])

  const handleCopy = useCallback(async () => {
    if (!result) return
    try {
      await navigator.clipboard.writeText(result.altText)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      setError('Could not copy to the clipboard.')
    }
  }, [result])

  const handleReset = useCallback(() => {
    setFile(null)
    setResult(null)
    setError(null)
    setCopied(false)
    if (inputRef.current) inputRef.current.value = ''
  }, [])

  return (
    <div className="generator">
      <div className="lang-toggle" role="group" aria-label="Output language">
        {LANGUAGES.map((lang) => (
          <button
            key={lang.code}
            type="button"
            className={`lang-toggle__btn ${language === lang.code ? 'lang-toggle__btn--active' : ''}`}
            onClick={() => setLanguage(lang.code)}
            aria-pressed={language === lang.code}
          >
            {lang.label}
          </button>
        ))}
        <span className="lang-toggle__hint">output language</span>
      </div>

      <label
        className={`dropzone ${dragging ? 'dropzone--active' : ''} ${file ? 'dropzone--with-file' : ''}`}
        onDragEnter={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragOver={(e) => e.preventDefault()}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
      >
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES}
          onChange={(e) => handleFile(e.target.files?.[0] ?? null)}
          className="dropzone__input"
        />
        {previewUrl ? (
          <img src={previewUrl} alt="" className="dropzone__preview" />
        ) : (
          <div className="dropzone__hint">
            <strong>Drop an image here</strong>
            <span>or click to browse · PNG, JPEG, WebP, GIF up to 10 MB</span>
          </div>
        )}
      </label>

      <div className="actions">
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleGenerate}
          disabled={!file || loading}
        >
          {loading ? 'Generating…' : 'Generate alt text'}
        </button>
        {file && !loading && (
          <button type="button" className="btn btn--ghost" onClick={handleReset}>
            Reset
          </button>
        )}
      </div>

      {error && <div className="alert alert--error" role="alert">{error}</div>}

      {result && (
        <article className="result">
          <header className="result__meta">
            <span className="result__chip">{result.model}</span>
            <span className="result__chip result__chip--muted">lang · {result.language}</span>
            <span className="result__chip result__chip--muted">{formatBytes(result.sizeBytes)}</span>
          </header>
          <p className="result__text">{result.altText}</p>
          <footer className="result__actions">
            <button type="button" className="btn btn--secondary" onClick={handleCopy}>
              {copied ? 'Copied ✓' : 'Copy alt text'}
            </button>
            <code className="result__filename">{result.fileName}</code>
          </footer>
        </article>
      )}
    </div>
  )
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
