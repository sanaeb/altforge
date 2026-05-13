/** Shape of the JSON returned by `POST /api/alt-text`. */
export interface AltTextResponse {
  altText: string
  language: string
  model: string
  fileName: string
  sizeBytes: number
}
