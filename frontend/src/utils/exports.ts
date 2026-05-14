import type { BatchAltTextResponse } from '../types/api'

/** Trigger a browser download for the given text content. */
function downloadFile(content: string, mimeType: string, fileName: string): void {
  const blob = new Blob([content], { type: `${mimeType};charset=utf-8` })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * RFC 4180 CSV escaping — wraps every field in double quotes and doubles
 * any embedded double quotes. A BOM is prepended so Excel under FR locales
 * detects UTF-8 correctly.
 */
function csvEscape(value: string | number | null): string {
  if (value === null || value === undefined) return '""'
  const str = String(value).replace(/"/g, '""')
  return `"${str}"`
}

/** UTF-8 BOM, required for Excel under FR locales to detect encoding. */
const UTF8_BOM = '﻿'

export function toCsv(response: BatchAltTextResponse): string {
  const header = ['file_name', 'alt_text', 'language', 'size_bytes', 'error'].map(csvEscape).join(',')
  const rows = response.items.map((item) =>
    [item.fileName, item.altText, item.language, item.sizeBytes, item.error].map(csvEscape).join(','),
  )
  return UTF8_BOM + [header, ...rows].join('\r\n')
}

export function downloadCsv(response: BatchAltTextResponse, fileName = 'altforge-batch.csv'): void {
  downloadFile(toCsv(response), 'text/csv', fileName)
}

export function downloadJson(response: BatchAltTextResponse, fileName = 'altforge-batch.json'): void {
  downloadFile(JSON.stringify(response, null, 2), 'application/json', fileName)
}
