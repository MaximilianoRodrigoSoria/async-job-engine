package com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.mapper;

import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.entity.JobEntity;
import org.springframework.stereotype.Component;

/**
 * Conversión explícita entre {@link Job} (dominio) y {@link JobEntity} (JPA).
 *
 * <p>Se implementa a mano (en vez de MapStruct) por las conversiones no triviales: {@code status}
 * ↔ texto y {@code priority} ↔ peso numérico.
 */
@Component
public class JobEntityMapper {

    public Job toDomain(JobEntity e) {
        if (e == null) {
            return null;
        }
        return Job.builder()
                .id(e.getId())
                .jobType(e.getJobType())
                .payload(e.getPayload())
                .status(e.getStatus() == null ? null : JobStatus.valueOf(e.getStatus()))
                .priority(Priority.fromWeight(e.getPriority()))
                .idempotencyKey(e.getIdempotencyKey())
                .attempts(e.getAttempts())
                .maxAttempts(e.getMaxAttempts())
                .scheduledAt(e.getScheduledAt())
                .nextAttemptAt(e.getNextAttemptAt())
                .lockedBy(e.getLockedBy())
                .lockedAt(e.getLockedAt())
                .lastError(e.getLastError())
                .result(e.getResult())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public JobEntity toEntity(Job j) {
        if (j == null) {
            return null;
        }
        return JobEntity.builder()
                .id(j.getId())
                .jobType(j.getJobType())
                .payload(j.getPayload())
                .status(j.getStatus() == null ? null : j.getStatus().name())
                .priority(j.getPriority() == null ? Priority.NORMAL.weight() : j.getPriority().weight())
                .idempotencyKey(j.getIdempotencyKey())
                .attempts(j.getAttempts())
                .maxAttempts(j.getMaxAttempts())
                .scheduledAt(j.getScheduledAt())
                .nextAttemptAt(j.getNextAttemptAt())
                .lockedBy(j.getLockedBy())
                .lockedAt(j.getLockedAt())
                .lastError(j.getLastError())
                .result(j.getResult())
                .createdAt(j.getCreatedAt())
                .updatedAt(j.getUpdatedAt())
                .build();
    }
}
