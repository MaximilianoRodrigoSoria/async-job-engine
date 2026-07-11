-- ─────────────────────────────────────────────────────────────────────────────
-- Cola de jobs del motor de procesamiento asíncrono.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS app.job (
    id               UUID         PRIMARY KEY,
    job_type         VARCHAR(120) NOT NULL,
    payload          TEXT         NOT NULL DEFAULT '{}',
    status           VARCHAR(20)  NOT NULL,
    priority         INTEGER      NOT NULL DEFAULT 1,
    idempotency_key  VARCHAR(200),
    attempts         INTEGER      NOT NULL DEFAULT 0,
    max_attempts     INTEGER      NOT NULL DEFAULT 3,
    scheduled_at     TIMESTAMP  ,
    next_attempt_at  TIMESTAMP  ,
    locked_by        VARCHAR(200),
    locked_at        TIMESTAMP  ,
    last_error       TEXT,
    result           TEXT,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

-- Índice que sirve al claim de la cola: elige por estado, prioridad (desc) y antigüedad (asc).
CREATE INDEX IF NOT EXISTS idx_job_claim
    ON app.job (status, priority DESC, created_at);

-- El poller filtra por ventana de ejecución vencida.
CREATE INDEX IF NOT EXISTS idx_job_next_attempt_at ON app.job (next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_job_scheduled_at    ON app.job (scheduled_at);

-- El reaper de jobs zombie busca por antigüedad del lock.
CREATE INDEX IF NOT EXISTS idx_job_locked_at ON app.job (locked_at);

-- Idempotencia: una misma clave no puede encolarse dos veces (permite múltiples NULL).
CREATE UNIQUE INDEX IF NOT EXISTS uk_job_idempotency_key
    ON app.job (idempotency_key) WHERE idempotency_key IS NOT NULL;

COMMENT ON TABLE  app.job                 IS 'Cola de jobs procesados de forma asíncrona por workers';
COMMENT ON COLUMN app.job.status          IS 'PENDING | RUNNING | RETRYING | COMPLETED | FAILED | CANCELLED';
COMMENT ON COLUMN app.job.priority        IS 'Peso de prioridad: 0=LOW, 1=NORMAL, 2=HIGH (mayor = antes)';
COMMENT ON COLUMN app.job.idempotency_key IS 'Clave de idempotencia opcional para evitar doble encolado';
COMMENT ON COLUMN app.job.next_attempt_at IS 'Instante a partir del cual el job puede reintentarse (backoff)';
COMMENT ON COLUMN app.job.locked_by       IS 'Identificador de la instancia/worker que tomó el job';
