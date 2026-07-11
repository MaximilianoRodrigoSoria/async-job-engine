package com.ar.laboratory.asyncjobengine.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests de integración del ExampleController.
 *
 * <p>Levanta el contexto completo de Spring Boot con:
 *
 * <ul>
 *   <li>PostgreSQL real via Testcontainers ({@link ServiceConnection} inyecta las propiedades de
 *       datasource automáticamente)
 *   <li>Flyway ejecuta las migraciones sobre el contenedor
 *   <li>Redis y Spring Security deshabilitados (perfil {@code test})
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.jpa.properties.hibernate.dialect=",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.datasource.driver-class-name=org.postgresql.Driver"
        })
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("ExampleController - Integration Tests")
class ExampleControllerIT {

    private static final String BASE_PATH = "/async-job-engine/api/v1/examples";

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort private int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    // =========================================================================
    // POST /examples
    // =========================================================================

    @Test
    @DisplayName("POST /examples → 201 y devuelve el recurso creado")
    void shouldCreateExampleSuccessfully() {
        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Juan Perez", "dni", "CREATE001"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.id")
                .isNotEmpty()
                .jsonPath("$.name")
                .isEqualTo("Juan Perez")
                .jsonPath("$.dni")
                .isEqualTo("CREATE001");
    }

    @Test
    @DisplayName("POST /examples → 409 cuando el DNI ya existe")
    void shouldReturnConflictWhenDniAlreadyExists() {
        var request = Map.of("name", "Carlos Ruiz", "dni", "CONFLICT01");

        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated();

        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("POST /examples → 400 cuando el body tiene campos inválidos")
    void shouldReturnBadRequestWhenBodyIsInvalid() {
        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "", "dni", ""))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.validationErrors")
                .isNotEmpty();
    }

    // =========================================================================
    // GET /examples — paginado con filtros
    // =========================================================================

    @Test
    @DisplayName("GET /examples → 200 con estructura de página")
    void shouldListExamplesPaged() {
        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Lista Test", "dni", "LIST00001"))
                .exchange()
                .expectStatus()
                .isCreated();

        client.get()
                .uri(BASE_PATH + "?page=0&size=10")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content")
                .isArray()
                .jsonPath("$.totalElements")
                .isNumber()
                .jsonPath("$.page")
                .isEqualTo(0)
                .jsonPath("$.size")
                .isEqualTo(10);
    }

    @Test
    @DisplayName("GET /examples?search=SearchTest → filtra por nombre parcial")
    void shouldFilterBySearch() {
        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "SearchTest Único", "dni", "SRCH00001"))
                .exchange()
                .expectStatus()
                .isCreated();

        client.get()
                .uri(BASE_PATH + "?search=SearchTest")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content")
                .isArray()
                .jsonPath("$.totalElements")
                .value(total -> assertThat((Integer) total).isGreaterThanOrEqualTo(1));
    }

    // =========================================================================
    // GET /examples/dni/{dni}
    // =========================================================================

    @Test
    @DisplayName("GET /examples/dni/{dni} → 200 cuando existe")
    void shouldFindExampleByDni() {
        client.post()
                .uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Maria Lopez", "dni", "FIND00001"))
                .exchange()
                .expectStatus()
                .isCreated();

        client.get()
                .uri(BASE_PATH + "/dni/FIND00001")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.dni")
                .isEqualTo("FIND00001")
                .jsonPath("$.name")
                .isEqualTo("Maria Lopez");
    }

    @Test
    @DisplayName("GET /examples/dni/{dni} → 404 cuando no existe")
    void shouldReturnNotFoundWhenDniDoesNotExist() {
        client.get()
                .uri(BASE_PATH + "/dni/DNI_NO_EXISTE_XYZ")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(404);
    }
}
