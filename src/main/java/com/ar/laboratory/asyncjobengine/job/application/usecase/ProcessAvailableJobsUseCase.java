package com.ar.laboratory.asyncjobengine.job.application.usecase;

import com.ar.laboratory.asyncjobengine.job.application.backoff.BackoffPolicy;
import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandler;
import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandlerRegistry;
import com.ar.laboratory.asyncjobengine.job.application.handler.NonRecoverableJobException;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.ProcessAvailableJobsCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobTypeNotSupportedException;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquesta una pasada del worker: toma un lote de jobs (claim atómico vía el puerto), ejecuta el
 * handler correspondiente a cada uno y aplica el resultado sobre la máquina de estados
 * (COMPLETED / RETRYING / FAILED).
 *
 * <p>La ejecución del handler ocurre <b>fuera</b> de la transacción de claim: el puerto marca los
 * jobs como RUNNING en su propia transacción y luego cada actualización de resultado se persiste
 * de forma independiente. Así una ejecución larga no mantiene abierta la transacción de la cola.
 *
 * <p>POJO puro sin framework: el {@code workerId} y el {@code batchSize} se inyectan al construirlo.
 */
@Slf4j
public class ProcessAvailableJobsUseCase implements ProcessAvailableJobsCommand {

    private final JobRepositoryPort repository;
    private final JobHandlerRegistry registry;
    private final BackoffPolicy backoffPolicy;
    private final String workerId;
    private final int batchSize;

    public ProcessAvailableJobsUseCase(
            JobRepositoryPort repository,
            JobHandlerRegistry registry,
            BackoffPolicy backoffPolicy,
            String workerId,
            int batchSize) {
        this.repository = repository;
        this.registry = registry;
        this.backoffPolicy = backoffPolicy;
        this.workerId = workerId;
        this.batchSize = batchSize;
    }

    @Override
    public int execute() {
        Instant now = Instant.now();
        List<Job> claimed = repository.claimBatch(workerId, now, batchSize);
        if (claimed.isEmpty()) {
            return 0;
        }
        log.debug("Worker {} tomó {} job(s)", workerId, claimed.size());
        for (Job job : claimed) {
            processOne(job);
        }
        return claimed.size();
    }

    private void processOne(Job job) {
        try {
            JobHandler handler = registry.handlerFor(job.getJobType());
            String result = handler.handle(job.getPayload());
            job.markCompleted(result, Instant.now());
            repository.save(job);
            log.info("Job {} COMPLETED (type={})", job.getId(), job.getJobType());
        } catch (NonRecoverableJobException e) {
            fail(job, "No recuperable: " + e.getMessage());
        } catch (JobTypeNotSupportedException e) {
            fail(job, e.getMessage());
        } catch (Exception e) {
            handleRecoverableFailure(job, e);
        }
    }

    private void handleRecoverableFailure(Job job, Exception e) {
        String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (job.canRetry()) {
            Instant next = backoffPolicy.nextAttempt(Instant.now(), job.getAttempts() + 1);
            job.markForRetry(reason, next, Instant.now());
            repository.save(job);
            log.warn(
                    "Job {} RETRYING (intento {}/{}, próximo={}) causa={}",
                    job.getId(),
                    job.getAttempts(),
                    job.getMaxAttempts(),
                    next,
                    reason);
        } else {
            fail(job, reason);
        }
    }

    private void fail(Job job, String reason) {
        job.markFailed(reason, Instant.now());
        repository.save(job);
        log.error("Job {} FAILED (intentos={}) causa={}", job.getId(), job.getAttempts(), reason);
    }
}
