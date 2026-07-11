package com.ar.laboratory.asyncjobengine.job.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ar.laboratory.asyncjobengine.job.domain.exception.InvalidJobTransitionException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Job — máquina de estados")
class JobStateMachineTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private Job pending() {
        return Job.createPending("echo", "{}", Priority.NORMAL, 3, null, null, NOW);
    }

    @Test
    @DisplayName("createPending arranca en PENDING con 0 intentos")
    void createPendingStartsPending() {
        Job job = pending();
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getAttempts()).isZero();
        assertThat(job.getId()).isNotNull();
    }

    @Test
    @DisplayName("PENDING → RUNNING → COMPLETED es un camino válido")
    void happyPath() {
        Job job = pending();
        job.markRunning("worker-1", NOW);
        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(job.getLockedBy()).isEqualTo("worker-1");

        job.markCompleted("{\"ok\":true}", NOW);
        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getResult()).contains("ok");
        assertThat(job.getLockedBy()).isNull();
    }

    @Test
    @DisplayName("markForRetry incrementa intentos y agenda el próximo intento")
    void retryIncrementsAttempts() {
        Job job = pending();
        job.markRunning("w", NOW);
        Instant next = NOW.plusSeconds(30);
        job.markForRetry("boom", next, NOW);
        assertThat(job.getStatus()).isEqualTo(JobStatus.RETRYING);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getNextAttemptAt()).isEqualTo(next);
        assertThat(job.getLastError()).isEqualTo("boom");
    }

    @Test
    @DisplayName("cancel desde PENDING es válido; desde COMPLETED es inválido")
    void cancelRules() {
        Job job = pending();
        job.cancel(NOW);
        assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);

        Job done = pending();
        done.markRunning("w", NOW);
        done.markCompleted("{}", NOW);
        assertThatThrownBy(() -> done.cancel(NOW))
                .isInstanceOf(InvalidJobTransitionException.class);
    }

    @Test
    @DisplayName("no se puede completar un job que no está RUNNING")
    void cannotCompleteNonRunning() {
        Job job = pending();
        assertThatThrownBy(() -> job.markCompleted("{}", NOW))
                .isInstanceOf(InvalidJobTransitionException.class);
    }

    @Test
    @DisplayName("requeue lleva un job FAILED de vuelta a PENDING y resetea intentos")
    void requeueResetsAttempts() {
        Job job = pending();
        job.markRunning("w", NOW);
        job.markFailed("fatal", NOW);
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getAttempts()).isEqualTo(1);

        job.requeue(NOW);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getAttempts()).isZero();
        assertThat(job.getLastError()).isNull();
    }

    @Test
    @DisplayName("canRetry es true mientras queden intentos")
    void canRetryReflectsRemainingAttempts() {
        Job job = pending(); // maxAttempts=3, attempts=0
        assertThat(job.canRetry()).isTrue(); // 0+1 < 3
        job.markRunning("w", NOW);
        job.markForRetry("e", NOW, NOW); // attempts=1
        assertThat(job.canRetry()).isTrue(); // 1+1 < 3
        job.markRunning("w", NOW);
        job.markForRetry("e", NOW, NOW); // attempts=2
        assertThat(job.canRetry()).isFalse(); // 2+1 < 3 → false
    }

    @Test
    @DisplayName("isTerminal / isClaimable reflejan la semántica de cada estado")
    void statusSemantics() {
        assertThat(JobStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(JobStatus.PENDING.isTerminal()).isFalse();
        assertThat(JobStatus.PENDING.isClaimable()).isTrue();
        assertThat(JobStatus.RETRYING.isClaimable()).isTrue();
        assertThat(JobStatus.RUNNING.isClaimable()).isFalse();
    }
}
