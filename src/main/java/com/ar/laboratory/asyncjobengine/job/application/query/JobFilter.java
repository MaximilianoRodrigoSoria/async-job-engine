package com.ar.laboratory.asyncjobengine.job.application.query;

import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;

/**
 * Criterios de filtrado para el listado de jobs. Todos los campos son opcionales; cuando un campo
 * es {@code null}, ese criterio no se aplica.
 */
public record JobFilter(JobStatus status, String jobType, Priority priority) {

    public boolean isEmpty() {
        return status == null && (jobType == null || jobType.isBlank()) && priority == null;
    }
}
