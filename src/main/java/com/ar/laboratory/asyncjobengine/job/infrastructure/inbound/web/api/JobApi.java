package com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.api;

import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto.EnqueueJobRequest;
import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto.JobResponse;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.web.api.StandardApiResponses;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/** Contrato OpenAPI del API de jobs. */
@Tag(name = "Jobs", description = "Motor de procesamiento asíncrono: encolar y gestionar jobs")
public interface JobApi extends StandardApiResponses {

    @Operation(
            summary = "Encolar un job",
            description =
                    "Encola un job para procesamiento asíncrono y responde 202 con el id de"
                            + " seguimiento. La cabecera opcional Idempotency-Key evita el doble"
                            + " encolado ante reintentos del cliente.")
    @ApiResponse(
            responseCode = "202",
            description = "Job aceptado y encolado",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JobResponse.class)))
    @ApiResponse(responseCode = "400", description = "Request inválido o jobType no soportado")
    ResponseEntity<JobResponse> enqueue(
            @Valid @RequestBody EnqueueJobRequest request,
            @Parameter(description = "Clave de idempotencia opcional")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey);

    @Operation(summary = "Consultar un job por id")
    @ApiResponse(
            responseCode = "200",
            description = "Job encontrado",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JobResponse.class)))
    @ApiResponse(responseCode = "404", description = "Job no encontrado")
    ResponseEntity<JobResponse> getById(
            @Parameter(description = "Id del job", required = true) @PathVariable UUID id);

    @Operation(summary = "Listar jobs con filtros y paginación")
    @ApiResponse(responseCode = "200", description = "Página de jobs")
    ResponseEntity<PageResponse<JobResponse>> list(
            @Parameter(description = "Número de página (0-based)", example = "0")
                    @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Tamaño de página", example = "20")
                    @RequestParam(defaultValue = "20")
                    int size,
            @Parameter(description = "Filtrar por estado (PENDING, RUNNING, ...)")
                    @RequestParam(required = false)
                    String status,
            @Parameter(description = "Filtrar por jobType exacto") @RequestParam(required = false)
                    String jobType,
            @Parameter(description = "Filtrar por prioridad (LOW, NORMAL, HIGH)")
                    @RequestParam(required = false)
                    String priority);

    @Operation(summary = "Cancelar un job pendiente o en espera de reintento")
    @ApiResponse(responseCode = "200", description = "Job cancelado")
    @ApiResponse(responseCode = "404", description = "Job no encontrado")
    @ApiResponse(responseCode = "409", description = "El job no admite cancelación en su estado actual")
    ResponseEntity<JobResponse> cancel(
            @Parameter(description = "Id del job", required = true) @PathVariable UUID id);

    @Operation(summary = "Reintentar manualmente un job fallido o cancelado")
    @ApiResponse(responseCode = "200", description = "Job reencolado")
    @ApiResponse(responseCode = "404", description = "Job no encontrado")
    @ApiResponse(responseCode = "409", description = "El job no admite reintento en su estado actual")
    ResponseEntity<JobResponse> retry(
            @Parameter(description = "Id del job", required = true) @PathVariable UUID id);

    @Operation(
            summary = "Listar la Dead-Letter Queue",
            description = "Jobs que agotaron sus reintentos (estado FAILED).")
    @ApiResponse(responseCode = "200", description = "Página de jobs en DLQ")
    ResponseEntity<PageResponse<JobResponse>> deadLetter(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "Reprocesar un job desde la Dead-Letter Queue")
    @ApiResponse(responseCode = "200", description = "Job reencolado")
    @ApiResponse(responseCode = "404", description = "Job no encontrado")
    ResponseEntity<JobResponse> reprocess(
            @Parameter(description = "Id del job", required = true) @PathVariable UUID id);
}
