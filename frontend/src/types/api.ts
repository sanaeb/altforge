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
