package com.ar.laboratory.asyncjobengine.job.application.usecase;

import com.ar.laboratory.asyncjobengine.job.application.inbound.command.CancelJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobNotFoundException;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cancela un job en estado PENDING o RETRYING. Si el job está en otro estado, la máquina de estados
 * del dominio lanza {@code InvalidJobTransitionException}. POJO puro sin framework.
 */
@Slf4j
@RequiredArgsConstructor
public class CancelJobUseCase implements CancelJobCommand {

    private final JobRepositoryPort repository;

    @Override
    public Job execute(UUID id) {
        Job job = repository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        job.cancel(Instant.now());
        Job saved = repository.save(job);
        log.info("Job cancelado id={}", saved.getId());
        return saved;
    }
}
