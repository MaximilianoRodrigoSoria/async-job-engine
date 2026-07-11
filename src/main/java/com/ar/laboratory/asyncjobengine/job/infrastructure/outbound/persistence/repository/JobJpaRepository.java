package com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.repository;

import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.entity.JobEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio JPA de {@link JobEntity}, con el claim atómico de la cola. */
public interface JobJpaRepository
        extends JpaRepository<JobEntity, UUID>, JpaSpecificationExecutor<JobEntity> {

    Optional<JobEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Selecciona hasta {@code limit} jobs elegibles y los bloquea con {@code FOR UPDATE SKIP
     * LOCKED}: los jobs PENDING/RETRYING cuya ventana de ejecución (scheduled_at / next_attempt_at)
     * ya venció, ordenados por prioridad (desc) y antigüedad (asc). Las filas bloqueadas por otra
     * transacción se saltan, de modo que dos workers nunca toman el mismo job.
     */
    @Query(
            value =
                    "SELECT * FROM app.job j "
                            + "WHERE j.status IN ('PENDING','RETRYING') "
                            + "AND (j.scheduled_at IS NULL OR j.scheduled_at <= :now) "
                            + "AND (j.next_attempt_at IS NULL OR j.next_attempt_at <= :now) "
                            + "ORDER BY j.priority DESC, j.created_at ASC "
                            + "LIMIT :limit "
                            + "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<JobEntity> findClaimable(@Param("now") Instant now, @Param("limit") int limit);

    /**
     * Reencola jobs "zombie": quedaron RUNNING con un lock más viejo que {@code threshold}. Vuelven
     * a PENDING para reprocesarse.
     */
    @Modifying
    @Query(
            value =
                    "UPDATE app.job "
                            + "SET status = 'PENDING', locked_by = NULL, locked_at = NULL, "
                            + "updated_at = :now "
                            + "WHERE status = 'RUNNING' AND locked_at < :threshold",
            nativeQuery = true)
    int requeueStuck(@Param("threshold") Instant threshold, @Param("now") Instant now);

    /** Cuenta jobs por estado (para métricas de salud de la cola). */
    long countByStatus(String status);
}
