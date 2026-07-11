-- Insertar datos de ejemplo (seed data)
-- Los datos solo se insertan si no existen (idempotente)

INSERT INTO app.example (name, dni)
SELECT 'Juan Perez', '12345678'
WHERE NOT EXISTS (
    SELECT 1 FROM app.example WHERE dni = '12345678'
);

INSERT INTO app.example (name, dni)
SELECT 'Maria Gomez', '87654321'
WHERE NOT EXISTS (
    SELECT 1 FROM app.example WHERE dni = '87654321'
);

INSERT INTO app.example (name, dni)
SELECT 'Carlos Rodriguez', '11223344'
WHERE NOT EXISTS (
    SELECT 1 FROM app.example WHERE dni = '11223344'
);
