<!-- banner-badges -->
<p align="center">
  <a href="https://www.linkedin.com/in/soriamaximilianorodrigo/" target="_blank" rel="noopener noreferrer">
    <img width="100%" src="docs/img/banner.gif" alt="Async Job Engine — Maximiliano Rodrigo Soria">
  </a>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/MaximilianoRodrigoSoria/async-job-engine?style=flat-square&labelColor=1A1C1F&color=06C69C" alt="License"></a>
  <img src="https://img.shields.io/github/last-commit/MaximilianoRodrigoSoria/async-job-engine?style=flat-square&labelColor=1A1C1F&color=06C69C" alt="Last commit">
  <img src="https://img.shields.io/github/repo-size/MaximilianoRodrigoSoria/async-job-engine?style=flat-square&labelColor=1A1C1F&color=06C69C" alt="Repo size">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-06C69C?style=flat-square&labelColor=1A1C1F&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring_Boot-4.x-06C69C?style=flat-square&labelColor=1A1C1F&logo=springboot&logoColor=white" alt="Spring_Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-•-06C69C?style=flat-square&labelColor=1A1C1F&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-•-06C69C?style=flat-square&labelColor=1A1C1F&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Resilience4j-•-06C69C?style=flat-square&labelColor=1A1C1F" alt="Resilience4j">
</p>

# Async Job Engine

Motor de procesamiento asincrono: la API encola tareas pesadas y un pool de workers las procesa en segundo plano, con reintentos, prioridades, scheduling y una maquina de estados por job.

> Proyecto de portafolio backend. Sigue el estandar de **arquitectura hexagonal (Ports & Adapters)**, Java 21 y Spring Boot, con quality gates (Spotless, Checkstyle, PMD, SpotBugs, ArchUnit), testing con Testcontainers y observabilidad (Micrometer + Prometheus).

## Caracteristicas

- Encolado con respuesta inmediata `202 Accepted` e id de seguimiento
- Pool de workers con Virtual Threads y claim atomico (`SKIP LOCKED`)
- Reintentos con backoff exponencial + jitter y limite de intentos
- Prioridades (HIGH / NORMAL / LOW)
- Tareas programadas: diferidas (`runAt`) y recurrentes (cron)
- Maquina de estados con Dead-Letter Queue y reproceso
- Idempotencia por `Idempotency-Key`
- Metricas de cola, latencia y tasa de fallo (Micrometer + Prometheus)

## Stack

Java 21 · Spring Boot 4.x · PostgreSQL · Redis · Resilience4j · Gradle · Flyway · Docker · JUnit 5 · Testcontainers

## Arquitectura

Organizado por **feature** en capas `domain -> application -> infrastructure`, con la regla de dependencia verificada por ArchUnit. La logica de negocio (dominio y casos de uso) no depende de framework ni de infraestructura; los adaptadores (web, persistencia, mensajeria) implementan puertos definidos por la aplicacion.

## API

Contexto: `/async-job-engine/api/v1`. Documentacion OpenAPI en `/async-job-engine/swagger-ui.html`.

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| `POST` | `/jobs` | Encola un job (`202`). Body: `jobType`, `payload?`, `priority?`, `maxAttempts?`, `runAt?`. Header opcional `Idempotency-Key`. |
| `GET` | `/jobs/{id}` | Estado y resultado del job |
| `GET` | `/jobs` | Lista/filtra por `status`, `jobType`, `priority` (paginado) |
| `POST` | `/jobs/{id}/cancel` | Cancela un job PENDING o RETRYING |
| `POST` | `/jobs/{id}/retry` | Reintenta manualmente un job FAILED/CANCELLED |
| `GET` | `/jobs/dead-letter` | Lista la Dead-Letter Queue (jobs FAILED) |
| `POST` | `/jobs/dead-letter/{id}/reprocess` | Reprocesa un job desde la DLQ |

Handlers de ejemplo incluidos: `echo` y `email-notification`. Agregar un tipo nuevo es implementar `JobHandler` como un `@Component` — el motor lo descubre solo.

## Correr localmente

```bash
docker compose up -d            # PostgreSQL + Redis
./gradlew bootRun               # arranca en http://localhost:8080/async-job-engine

# Encolar un job
curl -X POST http://localhost:8080/async-job-engine/api/v1/jobs \
  -H 'Content-Type: application/json' \
  -d '{"jobType":"echo","payload":{"msg":"hola"},"priority":"HIGH"}'
```

## Estado

✅ Nucleo funcional implementado: encolado, worker con claim atomico (`SKIP LOCKED`), reintentos con backoff, prioridades, tareas diferidas, maquina de estados, DLQ, idempotencia, reaper de jobs zombie, API REST y tests (unit + Testcontainers). La feature `example` del scaffold se conserva como referencia y puede eliminarse.

---

<p align="center">
  <strong>Maximiliano Rodrigo Soria</strong><br>
  <a href="https://www.linkedin.com/in/soriamaximilianorodrigo/">LinkedIn</a> · <a href="mailto:maximilianorodrigosoria@gmail.com">maximilianorodrigosoria@gmail.com</a>
</p>
