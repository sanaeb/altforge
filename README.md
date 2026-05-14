# AltForge

AI alt-text generator for WCAG accessibility compliance. Drop an image in your browser, get a clean, descriptive alt text in seconds — in French or English. Built ahead of the European Accessibility Act (June 2025) deadline.

**Live demo:** [altforge.pages.dev](https://altforge.pages.dev)
**API:** [altforge.onrender.com](https://altforge.onrender.com/actuator/health)

> The backend runs on Render's free tier and sleeps after inactivity. First request after a cold start takes ~30–60s; subsequent requests are fast.

---

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 17 · Spring Boot 3.5 (Web, Validation, Actuator) · Lombok · JUnit 5 |
| Frontend | React 18 · TypeScript · Vite |
| LLM Vision | Google Gemini (`gemini-2.5-flash-lite`) |
| Build & deploy | Docker (multi-stage) · Render (backend) · Cloudflare Pages (frontend) |

---

## API

### `POST /api/alt-text`

Multipart upload. Returns the generated alt text plus metadata.

| Field | Type | Required | Notes |
|---|---|---|---|
| `image` | file | yes | JPEG, PNG, WebP, GIF. Max 10 MB. |
| `language` | string | no | `fr` or `en`. Defaults to `en`. |

**Example**

```bash
curl -X POST https://altforge.onrender.com/api/alt-text \
  -F "image=@photo.jpg" \
  -F "language=fr"
```

**Response**

```json
{
  "altText": "Une femme en kimono bleu fleuri marche dans une rue pavée bordée de maisons traditionnelles japonaises avec une pagode au loin.",
  "language": "fr",
  "model": "gemini-2.5-flash-lite",
  "sizeBytes": 159642
}
```

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
| `PORT` / `SERVER_PORT` | HTTP port (default 8080) |

---

## Deployment

The repo includes a multi-stage `backend/Dockerfile` (Eclipse Temurin 17 JDK builder → JRE runtime) used by Render. The frontend builds with `npm run build` and Cloudflare Pages serves the `dist/` output. Both auto-deploy on push to `main`.

---

## Roadmap

- **v0 (shipped):** upload one image, pick FR/EN, generate alt text, copy to clipboard
- **v1:** batch upload (10–100 images), CSV/JSON export
- **v2:** async queue (Spring Batch), per-user quotas, audit DB
- **v3:** auth, billing, public alt-text-as-a-service
