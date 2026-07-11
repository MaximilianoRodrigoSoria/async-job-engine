package com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA de la cola de jobs (tabla {@code app.job}).
 *
 * <p>{@code payload} y {@code result} se mapean a columnas {@code jsonb} de PostgreSQL mediante
 * {@link JdbcTypeCode}; {@code status} se guarda como texto y {@code priority} como su peso
 * numérico (para poder ordenar por prioridad en el claim).
 */
@Entity
@Table(name = "job", schema = "app")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 120)
    private String jobType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "locked_by", length = 200)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private String result;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
