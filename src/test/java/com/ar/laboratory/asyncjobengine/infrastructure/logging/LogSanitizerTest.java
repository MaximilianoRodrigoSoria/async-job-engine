package com.ar.laboratory.asyncjobengine.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.ar.laboratory.asyncjobengine.shared.infrastructure.logging.LogSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests unitarios para {@link LogSanitizer}. */
@DisplayName("LogSanitizer")
class LogSanitizerTest {

    @Test
    @DisplayName("Debe sanitizar password en JSON")
    void shouldSanitizePasswordInJson() {
        String input = "{\"username\":\"john\",\"password\":\"secret123\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"password\":\"****\"");
        assertThat(sanitized).doesNotContain("secret123");
    }

    @Test
    @DisplayName("Debe sanitizar password case-insensitive")
    void shouldSanitizePasswordCaseInsensitive() {
        String input = "{\"PASSWORD\":\"secret123\",\"Password\":\"another\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"PASSWORD\":\"****\"");
        assertThat(sanitized).contains("\"Password\":\"****\"");
        assertThat(sanitized).doesNotContain("secret123").doesNotContain("another");
    }

    @Test
    @DisplayName("Debe sanitizar token en JSON")
    void shouldSanitizeTokenInJson() {
        String input = "{\"token\":\"abc123xyz\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"token\":\"****\"");
        assertThat(sanitized).doesNotContain("abc123xyz");
    }

    @Test
    @DisplayName("Debe sanitizar secret en JSON")
    void shouldSanitizeSecretInJson() {
        String input = "{\"secret\":\"my-secret-key\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"secret\":\"****\"");
        assertThat(sanitized).doesNotContain("my-secret-key");
    }

    @Test
    @DisplayName("Debe sanitizar apiKey en JSON")
    void shouldSanitizeApiKeyInJson() {
        String input = "{\"apiKey\":\"sk_live_abc123\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"apiKey\":\"****\"");
        assertThat(sanitized).doesNotContain("sk_live_abc123");
    }

    @Test
    @DisplayName("Debe sanitizar header Authorization")
    void shouldSanitizeAuthorizationHeader() {
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("Authorization: ****");
        assertThat(sanitized).doesNotContain("Bearer");
        assertThat(sanitized).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test
    @DisplayName("Debe sanitizar DNI argentino en JSON")
    void shouldSanitizeDniInJson() {
        String input = "{\"nombre\":\"Juan\",\"dni\":\"12345678\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"dni\":\"****\"");
        assertThat(sanitized).doesNotContain("12345678");
        assertThat(sanitized).contains("\"nombre\":\"Juan\""); // nombre no se sanitiza
    }

    @Test
    @DisplayName("Debe sanitizar CUIT/CUIL en JSON")
    void shouldSanitizeCuitInJson() {
        String input = "{\"cuit\":\"20-12345678-9\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"cuit\":\"****\"");
        assertThat(sanitized).doesNotContain("20-12345678-9");
    }

    @Test
    @DisplayName("Debe sanitizar CUIT sin guiones")
    void shouldSanitizeCuitWithoutDashes() {
        String input = "{\"cuit\":\"20123456789\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"cuit\":\"****\"");
        assertThat(sanitized).doesNotContain("20123456789");
    }

    @Test
    @DisplayName("Debe sanitizar múltiples campos sensibles")
    void shouldSanitizeMultipleSensitiveFields() {
        String input =
                "{\"username\":\"john\",\"password\":\"pass123\",\"token\":\"tok456\",\"dni\":\"87654321\"}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).contains("\"password\":\"****\"");
        assertThat(sanitized).contains("\"token\":\"****\"");
        assertThat(sanitized).contains("\"dni\":\"****\"");
        assertThat(sanitized).contains("\"username\":\"john\""); // username no se sanitiza
        assertThat(sanitized)
                .doesNotContain("pass123")
                .doesNotContain("tok456")
                .doesNotContain("87654321");
    }

    @Test
    @DisplayName("Debe retornar null si input es null")
    void shouldReturnNullWhenInputIsNull() {
        String sanitized = LogSanitizer.sanitize(null);
        assertThat(sanitized).isNull();
    }

    @Test
    @DisplayName("Debe retornar string vacío si input es vacío")
    void shouldReturnEmptyWhenInputIsBlank() {
        assertThat(LogSanitizer.sanitize("")).isEmpty();
        assertThat(LogSanitizer.sanitize("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("Debe preservar estructura JSON después de sanitizar")
    void shouldPreserveJsonStructureAfterSanitizing() {
        String input = "{\"name\":\"John\",\"password\":\"secret\",\"age\":30}";
        String sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).startsWith("{").endsWith("}");
        assertThat(sanitized).contains("\"name\":\"John\"");
        assertThat(sanitized).contains("\"password\":\"****\"");
        assertThat(sanitized).contains("\"age\":30");
    }

    @Test
    @DisplayName("sanitizeHeader debe enmascarar Authorization completamente")
    void sanitizeHeaderShouldMaskAuthorizationCompletely() {
        String sanitized = LogSanitizer.sanitizeHeader("Authorization", "Bearer abc123");
        assertThat(sanitized).isEqualTo("****");
    }

    @Test
    @DisplayName("sanitizeHeader debe enmascarar X-API-Key completamente")
    void sanitizeHeaderShouldMaskApiKey() {
        String sanitized = LogSanitizer.sanitizeHeader("X-API-Key", "secret-key-12345");
        assertThat(sanitized).isEqualTo("****");
    }

    @Test
    @DisplayName("sanitizeHeader debe enmascarar X-Auth-Token completamente")
    void sanitizeHeaderShouldMaskAuthToken() {
        String sanitized = LogSanitizer.sanitizeHeader("X-Auth-Token", "token-value");
        assertThat(sanitized).isEqualTo("****");
    }

    @Test
    @DisplayName("sanitizeHeader no debe enmascarar headers no sensibles")
    void sanitizeHeaderShouldNotMaskNonSensitiveHeaders() {
        String value = "application/json";
        String sanitized = LogSanitizer.sanitizeHeader("Content-Type", value);
        assertThat(sanitized).isEqualTo(value);
    }

    @Test
    @DisplayName("sanitizeHeader debe ser case-insensitive")
    void sanitizeHeaderShouldBeCaseInsensitive() {
        assertThat(LogSanitizer.sanitizeHeader("authorization", "Bearer abc")).isEqualTo("****");
        assertThat(LogSanitizer.sanitizeHeader("AUTHORIZATION", "Bearer abc")).isEqualTo("****");
        assertThat(LogSanitizer.sanitizeHeader("x-api-key", "key")).isEqualTo("****");
        assertThat(LogSanitizer.sanitizeHeader("X-API-KEY", "key")).isEqualTo("****");
    }

    @Test
    @DisplayName("sanitizeHeader debe manejar valores null o vacíos")
    void sanitizeHeaderShouldHandleNullOrEmptyValues() {
        assertThat(LogSanitizer.sanitizeHeader("Authorization", null)).isNull();
        assertThat(LogSanitizer.sanitizeHeader("Authorization", "")).isEmpty();
        assertThat(LogSanitizer.sanitizeHeader("Authorization", "   ")).isEqualTo("   ");
    }
}
