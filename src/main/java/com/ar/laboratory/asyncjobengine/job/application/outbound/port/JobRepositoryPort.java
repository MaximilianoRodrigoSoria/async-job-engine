package com.ar.laboratory.asyncjobengine.job.application.outbound.port;

import com.ar.laboratory.asyncjobengine.job.application.query.JobFilter;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Puerto de salida para la persistencia y la cola de jobs. */
public interface JobRepositoryPort {

    Job save(Job job);

    Optional<Job> findById(UUID id);

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    Page<Job> findAll(JobFilter filter, Pageable pageable);

    /**
     * Toma atómicamente hasta {@code limit} jobs elegibles (PENDING/RETRYING cuya ventana de
     * ejecución ya venció), los marca como RUNNING con el {@code workerId} y los devuelve.
     *
     * <p>La atomicidad se garantiza con {@code SELECT ... FOR UPDATE SKIP LOCKED}, de modo que
     * varias instancias del worker nunca tomen el mismo job.
     */
    List<Job> claimBatch(String workerId, Instant now, int limit);

    /**
     * Reencola los jobs "zombie": quedaron en RUNNING con un lock más viejo que {@code threshold}
     * (típicamente por caída del worker). Vuelven a PENDING para reprocesarse.
     *
     * @return cantidad de jobs recuperados.
     */
    int requeueStuckJobs(Instant threshold, Instant now);
}
