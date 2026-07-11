package com.ar.laboratory.asyncjobengine.job.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ar.laboratory.asyncjobengine.job.application.inbound.command.ProcessAvailableJobsCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests de integración del {@link com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web
 * .controller.JobController} con contexto completo y PostgreSQL real. El worker en background está
 * desactivado (perfil {@code test}); el procesamiento se dispara manualmente vía {@link
 * ProcessAvailableJobsCommand}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("JobController - Integration Tests")
class JobControllerIT {

    private static final String BASE_PATH = "/async-job-engine/api/v1/jobs";

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort private int port;
    @Autowired private ProcessAvailableJobsCommand processCommand;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private JsonNode enqueue(Map<String, Object> body, String idempotencyKey) throws Exception {
        WebTestClient.RequestBodySpec spec =
                client.post().uri(BASE_PATH).contentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }
        byte[] bytes =
                spec.bodyValue(body)
                        .exchange()
                        .expectStatus()
                        .isAccepted()
                        .expectBody()
                        .returnResult()
                        .getResponseBodyContent();
        return objectMapper.readTree(bytes);
    }

    @Test
    @DisplayName("POST /jobs → 202 y estado PENDING")
    void enqueueReturnsAccepted() throws Exception {
        JsonNode node = enqueue(Map.of("jobType", "echo", "payload", Map.of("msg", "hola")), null);
        assertThat(node.get("id").asText()).isNotBlank();
        assertThat(node.get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("POST /jobs → 400 cuando falta jobType")
    void enqueueValidationError() {
        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("payload", Map.of("x", 1)))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.validationErrors")
                .isNotEmpty();
    }

    @Test
    @DisplayName("POST /jobs → 400 cuando el jobType no tiene handler")
    void enqueueUnsupportedType() {
        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("jobType", "tipo-inexistente"))
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    @DisplayName("GET /jobs/{id} → 404 cuando no existe")
    void getMissingReturns404() {
        client.get()
                .uri(BASE_PATH + "/" + UUID.randomUUID())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(404);
    }

    @Test
    @DisplayName("flujo e2e: encolar echo → procesar → COMPLETED con resultado")
    void endToEndProcessing() throws Exception {
        JsonNode created =
                enqueue(Map.of("jobType", "echo", "payload", Map.of("ping", "pong")), null);
        UUID id = UUID.fromString(created.get("id").asText());

        // Dispara el procesamiento (worker desactivado en tests).
        int processed = processCommand.execute();
        assertThat(processed).isGreaterThanOrEqualTo(1);

        client.get()
                .uri(BASE_PATH + "/" + id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("COMPLETED")
                .jsonPath("$.result.ping")
                .isEqualTo("pong");
    }

    @Test
    @DisplayName("POST /jobs/{id}/cancel → 200 y estado CANCELLED")
    void cancelPendingJob() throws Exception {
        JsonNode created = enqueue(Map.of("jobType", "echo"), null);
        UUID id = UUID.fromString(created.get("id").asText());

        client.post()
                .uri(BASE_PATH + "/" + id + "/cancel")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Idempotency-Key: dos encolados con la misma clave devuelven el mismo job")
    void idempotentEnqueue() throws Exception {
        String key = "idem-" + UUID.randomUUID();
        JsonNode first = enqueue(Map.of("jobType", "echo"), key);
        JsonNode second = enqueue(Map.of("jobType", "echo"), key);
        assertThat(second.get("id").asText()).isEqualTo(first.get("id").asText());
    }

    @Test
    @DisplayName("GET /jobs → 200 con estructura de página")
    void listJobsPaged() throws Exception {
        enqueue(Map.of("jobType", "echo"), null);
        client.get()
                .uri(BASE_PATH + "?page=0&size=10")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content")
                .isArray()
                .jsonPath("$.page")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("GET /jobs/dead-letter → 200 lista (array)")
    void deadLetterListing() {
        client.get()
                .uri(BASE_PATH + "/dead-letter")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content")
                .isArray();
    }
}
