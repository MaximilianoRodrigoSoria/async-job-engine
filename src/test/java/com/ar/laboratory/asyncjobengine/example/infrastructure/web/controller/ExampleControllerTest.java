package com.ar.laboratory.asyncjobengine.example.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ar.laboratory.asyncjobengine.example.application.inbound.command.CreateExampleCommand;
import com.ar.laboratory.asyncjobengine.example.application.inbound.command.FindExampleByDniCommand;
import com.ar.laboratory.asyncjobengine.example.application.inbound.command.ListExamplesCommand;
import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleAlreadyExistsException;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleNotFoundException;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.controller.ExampleController;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.CreateExampleRequest;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.ExampleResponse;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.mapper.ExampleDtoMapper;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.config.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests unitarios para ExampleController.
 *
 * <p>Usa {@code MockMvcBuilders.standaloneSetup()} para aislar el controller sin levantar el
 * contexto de Spring Boot. No depende de autoconfigure ni de Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExampleController — Tests unitarios")
class ExampleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private CreateExampleCommand createExampleCommand;
    @Mock private ListExamplesCommand listExamplesCommand;
    @Mock private FindExampleByDniCommand findExampleByDniCommand;
    @Mock private ExampleDtoMapper dtoMapper;

    @InjectMocks private ExampleController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    // =========================================================================
    // POST /api/v1/examples
    // =========================================================================

    @Test
    @DisplayName("POST /examples — debe crear y retornar 201 con el recurso creado")
    void shouldCreateExampleAndReturn201() throws Exception {
        // Given
        CreateExampleRequest request =
                CreateExampleRequest.builder().name("John Doe").dni("12345678").build();

        Example domain = Example.builder().id(1L).name("John Doe").dni("12345678").build();
        ExampleResponse response =
                ExampleResponse.builder().id(1L).name("John Doe").dni("12345678").build();

        when(dtoMapper.toDomain(any(CreateExampleRequest.class))).thenReturn(domain);
        when(createExampleCommand.execute(any(Example.class))).thenReturn(domain);
        when(dtoMapper.toResponse(domain)).thenReturn(response);

        // When / Then
        mockMvc.perform(
                        post("/api/v1/examples")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    @DisplayName("POST /examples — debe retornar 409 si el DNI ya existe")
    void shouldReturn409WhenDniAlreadyExists() throws Exception {
        // Given
        CreateExampleRequest request =
                CreateExampleRequest.builder().name("John Doe").dni("12345678").build();

        Example domain = Example.builder().name("John Doe").dni("12345678").build();

        when(dtoMapper.toDomain(any(CreateExampleRequest.class))).thenReturn(domain);
        when(createExampleCommand.execute(any(Example.class)))
                .thenThrow(new ExampleAlreadyExistsException("12345678"));

        // When / Then
        mockMvc.perform(
                        post("/api/v1/examples")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /examples — debe retornar 400 si el body es inválido")
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        // Given — nombre vacío viola @NotBlank
        CreateExampleRequest request =
                CreateExampleRequest.builder().name("").dni("12345678").build();

        // When / Then
        mockMvc.perform(
                        post("/api/v1/examples")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /api/v1/examples
    // =========================================================================

    @Test
    @DisplayName("GET /examples — debe retornar página con los resultados")
    void shouldListExamplesPagedSuccessfully() throws Exception {
        // Given
        Example domain = Example.builder().id(1L).name("John Doe").dni("12345678").build();
        ExampleResponse response =
                ExampleResponse.builder().id(1L).name("John Doe").dni("12345678").build();

        Page<Example> page = new PageImpl<>(List.of(domain), PageRequest.of(0, 20), 1);

        when(listExamplesCommand.execute(any(ExampleFilter.class), any(Pageable.class)))
                .thenReturn(page);
        when(dtoMapper.toResponse(domain)).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/examples").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("GET /examples — debe retornar página vacía cuando no hay resultados")
    void shouldReturnEmptyPageWhenNoResults() throws Exception {
        // Given
        Page<Example> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(listExamplesCommand.execute(any(ExampleFilter.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/examples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /examples?search=John — debe pasar el filtro al comando")
    void shouldPassSearchFilterToCommand() throws Exception {
        // Given
        Example domain = Example.builder().id(1L).name("John Doe").dni("12345678").build();
        ExampleResponse response =
                ExampleResponse.builder().id(1L).name("John Doe").dni("12345678").build();

        Page<Example> page = new PageImpl<>(List.of(domain), PageRequest.of(0, 20), 1);

        when(listExamplesCommand.execute(any(ExampleFilter.class), any(Pageable.class)))
                .thenReturn(page);
        when(dtoMapper.toResponse(domain)).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/examples").param("search", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // =========================================================================
    // GET /api/v1/examples/dni/{dni}
    // =========================================================================

    @Test
    @DisplayName("GET /examples/dni/{dni} — debe retornar el Example encontrado")
    void shouldFindExampleByDni() throws Exception {
        // Given
        Example domain = Example.builder().id(1L).name("John Doe").dni("12345678").build();
        ExampleResponse response =
                ExampleResponse.builder().id(1L).name("John Doe").dni("12345678").build();

        when(findExampleByDniCommand.execute("12345678")).thenReturn(domain);
        when(dtoMapper.toResponse(domain)).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/examples/dni/12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    @DisplayName("GET /examples/dni/{dni} — debe retornar 404 si el DNI no existe")
    void shouldReturn404WhenDniNotFound() throws Exception {
        // Given
        when(findExampleByDniCommand.execute("99999999"))
                .thenThrow(new ExampleNotFoundException("Example no encontrado con DNI: 99999999"));

        // When / Then
        mockMvc.perform(get("/api/v1/examples/dni/99999999")).andExpect(status().isNotFound());
    }

    // =========================================================================
    // Rate Limiting
    // =========================================================================

    @Test
    @DisplayName("POST /examples — debe retornar 429 cuando se supera el rate limit")
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        // Given
        CreateExampleRequest request =
                CreateExampleRequest.builder().name("John Doe").dni("12345678").build();

        // doThrow(Class) evita instanciar RequestNotPermitted directamente.
        // createRequestNotPermitted(null) lanza NPE internamente porque llama
        // rateLimiter.getName(),
        // dejando el stubbing de Mockito incompleto (UnfinishedStubbingException).
        doThrow(RequestNotPermitted.class)
                .when(dtoMapper)
                .toDomain(any(CreateExampleRequest.class));

        // When / Then
        mockMvc.perform(
                        post("/api/v1/examples")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }
}
