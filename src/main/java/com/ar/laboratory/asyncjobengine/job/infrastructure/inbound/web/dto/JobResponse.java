package com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto;

import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta de un job. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobResponse {

    private UUID id;
    private String jobType;
    private JobStatus status;
    private Priority priority;
    private int attempts;
    private int maxAttempts;
    private JsonNode payload;
    private JsonNode result;
    private String idempotencyKey;
    private Instant scheduledAt;
    private Instant nextAttemptAt;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
}
