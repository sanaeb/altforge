# AltForge

AI alt-text generator for WCAG accessibility compliance. Drop an image in your browser, get a clean, descriptive alt text in seconds — in French or English. Built ahead of the European Accessibility Act (June 2025) deadline.

**Live demo:** [altforge.pages.dev](https://altforge.pages.dev)
**API:** [altforge.onrender.com](https://altforge.onrender.com/actuator/health)

> The backend runs on Render's free tier and sleeps after inactivity. First request after a cold start takes ~30–60s; subsequent requests are fast.

---

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 17 · Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) · Lombok · JUnit 5 · H2 (tests) |
| Frontend | React 18 · TypeScript · Vite |
| LLM Vision | Google Gemini (`gemini-2.5-flash-lite`) |
| Persistence | PostgreSQL 17 (Neon) · Flyway migrations · HikariCP |
| Build & deploy | Docker (multi-stage) · Render (backend) · Cloudflare Pages (frontend) |

---

## API

### `POST /api/alt-text`

Single-image multipart upload. Returns the generated alt text plus metadata.

| Field | Type | Required | Notes |
|---|---|---|---|
| `image` | file | yes | JPEG, PNG, WebP, GIF. Max 10 MB. |
| `lang` | string | no | `fr` or `en`. Defaults to `en`. |

**Example**

```bash
curl -X POST "https://altforge.onrender.com/api/alt-text?lang=fr" \
  -F "image=@photo.jpg"
```

**Response**

```json
{
  "altText": "Une femme en kimono bleu fleuri marche dans une rue pavée bordée de maisons traditionnelles japonaises avec une pagode au loin.",
  "language": "fr",
  "model": "gemini-2.5-flash-lite",
  "fileName": "photo.jpg",
  "sizeBytes": 159642
}
```

### `POST /api/alt-text/batch`

Process up to 10 images in one request. Always returns HTTP 200 when the request itself is valid; per-image failures are reported through individual `items[].error` codes (`empty_file`, `invalid_image`, `too_large`, `gemini_unavailable`, `io_error`).

| Field | Type | Required | Notes |
|---|---|---|---|
| `images` | file[] | yes | Up to 10 files, 10 MB each, 60 MB total per request. |
| `lang` | string | no | `fr` or `en`. Defaults to `en`. |

**Example**

```bash
curl -X POST "https://altforge.onrender.com/api/alt-text/batch?lang=en" \
  -F "images=@a.jpg" \
  -F "images=@b.png" \
  -F "images=@c.webp"
```

**Response**

```json
{
  "model": "gemini-2.5-flash-lite",
  "succeeded": 2,
  "failed": 1,
  "items": [
    { "fileName": "a.jpg", "altText": "A red apple on a wooden table.", "language": "en", "sizeBytes": 84211 },
    { "fileName": "b.png", "altText": "A blue mug next to an open book.", "language": "en", "sizeBytes": 122044 },
    { "fileName": "c.webp", "sizeBytes": 12000000, "error": "too_large" }
  ]
}
```

### `GET /api/stats`

Aggregated audit metrics over a configurable rolling window (defaults to the last 24 hours). Counts, success rate, average latency, and breakdowns by language and endpoint.

| Field | Type | Required | Notes |
|---|---|---|---|
| `hours` | int | no | Window size in hours. Defaults to 24, capped at 720 (30 days). |

**Example**

```bash
curl "https://altforge.onrender.com/api/stats?hours=168"
```

**Response**

```json
{
  "totalRequests": 142,
  "succeededRequests": 138,
  "failedRequests": 4,
  "successRatePct": 97.2,
  "avgLatencyMs": 814.5,
  "byLanguage": { "en": 96, "fr": 46 },
  "byEndpoint": { "single": 110, "batch": 32 },
  "windowHours": 168
}
```

Stats are computed from the `request_audits` table populated by a Spring `HandlerInterceptor` around every call to `/api/alt-text*`. Client IPs are SHA-256 hashed before storage so the table never contains raw personal data.

### `GET /actuator/health`

Spring Boot health probe. Returns `{"status":"UP"}` when the service is ready.

---

## Local development

### Prerequisites

- JDK 17+
- Node.js 20+
- A Google Gemini API key ([aistudio.google.com](https://aistudio.google.com/apikey))

### Backend

```bash
cd backend
export GEMINI_API_KEY=your_key_here
./mvnw spring-boot:run
# → http://localhost:8080
```

Run the tests:

```bash
./mvnw test
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

The dev server proxies API calls to `http://localhost:8080` by default. To point to a different backend, copy `.env.example` to `.env.local` and set `VITE_API_BASE_URL`.

---

## Configuration

The backend reads its configuration from environment variables (see `application.properties` for defaults):

| Variable | Purpose |
|---|---|
| `GEMINI_API_KEY` | Google Gemini API key (required) |
| `GEMINI_MODEL` | Override the vision model (default `gemini-2.0-flash`) |
| `GEMINI_BASE_URL` | Override the Gemini endpoint |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Comma-separated origin patterns allowed on `/api/**` |
| `SPRING_DATASOURCE_URL` | JDBC URL of the audit Postgres (e.g. `jdbc:postgresql://host/altforge?sslmode=require`) |
| `SPRING_DATASOURCE_USERNAME` | Database role used by Spring Data JPA |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `PORT` / `SERVER_PORT` | HTTP port (default 8080) |

---

## Deployment

The repo includes a multi-stage `backend/Dockerfile` (Eclipse Temurin 17 JDK builder → JRE runtime) used by Render. The frontend builds with `npm run build` and Cloudflare Pages serves the `dist/` output. Both auto-deploy on push to `main`.

---

## Roadmap

- **v0 (shipped):** upload one image, pick FR/EN, generate alt text, copy to clipboard
- **v1 (shipped):** batch upload (up to 10 images per request) with per-image error handling, CSV/JSON export, single/batch mode toggle in the UI
- **v2.0 (shipped):** audit DB (Postgres + Flyway), `GET /api/stats` with aggregates, stats tab in the UI
- **v2.1:** per-IP rate limiting backed by the audit table (`429 Too Many Requests`)
- **v2.2:** async queue (Spring Batch / `@Async`), polling endpoint, job table
- **v3:** auth, billing, public alt-text-as-a-service
