/** Shape of the JSON returned by `POST /api/alt-text`. */
export interface AltTextResponse {
  altText: string
  language: string
  model: string
  fileName: string
  sizeBytes: number
}

/** One entry in a batch response. Either `altText`/`language` are set, or `error` is. */
export interface BatchItemResult {
  fileName: string
  altText: string | null
  language: string | null
  sizeBytes: number
  error: string | null
}

/** Shape of the JSON returned by `POST /api/alt-text/batch`. */
export interface BatchAltTextResponse {
  model: string
  succeeded: number
  failed: number
  items: BatchItemResult[]
}

/** Returned by `POST /api/alt-text/batch/async`. */
export interface BatchJobSubmitResponse {
  id: string
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  totalImages: number
  acceptedImages: number
  rejectedImages: number
}

/** One item inside a `GET /api/jobs/{id}` response. */
export interface BatchJobItem {
  position: number
  fileName: string | null
  sizeBytes: number
  altText: string | null
  language: string | null
  errorCode: string | null
  completedAt: string | null
}

/** Returned by `GET /api/jobs/{id}`. */
export interface BatchJobStatusResponse {
  id: string
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  createdAt: string
  startedAt: string | null
  completedAt: string | null
  model: string
  language: string
  totalImages: number
  processedImages: number
  errorCode: string | null
  items: BatchJobItem[]
}

/** Shape of the JSON returned by `GET /api/stats`. */
export interface StatsResponse {
  totalRequests: number
  succeededRequests: number
  failedRequests: number
  successRatePct: number
  avgLatencyMs: number | null
  byLanguage: Record<string, number>
  byEndpoint: Record<string, number>
  windowHours: number
}
