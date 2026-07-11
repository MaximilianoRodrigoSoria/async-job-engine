package com.ar.laboratory.asyncjobengine.job.application.handler;

import com.ar.laboratory.asyncjobengine.job.domain.exception.JobTypeNotSupportedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registro de {@link JobHandler} indexados por {@code jobType}. Se construye con la lista de
 * handlers disponibles (inyectada por el contenedor) y resuelve el handler adecuado en tiempo de
 * ejecución.
 */
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlersByType = new HashMap<>();

    public JobHandlerRegistry(List<JobHandler> handlers) {
        if (handlers != null) {
            for (JobHandler handler : handlers) {
                handlersByType.put(handler.jobType(), handler);
            }
        }
    }

    /** {@code true} si existe un handler registrado para el {@code jobType}. */
    public boolean supports(String jobType) {
        return handlersByType.containsKey(jobType);
    }

    /**
     * Devuelve el handler para el {@code jobType}.
     *
     * @throws JobTypeNotSupportedException si no hay handler registrado.
     */
    public JobHandler handlerFor(String jobType) {
        JobHandler handler = handlersByType.get(jobType);
        if (handler == null) {
            throw new JobTypeNotSupportedException(jobType);
        }
        return handler;
    }

    /** Conjunto de jobTypes soportados (para diagnóstico / documentación). */
    public Set<String> supportedTypes() {
        return handlersByType.keySet();
    }
}
