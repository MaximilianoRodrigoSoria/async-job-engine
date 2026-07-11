package com.ar.laboratory.asyncjobengine.job.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ar.laboratory.asyncjobengine.job.application.backoff.BackoffPolicy;
import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandler;
import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandlerRegistry;
import com.ar.laboratory.asyncjobengine.job.application.handler.NonRecoverableJobException;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessAvailableJobsUseCase — orquestación del worker")
class ProcessAvailableJobsUseCaseTest {

    private static final Instant NOW = Instant.now();

    @Mock private JobRepositoryPort repository;

    private final BackoffPolicy backoff =
            new BackoffPolicy(Duration.ofSeconds(1), Duration.ofSeconds(30), 2.0, 0.0);

    private JobHandler handler(String type, HandlerBody body) {
        return new JobHandler() {
            @Override
            public String jobType() {
                return type;
            }

            @Override
            public String handle(String payload) throws Exception {
                return body.run(payload);
            }
        };
    }

    private interface HandlerBody {
        String run(String payload) throws Exception;
    }

    private Job runningJob(String type, int attempts, int maxAttempts) {
        return Job.builder()
                .id(UUID.randomUUID())
                .jobType(type)
                .payload("{}")
                .status(JobStatus.RUNNING)
                .priority(Priority.NORMAL)
                .attempts(attempts)
                .maxAttempts(maxAttempts)
                .lockedBy("w")
                .lockedAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private ProcessAvailableJobsUseCase useCaseWith(JobHandler... handlers) {
        return new ProcessAvailableJobsUseCase(
                repository, new JobHandlerRegistry(List.of(handlers)), backoff, "test-worker", 10);
    }

    @Test
    @DisplayName("job exitoso → COMPLETED con el resultado del handler")
    void successCompletes() {
        Job job = runningJob("echo", 0, 3);
        when(repository.claimBatch(anyString(), any(Instant.class), anyInt()))
                .thenReturn(List.of(job));
        var useCase = useCaseWith(handler("echo", p -> "{\"done\":true}"));

        int processed = useCase.execute();

        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(repository).save(captor.capture());
        Job saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(saved.getResult()).contains("done");
    }

    @Test
    @DisplayName("falla recuperable con intentos restantes → RETRYING con próximo intento agendado")
    void recoverableFailureRetries() {
        Job job = runningJob("boom", 0, 3);
        when(repository.claimBatch(anyString(), any(Instant.class), anyInt()))
                .thenReturn(List.of(job));
        var useCase =
                useCaseWith(
                        handler(
                                "boom",
                                p -> {
                                    throw new RuntimeException("transient");
                                }));

        useCase.execute();

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(repository).save(captor.capture());
        Job saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(JobStatus.RETRYING);
        assertThat(saved.getAttempts()).isEqualTo(1);
        assertThat(saved.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("falla recuperable sin intentos restantes → FAILED")
    void recoverableFailureExhaustedFails() {
        Job job = runningJob("boom", 2, 3); // 2+1 no es < 3 → no reintenta
        when(repository.claimBatch(anyString(), any(Instant.class), anyInt()))
                .thenReturn(List.of(job));
        var useCase =
                useCaseWith(
                        handler(
                                "boom",
                                p -> {
                                    throw new RuntimeException("still failing");
                                }));

        useCase.execute();

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    @DisplayName("falla no recuperable → FAILED directo aunque queden intentos")
    void nonRecoverableFailsImmediately() {
        Job job = runningJob("fatal", 0, 5);
        when(repository.claimBatch(anyString(), any(Instant.class), anyInt()))
                .thenReturn(List.of(job));
        var useCase =
                useCaseWith(
                        handler(
                                "fatal",
                                p -> {
                                    throw new NonRecoverableJobException("no reintentar");
                                }));

        useCase.execute();

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    @DisplayName("sin jobs elegibles → no procesa nada")
    void nothingToProcess() {
        when(repository.claimBatch(anyString(), any(Instant.class), anyInt()))
                .thenReturn(List.of());
        var useCase = useCaseWith(handler("echo", p -> p));

        assertThat(useCase.execute()).isZero();
    }
}
