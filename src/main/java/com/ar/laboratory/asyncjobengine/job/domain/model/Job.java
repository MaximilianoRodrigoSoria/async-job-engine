package com.ar.laboratory.asyncjobengine.job.domain.model;

import com.ar.laboratory.asyncjobengine.job.domain.exception.InvalidJobTransitionException;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Agregado de dominio que representa una tarea asíncrona (job).
 *
 * <p>Concentra la <b>máquina de estados</b>: las transiciones se realizan exclusivamente a través
 * de los métodos de comportamiento ({@code markRunning}, {@code markCompleted}, …), que validan el
 * estado de origen y lanzan {@link InvalidJobTransitionException} ante una transición inválida. No
 * expone setters: el estado solo cambia mediante estas operaciones.
 *
 * <p>El {@code payload} y el {@code result} se manejan como cadenas JSON, dejando el modelo de
 * dominio libre de dependencias de serialización.
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Job {

    /** Longitud máxima almacenada del último error (se trunca para no crecer sin control). */
    private static final int MAX_ERROR_LENGTH = 4000;

    private UUID id;
    private String jobType;
    private String payload;
    private JobStatus status;
    private Priority priority;
    private String idempotencyKey;

    private int attempts;
    private int maxAttempts;

    private Instant scheduledAt;
    private Instant nextAttemptAt;

    private String lockedBy;
    private Instant lockedAt;

    private String lastError;
    private String result;

    private Instant createdAt;
    private Instant updatedAt;

    // =========================================================================
    // Fábrica
    // =========================================================================

    /** Crea un job nuevo en estado {@link JobStatus#PENDING}, listo para encolar. */
    public static Job createPending(
            String jobType,
            String payload,
            Priority priority,
            int maxAttempts,
            String idempotencyKey,
            Instant scheduledAt,
            Instant now) {
        return Job.builder()
                .id(UUID.randomUUID())
                .jobType(jobType)
                .payload(payload == null ? "{}" : payload)
                .status(JobStatus.PENDING)
                .priority(priority == null ? Priority.NORMAL : priority)
                .idempotencyKey(idempotencyKey)
                .attempts(0)
                .maxAttempts(maxAttempts <= 0 ? 3 : maxAttempts)
                .scheduledAt(scheduledAt)
                .nextAttemptAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // =========================================================================
    // Transiciones
    // =========================================================================

    /** PENDING/RETRYING → RUNNING: un worker toma el job. */
    public void markRunning(String workerId, Instant now) {
        transitionTo(JobStatus.RUNNING);
        this.lockedBy = workerId;
        this.lockedAt = now;
        this.updatedAt = now;
    }

    /** RUNNING → COMPLETED con el resultado producido por el handler. */
    public void markCompleted(String result, Instant now) {
        transitionTo(JobStatus.COMPLETED);
        this.result = result;
        this.lastError = null;
        clearLock();
        this.nextAttemptAt = null;
        this.updatedAt = now;
    }

    /** RUNNING → RETRYING: falla recuperable con intentos restantes; agenda el próximo intento. */
    public void markForRetry(String error, Instant nextAttemptAt, Instant now) {
        transitionTo(JobStatus.RETRYING);
        this.attempts += 1;
        this.lastError = truncate(error);
        this.nextAttemptAt = nextAttemptAt;
        clearLock();
        this.updatedAt = now;
    }

    /** RUNNING → FAILED: falla no recuperable o se agotaron los intentos (queda en la DLQ). */
    public void markFailed(String error, Instant now) {
        transitionTo(JobStatus.FAILED);
        this.attempts += 1;
        this.lastError = truncate(error);
        this.nextAttemptAt = null;
        clearLock();
        this.updatedAt = now;
    }

    /** PENDING/RETRYING → CANCELLED por pedido del usuario. */
    public void cancel(Instant now) {
        transitionTo(JobStatus.CANCELLED);
        clearLock();
        this.nextAttemptAt = null;
        this.updatedAt = now;
    }

    /** FAILED/CANCELLED → PENDING: reintento manual / reproceso desde la DLQ (resetea intentos). */
    public void requeue(Instant now) {
        transitionTo(JobStatus.PENDING);
        this.attempts = 0;
        this.lastError = null;
        this.nextAttemptAt = null;
        clearLock();
        this.updatedAt = now;
    }

    // =========================================================================
    // Consultas de negocio
    // =========================================================================

    /** {@code true} si aún quedan intentos disponibles para reintentar tras una falla. */
    public boolean canRetry() {
        return (attempts + 1) < maxAttempts;
    }

    // =========================================================================
    // Interno
    // =========================================================================

    private void transitionTo(JobStatus target) {
        if (status == null || !status.canTransitionTo(target)) {
            throw new InvalidJobTransitionException(status, target);
        }
        this.status = target;
    }

    private void clearLock() {
        this.lockedBy = null;
        this.lockedAt = null;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
