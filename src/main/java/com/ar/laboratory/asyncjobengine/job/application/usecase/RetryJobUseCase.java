package com.ar.laboratory.asyncjobengine.job.application.usecase;

import com.ar.laboratory.asyncjobengine.job.application.inbound.command.RetryJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobNotFoundException;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reencola manualmente un job FAILED o CANCELLED (reproceso desde la DLQ), reseteando los intentos.
 * POJO puro sin framework.
 */
@Slf4j
@RequiredArgsConstructor
public class RetryJobUseCase implements RetryJobCommand {

    private final JobRepositoryPort repository;

    @Override
    public Job execute(UUID id) {
        Job job = repository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        job.requeue(Instant.now());
        Job saved = repository.save(job);
        log.info("Job reencolado manualmente id={}", saved.getId());
        return saved;
    }
}
