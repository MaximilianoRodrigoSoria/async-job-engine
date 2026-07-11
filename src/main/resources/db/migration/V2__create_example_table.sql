-- Crear tabla example dentro del schema app
CREATE TABLE IF NOT EXISTS app.example (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    dni  VARCHAR(20)  NOT NULL UNIQUE
);

-- Índice en DNI para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_example_dni ON app.example(dni);

-- Comentarios
COMMENT ON TABLE app.example IS 'Tabla de ejemplos con nombre y DNI';
COMMENT ON COLUMN app.example.id IS 'Identificador único autoincremental';
COMMENT ON COLUMN app.example.name IS 'Nombre del ejemplo (2-120 caracteres)';
COMMENT ON COLUMN app.example.dni IS 'DNI único alfanumérico (máx 20 caracteres)';
