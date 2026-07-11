package com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.adapter;

import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.application.query.JobFilter;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.entity.JobEntity;
import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.mapper.JobEntityMapper;
import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.repository.JobJpaRepository;
import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.specification.JobSpecification;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.InfrastructureException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de persistencia de la cola de jobs. Traduce entre el puerto de dominio y JPA, y encapsula
 * el claim atómico ({@code FOR UPDATE SKIP LOCKED}) dentro de una transacción propia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobPersistenceAdapter implements JobRepositoryPort {

    private final JobJpaRepository repository;
    private final JobEntityMapper mapper;

    @Override
    public Job save(Job job) {
        try {
            JobEntity saved = repository.save(mapper.toEntity(job));
            return mapper.toDomain(saved);
        } catch (Exception e) {
            log.error("Error guardando job {}", job.getId(), e);
            throw new InfrastructureException("Error guardando job", e);
        }
    }

    @Override
    public Optional<Job> findById(UUID id) {
        try {
            return repository.findById(id).map(mapper::toDomain);
        } catch (Exception e) {
            log.error("Error buscando job por id {}", id, e);
            throw new InfrastructureException("Error buscando job por id", e);
        }
    }

    @Override
    public Optional<Job> findByIdempotencyKey(String idempotencyKey) {
        try {
            return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
        } catch (Exception e) {
            log.error("Error buscando job por idempotencyKey", e);
            throw new InfrastructureException("Error buscando job por idempotencyKey", e);
        }
    }

    @Override
    public Page<Job> findAll(JobFilter filter, Pageable pageable) {
        try {
            return repository.findAll(JobSpecification.of(filter), pageable).map(mapper::toDomain);
        } catch (Exception e) {
            log.error("Error listando jobs con filtro {}", filter, e);
            throw new InfrastructureException("Error listando jobs", e);
        }
    }

    /**
     * Claim atómico: dentro de una transacción, selecciona y bloquea (SKIP LOCKED) los jobs
     * elegibles, los marca RUNNING y los devuelve. Al commitear se liberan los locks; otra
     * instancia que corra en paralelo ve esos jobs ya en RUNNING y no los reprocesa.
     */
    @Override
    @Transactional
    public List<Job> claimBatch(String workerId, Instant now, int limit) {
        try {
            List<JobEntity> rows = repository.findClaimable(now, limit);
            for (JobEntity row : rows) {
                row.setStatus(JobStatus.RUNNING.name());
                row.setLockedBy(workerId);
                row.setLockedAt(now);
                row.setUpdatedAt(now);
            }
            repository.saveAll(rows);
            return rows.stream().map(mapper::toDomain).toList();
        } catch (Exception e) {
            log.error("Error tomando lote de jobs (worker={})", workerId, e);
            throw new InfrastructureException("Error tomando lote de jobs", e);
        }
    }

    @Override
    @Transactional
    public int requeueStuckJobs(Instant threshold, Instant now) {
        try {
            int recovered = repository.requeueStuck(threshold, now);
            if (recovered > 0) {
                log.warn("Reencolados {} job(s) zombie (lock < {})", recovered, threshold);
            }
            return recovered;
        } catch (Exception e) {
            log.error("Error reencolando jobs zombie", e);
            throw new InfrastructureException("Error reencolando jobs zombie", e);
        }
    }
}
