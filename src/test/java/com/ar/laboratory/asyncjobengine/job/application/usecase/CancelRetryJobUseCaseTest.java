package com.ar.laboratory.asyncjobengine.job.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobNotFoundException;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cancel/Retry JobUseCase — Tests unitarios")
class CancelRetryJobUseCaseTest {

    @Mock private JobRepositoryPort repository;

    private Job pending() {
        return Job.createPending("echo", "{}", Priority.NORMAL, 3, null, null, Instant.now());
    }

    private Job failed() {
        Job job = pending();
        job.markRunning("w", Instant.now());
        job.markFailed("boom", Instant.now());
        return job;
    }

    @Test
    @DisplayName("cancel: PENDING → CANCELLED y persiste")
    void cancelPending() {
        UUID id = UUID.randomUUID();
        Job job = pending();
        when(repository.findById(id)).thenReturn(Optional.of(job));
        when(repository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = new CancelJobUseCase(repository).execute(id);

        assertThat(result.getStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel: id inexistente → JobNotFoundException")
    void cancelMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CancelJobUseCase(repository).execute(id))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    @DisplayName("retry: FAILED → PENDING con intentos reseteados")
    void retryFailed() {
        UUID id = UUID.randomUUID();
        Job job = failed();
        when(repository.findById(id)).thenReturn(Optional.of(job));
        when(repository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = new RetryJobUseCase(repository).execute(id);

        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(result.getAttempts()).isZero();
    }
}
