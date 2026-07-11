package com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.mapper;

import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto.EnqueueJobRequest;
import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto.JobResponse;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.util.JsonHandler;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Conversión entre DTOs web y el dominio {@link Job}. Serializa/deserializa el {@code payload} y el
 * {@code result} (JSON ↔ texto) usando {@link JsonHandler}.
 */
@Component
@RequiredArgsConstructor
public class JobDtoMapper {

    private final JsonHandler jsonHandler;

    /** Construye un job PENDING a partir del request de encolado y la clave de idempotencia. */
    public Job toDomain(EnqueueJobRequest request, String idempotencyKey) {
        String payload = request.getPayload() == null ? "{}" : jsonHandler.toJson(request.getPayload());
        Priority priority = request.getPriority() == null ? Priority.NORMAL : request.getPriority();
        int maxAttempts = request.getMaxAttempts() == null ? 3 : request.getMaxAttempts();
        return Job.createPending(
                request.getJobType(),
                payload,
                priority,
                maxAttempts,
                (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey,
                request.getRunAt(),
                Instant.now());
    }

    public JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .attempts(job.getAttempts())
                .maxAttempts(job.getMaxAttempts())
                .payload(toNode(job.getPayload()))
                .result(toNode(job.getResult()))
                .idempotencyKey(job.getIdempotencyKey())
                .scheduledAt(job.getScheduledAt())
                .nextAttemptAt(job.getNextAttemptAt())
                .lastError(job.getLastError())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private com.fasterxml.jackson.databind.JsonNode toNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return jsonHandler.toJsonNode(json);
    }
}
