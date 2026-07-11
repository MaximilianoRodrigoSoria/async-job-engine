-- Índice para búsquedas LIKE %term% sobre la columna name.
--
-- Se usa pg_trgm (trigram) para soportar búsquedas parciales eficientes.
-- Sin este índice, `WHERE name ILIKE '%keyword%'` hace full-table scan.
--
-- Requiere la extensión pg_trgm disponible por defecto en PostgreSQL 9.1+.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_example_name_trgm
    ON app.example USING gin (name gin_trgm_ops);

COMMENT ON INDEX app.idx_example_name_trgm
    IS 'Índice GIN trigram para búsquedas LIKE/ILIKE sobre name';
