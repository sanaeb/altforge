import { useCallback, useEffect, useRef, useState } from 'react'
import {
  ApiError,
  MAX_BATCH_SIZE,
  fetchJob,
  submitAsyncBatch,
  type AltTextLanguage,
} from '../services/api'
import type { BatchJobStatusResponse } from '../types/api'
import './BatchGenerator.css'
import './AsyncBatchGenerator.css'

const ACCEPTED_TYPES = 'image/png,image/jpeg,image/webp,image/gif'
const MAX_BYTES = 10 * 1024 * 1024
const POLL_MS = 2000

const LANGUAGES: { code: AltTextLanguage; label: string }[] = [
  { code: 'en', label: 'EN' },
  { code: 'fr', label: 'FR' },
]

const ERROR_LABELS: Record<string, string> = {
  empty_file: 'Empty file',
  invalid_image: 'Not an image',
  too_large: 'File over 10 MB',
  gemini_unavailable: 'Gemini error',
  internal_error: 'Server error',
  io_error: 'Could not read file',
}

export function AsyncBatchGenerator() {
  const [files, setFiles] = useState<File[]>([])
  const [language, setLanguage] = useState<AltTextLanguage>('en')
  const [job, setJob] = useState<BatchJobStatusResponse | null>(null)
  const [jobId, setJobId] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const pollTimer = useRef<number | null>(null)

  const stopPolling = useCallback(() => {
    if (pollTimer.current !== null) {
      window.clearTimeout(pollTimer.current)
      pollTimer.current = null
    }
  }, [])

  useEffect(() => () => stopPolling(), [stopPolling])

  const addFiles = useCallback((incoming: FileList | File[] | null) => {
    if (!incoming) return
    setError(null)
    const incomingArray = Array.from(incoming)
    const valid: File[] = []
    const rejected: string[] = []

    for (const f of incomingArray) {
      if (!f.type.startsWith('image/')) rejected.push(`${f.name} (not an image)`)
      else if (f.size > MAX_BYTES) rejected.push(`${f.name} (over 10 MB)`)
      else valid.push(f)
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

  const handleReset = useCallback(() => {
    stopPolling()
    setFiles([])
    setJob(null)
    setJobId(null)
    setError(null)
    setCopiedIndex(null)
    if (inputRef.current) inputRef.current.value = ''
  }, [stopPolling])

  const pollOnce = useCallback(
    async (id: string) => {
      try {
        const next = await fetchJob(id)
        setJob(next)
        if (next.status === 'SUCCEEDED' || next.status === 'FAILED') {
          stopPolling()
          return
        }
        pollTimer.current = window.setTimeout(() => pollOnce(id), POLL_MS)
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Polling failed.')
        stopPolling()
      }
    },
    [stopPolling],
  )

  const handleSubmit = useCallback(async () => {
    if (files.length === 0) return
    setSubmitting(true)
    setError(null)
    setJob(null)
    setJobId(null)
    setCopiedIndex(null)
    try {
      const submission = await submitAsyncBatch(files, language)
      setJobId(submission.id)
      // Immediately fetch + start polling
      await pollOnce(submission.id)
    } catch (err) {
      if (err instanceof ApiError || err instanceof Error) setError(err.message)
      else setError('Unexpected error.')
    } finally {
      setSubmitting(false)
    }
  }, [files, language, pollOnce])

  const handleCopy = useCallback(async (text: string, index: number) => {
    try {
      await navigator.clipboard.writeText(text)
      setCopiedIndex(index)
      setTimeout(() => setCopiedIndex((c) => (c === index ? null : c)), 2000)
    } catch {
      setError('Could not copy to the clipboard.')
    }
  }, [])

  const removeFile = useCallback((index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index))
  }, [])

  const running = job?.status === 'PENDING' || job?.status === 'RUNNING'
  const canSubmit = files.length > 0 && !submitting && !running

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
            disabled={running}
          >
            {lang.label}
          </button>
        ))}
        <span className="lang-toggle__hint">output language</span>
      </div>

      {!jobId && (
        <>
          <label
            className={`dropzone batch-dropzone ${dragging ? 'dropzone--active' : ''}`}
            onDragEnter={(e) => {
              e.preventDefault()
              setDragging(true)
            }}
            onDragOver={(e) => e.preventDefault()}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => {
              e.preventDefault()
              setDragging(false)
              addFiles(e.dataTransfer.files)
            }}
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
              <span>Submit returns a job id immediately — results stream in as Gemini answers.</span>
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
                    disabled={submitting}
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
        </>
      )}

      <div className="actions">
        {!jobId ? (
          <button type="button" className="btn btn--primary" onClick={handleSubmit} disabled={!canSubmit}>
            {submitting ? 'Submitting…' : 'Submit job'}
          </button>
        ) : (
          <button type="button" className="btn btn--ghost" onClick={handleReset} disabled={running}>
            {running ? 'Running…' : 'New job'}
          </button>
        )}
      </div>

      {error && <div className="alert alert--error" role="alert">{error}</div>}

      {job && (
        <section className="async-job">
          <header className="async-job__header">
            <span className={`async-job__status async-job__status--${job.status.toLowerCase()}`}>
              {job.status}
            </span>
            <code className="async-job__id" title={job.id}>{job.id.slice(0, 8)}…</code>
            <span className="async-job__counter">
              {job.processedImages} / {job.totalImages}
            </span>
          </header>

          <div className="async-job__progress" aria-hidden="true">
            <div
              className="async-job__progress-fill"
              style={{ width: `${job.totalImages === 0 ? 0 : (job.processedImages / job.totalImages) * 100}%` }}
            />
          </div>

          <ol className="batch-results__list">
            {job.items.map((item, i) => (
              <li key={`${item.position}`} className="batch-results__item">
                <div className="batch-results__meta">
                  <code className="batch-results__filename">{item.fileName ?? '(no name)'}</code>
                  <span className="batch-results__size">{formatBytes(item.sizeBytes)}</span>
                  {item.language && (
                    <span className="result__chip result__chip--muted">{item.language}</span>
                  )}
                </div>
                {item.errorCode ? (
                  <p className="batch-results__error">
                    {ERROR_LABELS[item.errorCode] ?? item.errorCode}
                  </p>
                ) : item.altText ? (
                  <>
                    <p className="batch-results__text">{item.altText}</p>
                    <button
                      type="button"
                      className="btn btn--secondary batch-results__copy"
                      onClick={() => handleCopy(item.altText!, i)}
                    >
                      {copiedIndex === i ? 'Copied ✓' : 'Copy'}
                    </button>
                  </>
                ) : (
                  <p className="async-job__pending">Waiting for the worker…</p>
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
