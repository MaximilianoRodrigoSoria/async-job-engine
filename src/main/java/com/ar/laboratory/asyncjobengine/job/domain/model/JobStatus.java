package com.ar.laboratory.asyncjobengine.job.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados del ciclo de vida de un {@link Job} y sus transiciones válidas.
 *
 * <pre>
 *   PENDING  ──claim──►  RUNNING ──ok──►      COMPLETED
 *   RETRYING ──claim──►  RUNNING ──retry──►   RETRYING ──(vencido)──► RUNNING
 *                        RUNNING ──fail──►    FAILED
 *   PENDING/RETRYING ──cancel──► CANCELLED
 *   FAILED/CANCELLED ──requeue──► PENDING
 * </pre>
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    private static final Map<JobStatus, Set<JobStatus>> ALLOWED =
            Map.of(
                    PENDING, EnumSet.of(RUNNING, CANCELLED),
                    RETRYING, EnumSet.of(RUNNING, CANCELLED),
                    RUNNING, EnumSet.of(COMPLETED, RETRYING, FAILED),
                    FAILED, EnumSet.of(PENDING),
                    CANCELLED, EnumSet.of(PENDING),
                    COMPLETED, EnumSet.noneOf(JobStatus.class));

    /** Indica si la transición desde este estado hacia {@code target} es válida. */
    public boolean canTransitionTo(JobStatus target) {
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(JobStatus.class)).contains(target);
    }

    /** Estados finales que no admiten más procesamiento automático. */
    public boolean isTerminal() {
        return this == COMPLETED;
    }

    /** Estados que el poller puede tomar (elegibles para ejecución). */
    public boolean isClaimable() {
        return this == PENDING || this == RETRYING;
    }
}
