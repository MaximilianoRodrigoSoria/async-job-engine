package com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.api;

import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.CreateExampleRequest;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.ExampleResponse;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.web.api.StandardApiResponses;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrato OpenAPI para el API de Examples.
 *
 * <p>Define la documentación Swagger para todos los endpoints de Example.
 */
@Tag(name = "Examples", description = "API para gestión de Examples")
public interface ExampleApi extends StandardApiResponses {

    @Operation(
            summary = "Crear un nuevo Example",
            description = "Crea un nuevo Example con los datos proporcionados")
    @ApiResponse(
            responseCode = "201",
            description = "Example creado exitosamente",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExampleResponse.class)))
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "409", description = "Ya existe un Example con ese DNI")
    ResponseEntity<ExampleResponse> create(@Valid @RequestBody CreateExampleRequest request);

    @Operation(
            summary = "Listar Examples con paginación y filtros",
            description =
                    "Retorna una página de Examples. Permite filtrar por `name`, `dni` o una"
                            + " búsqueda libre con `search` (aplica sobre name y dni). "
                            + "Ordenamiento con `sort=campo,asc|desc`.")
    @ApiResponse(
            responseCode = "200",
            description = "Página de Examples",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class)))
    ResponseEntity<PageResponse<ExampleResponse>> listAll(
            @Parameter(description = "Número de página (0-based)", example = "0")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Tamaño de página", example = "20")
                    @RequestParam(defaultValue = "20")
                    int size,
            @Parameter(description = "Filtro exacto parcial sobre el campo name")
                    @RequestParam(required = false)
                    String name,
            @Parameter(description = "Filtro exacto parcial sobre el campo dni")
                    @RequestParam(required = false)
                    String dni,
            @Parameter(description = "Búsqueda libre: filtra por name OR dni con LIKE %search%")
                    @RequestParam(required = false)
                    String search);

    @Operation(
            summary = "Buscar Example por DNI",
            description = "Busca un Example específico por su DNI")
    @ApiResponse(
            responseCode = "200",
            description = "Example encontrado",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ExampleResponse.class)))
    @ApiResponse(responseCode = "404", description = "Example no encontrado")
    ResponseEntity<ExampleResponse> findByDni(
            @Parameter(description = "DNI del Example a buscar", required = true) @PathVariable
                    String dni);
}
