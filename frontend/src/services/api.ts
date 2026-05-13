import type { AltTextResponse } from '../types/api'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** Errors thrown by the API layer. */
export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/**
 * Upload one image and get a generated alt text back.
 * Throws {@link ApiError} on a non-2xx response.
 */
export async function generateAltText(image: File): Promise<AltTextResponse> {
  const formData = new FormData()
  formData.append('image', image)

  const response = await fetch(`${API_BASE}/api/alt-text`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    const message = await safeReadText(response)
    throw new ApiError(message || `Request failed (${response.status})`, response.status)
  }

  return response.json() as Promise<AltTextResponse>
}

async function safeReadText(response: Response): Promise<string> {
  try {
    return await response.text()
  } catch {
    return ''
  }
}
