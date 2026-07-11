package com.ar.laboratory.asyncjobengine.shared.infrastructure.logging;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilidad centralizada para sanitizar datos sensibles en logs.
 *
 * <p>Aplica reglas de enmascaramiento sobre strings antes de ser logueados, evitando que
 * información sensible como passwords, tokens, DNIs, etc. queden expuestos en archivos de log.
 *
 * <p>Enfoque de defensa en profundidad: aunque se configure logback-spring.xml, esta capa actúa
 * como primera línea de defensa directamente en el código.
 */
public final class LogSanitizer {

    private record Rule(Pattern pattern, String replacement) {}

    /**
     * Reglas de sanitización aplicadas en orden.
     *
     * <p>Cada regla define un patrón regex y su reemplazo. Se recomienda ajustar según el dominio
     * específico de la aplicación.
     */
    private static final List<Rule> RULES =
            List.of(
                    // JSON: "password":"valor" -> "password":"****"
                    new Rule(
                            Pattern.compile(
                                    "(\"password\"\\s*:\\s*\")([^\"]+)(\")",
                                    Pattern.CASE_INSENSITIVE),
                            "$1****$3"),

                    // JSON: "secret":"valor" -> "secret":"****"
                    new Rule(
                            Pattern.compile(
                                    "(\"secret\"\\s*:\\s*\")([^\"]+)(\")",
                                    Pattern.CASE_INSENSITIVE),
                            "$1****$3"),

                    // JSON: "token":"valor" -> "token":"****"
                    new Rule(
                            Pattern.compile(
                                    "(\"token\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE),
                            "$1****$3"),

                    // JSON: "apiKey":"valor" -> "apiKey":"****"
                    new Rule(
                            Pattern.compile(
                                    "(\"apiKey\"\\s*:\\s*\")([^\"]+)(\")",
                                    Pattern.CASE_INSENSITIVE),
                            "$1****$3"),

                    // Header Authorization: "Authorization: Bearer xxx" -> "Authorization: ****"
                    new Rule(
                            Pattern.compile(
                                    "(Authorization\\s*:\\s*)([^\\r\\n]+)",
                                    Pattern.CASE_INSENSITIVE),
                            "$1****"),

                    // DNI argentino (8 dígitos exactos en contexto de JSON)
                    new Rule(
                            Pattern.compile(
                                    "(\"dni\"\\s*:\\s*\")([0-9]{8})(\")", Pattern.CASE_INSENSITIVE),
                            "$1****$3"),

                    // CUIT/CUIL (11 dígitos con o sin guiones)
                    new Rule(
                            Pattern.compile(
                                    "(\"cuit\"\\s*:\\s*\")([0-9]{2}-?[0-9]{8}-?[0-9])(\")",
                                    Pattern.CASE_INSENSITIVE),
                            "$1****$3"),

                    // Tarjetas de crédito (13-19 dígitos) - enmascarar todo
                    new Rule(Pattern.compile("\\b[0-9]{13,19}\\b"), "****"));

    private LogSanitizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sanitiza un string aplicando todas las reglas definidas.
     *
     * @param input el string original que puede contener datos sensibles
     * @return el string con datos sensibles enmascarados, o el original si es null/vacío
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String output = input;
        for (Rule rule : RULES) {
            output = rule.pattern().matcher(output).replaceAll(rule.replacement());
        }
        return output;
    }

    /**
     * Sanitiza un string específicamente para headers HTTP.
     *
     * <p>Aplica reglas más estrictas para headers que típicamente contienen tokens.
     *
     * @param headerName nombre del header
     * @param headerValue valor del header
     * @return valor sanitizado si es un header sensible, original en caso contrario
     */
    public static String sanitizeHeader(String headerName, String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return headerValue;
        }

        // Headers que siempre se enmascaran completamente
        if (headerName.equalsIgnoreCase("Authorization")
                || headerName.equalsIgnoreCase("X-API-Key")
                || headerName.equalsIgnoreCase("X-Auth-Token")) {
            return "****";
        }

        return headerValue;
    }
}
