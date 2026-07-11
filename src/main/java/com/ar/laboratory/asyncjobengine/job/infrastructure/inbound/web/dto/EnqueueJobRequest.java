package com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto;

import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de encolado de un job. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnqueueJobRequest {

    @NotBlank(message = "jobType es obligatorio")
    @Size(max = 120, message = "jobType no puede superar 120 caracteres")
    private String jobType;

    /** Datos de entrada del job (JSON arbitrario). Opcional. */
    private JsonNode payload;

    /** Prioridad de procesamiento. Opcional; por defecto NORMAL. */
    private Priority priority;

    /** Máximo de intentos totales. Opcional; por defecto 3. */
    @Min(value = 1, message = "maxAttempts debe ser ≥ 1")
    private Integer maxAttempts;

    /** Ejecutar no antes de este instante (job diferido). Opcional. */
    private Instant runAt;
}
