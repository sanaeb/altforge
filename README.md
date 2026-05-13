# AltForge

AI-powered alt-text generator for WCAG accessibility compliance. Upload images, get clean, descriptive alt text in seconds. Built ahead of the European Accessibility Act (June 2025) deadline.

> **Status:** scaffolding · `v0.0.1`

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 17 · Spring Boot 3.5 · Spring Web · Validation · Actuator · Lombok |
| Frontend | React 18 · TypeScript · Vite |
| LLM Vision | Google Gemini Vision (later) |
| Storage | Cloudflare R2 (S3-compatible, later) |
| DB | PostgreSQL 15 via Neon (later) |
| Deploy | Frontend → Cloudflare Pages · Backend → Fly.io (free tier) |

## Local development

### Backend

```bash
cd backend
./mvnw spring-boot:run
# API on http://localhost:8080
# Health: http://localhost:8080/actuator/health
```

### Frontend

```bash
cd frontend
npm run dev
# UI on http://localhost:5173
```

## Roadmap

- **v0 (MVP):** upload one image → generate alt text → display + copy
- **v1:** batch upload (10-100 images), CSV/JSON download
- **v2:** queue async (Spring Batch), quotas, audit DB
- **v3:** auth, billing, public alt-text-as-a-service
