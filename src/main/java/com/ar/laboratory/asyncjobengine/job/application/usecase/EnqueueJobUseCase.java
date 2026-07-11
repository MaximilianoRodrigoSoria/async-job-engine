package com.ar.laboratory.asyncjobengine.job.application.usecase;

import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandlerRegistry;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.EnqueueJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobTypeNotSupportedException;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Encola un job validando que su tipo tenga handler y respetando la idempotencia. POJO puro sin
 * framework.
 */
@Slf4j
@RequiredArgsConstructor
public class EnqueueJobUseCase implements EnqueueJobCommand {

    private final JobRepositoryPort repository;
    private final JobHandlerRegistry registry;

    @Override
    public Job execute(Job job) {
        if (!registry.supports(job.getJobType())) {
            log.warn("jobType no soportado: {}", job.getJobType());
            throw new JobTypeNotSupportedException(job.getJobType());
        }

        // Idempotencia: si la clave ya se usó, devolvemos el job existente (no duplicamos).
        String key = job.getIdempotencyKey();
        if (key != null && !key.isBlank()) {
            var existing = repository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                log.info("Idempotency-Key '{}' ya existe → devolviendo job {}", key,
                        existing.get().getId());
                return existing.get();
            }
        }

        Job saved = repository.save(job);
        log.info("Job encolado id={} type={} priority={}", saved.getId(), saved.getJobType(),
                saved.getPriority());
        return saved;
    }
}
