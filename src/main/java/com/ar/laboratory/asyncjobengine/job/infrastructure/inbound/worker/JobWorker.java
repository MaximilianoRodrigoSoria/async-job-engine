package com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.worker;

import com.ar.laboratory.asyncjobengine.job.application.inbound.command.ProcessAvailableJobsCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.infrastructure.config.JobWorkerProperties;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada dirigido por scheduler: dispara periódicamente el procesamiento de la cola y
 * la recuperación de jobs zombie. Se desactiva con {@code app.worker.enabled=false} (útil en tests,
 * donde el procesamiento se invoca manualmente).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.worker.enabled", havingValue = "true", matchIfMissing = true)
public class JobWorker {

    private final ProcessAvailableJobsCommand processAvailableJobsCommand;
    private final JobRepositoryPort repository;
    private final JobWorkerProperties properties;

    /** Toma y procesa un lote de jobs elegibles. */
    @Scheduled(fixedDelayString = "${app.worker.poll-interval-ms:1000}")
    public void poll() {
        try {
            processAvailableJobsCommand.execute();
        } catch (Exception e) {
            // Nunca dejamos que una excepción mate el scheduler.
            log.error("Error en la pasada del worker: {}", e.getMessage(), e);
        }
    }

    /** Reencola jobs "zombie" (RUNNING con lock viejo por caída de un worker). */
    @Scheduled(fixedDelayString = "${app.worker.reaper-interval-ms:60000}")
    public void reapStuckJobs() {
        try {
            Instant now = Instant.now();
            Instant threshold = now.minusMillis(properties.getStuckAfterMs());
            repository.requeueStuckJobs(threshold, now);
        } catch (Exception e) {
            log.error("Error en el reaper de jobs zombie: {}", e.getMessage(), e);
        }
    }
}
