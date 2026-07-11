package com.ar.laboratory.asyncjobengine.example.application.query;

/**
 * Parámetros de filtrado dinámico para consultas de Example.
 *
 * <p>Pertenece a la capa de <b>application</b> porque representa una intención de consulta del caso
 * de uso, independiente de la tecnología de persistencia subyacente.
 *
 * <p>Todos los campos son opcionales. Cuando un campo es {@code null} o vacío, ese criterio no se
 * aplica en la consulta.
 *
 * <ul>
 *   <li>{@code name} — filtro parcial (LIKE %name%) sobre el campo {@code name}
 *   <li>{@code dni} — filtro parcial (LIKE %dni%) sobre el campo {@code dni}
 *   <li>{@code search} — búsqueda libre: aplica LIKE %search% sobre {@code name} OR {@code dni}
 * </ul>
 */
public record ExampleFilter(String name, String dni, String search) {

    /** Retorna {@code true} si todos los campos de filtrado son nulos o vacíos. */
    public boolean isEmpty() {
        return isBlank(name) && isBlank(dni) && isBlank(search);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
