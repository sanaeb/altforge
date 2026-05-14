import type {
  AltTextResponse,
  BatchAltTextResponse,
  BatchJobStatusResponse,
  BatchJobSubmitResponse,
  StatsResponse,
} from '../types/api'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** Maximum number of images accepted per batch by the backend. */
export const MAX_BATCH_SIZE = 10

/** Errors thrown by the API layer. */
export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export type AltTextLanguage = 'en' | 'fr'

/**
 * Upload one image and get a generated alt text back.
 * Throws {@link ApiError} on a non-2xx response.
 */
export async function generateAltText(
  image: File,
  language: AltTextLanguage = 'en',
): Promise<AltTextResponse> {
  const formData = new FormData()
  formData.append('image', image)

  const url = `${API_BASE}/api/alt-text?lang=${encodeURIComponent(language)}`
  const response = await fetch(url, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const message = await safeReadText(response)
    throw new ApiError(message || `Request failed (${response.status})`, response.status)
  }

  return response.json() as Promise<AltTextResponse>
}

/**
 * Upload up to {@link MAX_BATCH_SIZE} images at once. The backend always
 * returns 200 when the request itself is valid; per-image failures are
 * reported via {@link BatchItemResult.error}.
 */
export async function generateAltTextBatch(
  images: File[],
  language: AltTextLanguage = 'en',
): Promise<BatchAltTextResponse> {
  if (images.length === 0) {
    throw new ApiError('At least one image is required.', 400)
  }
  if (images.length > MAX_BATCH_SIZE) {
    throw new ApiError(`Batch size is limited to ${MAX_BATCH_SIZE} images.`, 400)
  }

  const formData = new FormData()
  for (const image of images) {
    formData.append('images', image)
  }

  const url = `${API_BASE}/api/alt-text/batch?lang=${encodeURIComponent(language)}`
  const response = await fetch(url, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const message = await safeReadText(response)
    throw new ApiError(message || `Request failed (${response.status})`, response.status)
  }

  return response.json() as Promise<BatchAltTextResponse>
}

/**
 * Submit an async batch. Returns immediately with a job id; poll
 * {@link fetchJob} to follow progress.
 */
export async function submitAsyncBatch(
  images: File[],
  language: AltTextLanguage = 'en',
): Promise<BatchJobSubmitResponse> {
  if (images.length === 0) {
    throw new ApiError('At least one image is required.', 400)
  }
  if (images.length > MAX_BATCH_SIZE) {
    throw new ApiError(`Batch size is limited to ${MAX_BATCH_SIZE} images.`, 400)
  }

  const formData = new FormData()
  for (const image of images) {
    formData.append('images', image)
  }

  const url = `${API_BASE}/api/alt-text/batch/async?lang=${encodeURIComponent(language)}`
  const response = await fetch(url, { method: 'POST', body: formData })

  if (!response.ok) {
    const message = await safeReadText(response)
    throw new ApiError(message || `Request failed (${response.status})`, response.status)
  }

  return response.json() as Promise<BatchJobSubmitResponse>
}

export async function fetchJob(jobId: string): Promise<BatchJobStatusResponse> {
  const url = `${API_BASE}/api/jobs/${encodeURIComponent(jobId)}`
  const response = await fetch(url)

  if (!response.ok) {
    const message = await safeReadText(response)
    throw new ApiError(message || `Request failed (${response.status})`, response.status)
  }

  return response.json() as Promise<BatchJobStatusResponse>
}

/**
 * Fetch aggregated audit stats over the given window (defaults to 24h on the
 * backend).
 */
export async function fetchStats(hours = 24): Promise<StatsResponse> {
  const url = `${API_BASE}/api/stats?hours=${hours}`
  const response = await fetch(url)

  if (!response.ok) {
    const message = await safeReadText(response)
    throw new ApiError(message || `Request failed (${response.status})`, response.status)
  }

  return response.json() as Promise<StatsResponse>
}

async function safeReadText(response: Response): Promise<string> {
  try {
    return await response.text()
  } catch {
    return ''
  }
}
