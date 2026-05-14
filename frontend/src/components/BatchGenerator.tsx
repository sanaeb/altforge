import { useCallback, useRef, useState } from 'react'
import {
  generateAltTextBatch,
  ApiError,
  MAX_BATCH_SIZE,
  type AltTextLanguage,
} from '../services/api'
import type { BatchAltTextResponse } from '../types/api'
import { downloadCsv, downloadJson } from '../utils/exports'
import './BatchGenerator.css'

const ACCEPTED_TYPES = 'image/png,image/jpeg,image/webp,image/gif'
const MAX_BYTES = 10 * 1024 * 1024

const LANGUAGES: { code: AltTextLanguage; label: string }[] = [
  { code: 'en', label: 'EN' },
  { code: 'fr', label: 'FR' },
]

const ERROR_LABELS: Record<string, string> = {
  empty_file: 'Empty file',
  invalid_image: 'Not an image',
  too_large: 'File over 10 MB',
  gemini_unavailable: 'Gemini error',
  io_error: 'Could not read file',
}

export function BatchGenerator() {
  const [files, setFiles] = useState<File[]>([])
  const [results, setResults] = useState<BatchAltTextResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  const [language, setLanguage] = useState<AltTextLanguage>('en')
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const addFiles = useCallback((incoming: FileList | File[] | null) => {
    if (!incoming) return
    setError(null)
    setResults(null)
    setCopiedIndex(null)
    const incomingArray = Array.from(incoming)
    const valid: File[] = []
    const rejected: string[] = []

    for (const f of incomingArray) {
      if (!f.type.startsWith('image/')) {
        rejected.push(`${f.name} (not an image)`)
      } else if (f.size > MAX_BYTES) {
        rejected.push(`${f.name} (over 10 MB)`)
      } else {
        valid.push(f)
      }
    }

    setFiles((prev) => {
      const combined = [...prev, ...valid]
      if (combined.length > MAX_BATCH_SIZE) {
        rejected.push(`only ${MAX_BATCH_SIZE} files per batch`)
        return combined.slice(0, MAX_BATCH_SIZE)
      }
      return combined
    })

    if (rejected.length > 0) {
      setError(`Skipped: ${rejected.join(', ')}`)
    }
  }, [])

  const handleDrop = useCallback(
    (event: React.DragEvent<HTMLLabelElement>) => {
      event.preventDefault()
      setDragging(false)
      addFiles(event.dataTransfer.files)
    },
    [addFiles],
  )

  const removeFile = useCallback((index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index))
    setResults(null)
    setCopiedIndex(null)
  }, [])

  const handleReset = useCallback(() => {
    setFiles([])
    setResults(null)
    setError(null)
    setCopiedIndex(null)
    if (inputRef.current) inputRef.current.value = ''
  }, [])

  const handleGenerate = useCallback(async () => {
    if (files.length === 0) return
    setLoading(true)
    setError(null)
    setResults(null)
    setCopiedIndex(null)
    try {
      const response = await generateAltTextBatch(files, language)
      setResults(response)
    } catch (err) {
      if (err instanceof ApiError || err instanceof Error) {
        setError(err.message)
      } else {
        setError('Unexpected error.')
      }
    } finally {
      setLoading(false)
    }
  }, [files, language])

  const handleCopy = useCallback(async (text: string, index: number) => {
    try {
      await navigator.clipboard.writeText(text)
      setCopiedIndex(index)
      setTimeout(() => setCopiedIndex((current) => (current === index ? null : current)), 2000)
    } catch {
      setError('Could not copy to the clipboard.')
    }
  }, [])

  const canGenerate = files.length > 0 && !loading

  return (
    <div className="batch">
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
        className={`dropzone batch-dropzone ${dragging ? 'dropzone--active' : ''}`}
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
          multiple
          accept={ACCEPTED_TYPES}
          onChange={(e) => addFiles(e.target.files)}
          className="dropzone__input"
        />
        <div className="dropzone__hint">
          <strong>Drop up to {MAX_BATCH_SIZE} images</strong>
          <span>or click to browse · PNG, JPEG, WebP, GIF up to 10 MB each</span>
        </div>
      </label>

      {files.length > 0 && (
        <ul className="batch-list" aria-label="Selected images">
          {files.map((file, i) => (
            <li key={`${file.name}-${i}`} className="batch-list__item">
              <span className="batch-list__name" title={file.name}>{file.name}</span>
              <span className="batch-list__size">{formatBytes(file.size)}</span>
              <button
                type="button"
                className="batch-list__remove"
                onClick={() => removeFile(i)}
                aria-label={`Remove ${file.name}`}
                disabled={loading}
              >
                ×
              </button>
            </li>
          ))}
          <li className="batch-list__count">
            {files.length} / {MAX_BATCH_SIZE} images selected
          </li>
        </ul>
      )}

      <div className="actions">
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleGenerate}
          disabled={!canGenerate}
        >
          {loading ? `Generating ${files.length} image${files.length > 1 ? 's' : ''}…` : 'Generate alt text'}
        </button>
        {files.length > 0 && !loading && (
          <button type="button" className="btn btn--ghost" onClick={handleReset}>
            Reset
          </button>
        )}
      </div>

      {error && <div className="alert alert--error" role="alert">{error}</div>}

      {results && (
        <section className="batch-results">
          <header className="batch-results__header">
            <div className="batch-results__summary">
              <span className="result__chip">{results.model}</span>
              <span className="result__chip result__chip--muted">
                {results.succeeded} ok
              </span>
              {results.failed > 0 && (
                <span className="result__chip batch-results__chip--error">
                  {results.failed} failed
                </span>
              )}
            </div>
            <div className="batch-results__exports">
              <button
                type="button"
                className="btn btn--secondary"
                onClick={() => downloadCsv(results)}
              >
                Download CSV
              </button>
              <button
                type="button"
                className="btn btn--secondary"
                onClick={() => downloadJson(results)}
              >
                Download JSON
              </button>
            </div>
          </header>

          <ol className="batch-results__list">
            {results.items.map((item, i) => (
              <li key={`${item.fileName}-${i}`} className="batch-results__item">
                <div className="batch-results__meta">
                  <code className="batch-results__filename" title={item.fileName ?? ''}>
                    {item.fileName ?? '(no name)'}
                  </code>
                  <span className="batch-results__size">{formatBytes(item.sizeBytes)}</span>
                  {item.language && (
                    <span className="result__chip result__chip--muted">{item.language}</span>
                  )}
                </div>
                {item.error ? (
                  <p className="batch-results__error">
                    {ERROR_LABELS[item.error] ?? item.error}
                  </p>
                ) : (
                  <>
                    <p className="batch-results__text">{item.altText}</p>
                    <button
                      type="button"
                      className="btn btn--secondary batch-results__copy"
                      onClick={() => item.altText && handleCopy(item.altText, i)}
                    >
                      {copiedIndex === i ? 'Copied ✓' : 'Copy'}
                    </button>
                  </>
                )}
              </li>
            ))}
          </ol>
        </section>
      )}
    </div>
  )
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
