package com.ar.laboratory.asyncjobengine.job.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandlerRegistry;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobTypeNotSupportedException;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnqueueJobUseCase — Tests unitarios")
class EnqueueJobUseCaseTest {

    @Mock private JobRepositoryPort repository;
    @Mock private JobHandlerRegistry registry;

    private EnqueueJobUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EnqueueJobUseCase(repository, registry);
    }

    private Job pending(String key) {
        return Job.createPending("echo", "{}", Priority.NORMAL, 3, key, null, Instant.now());
    }

    @Test
    @DisplayName("encola cuando el jobType está soportado")
    void enqueuesWhenSupported() {
        Job job = pending(null);
        when(registry.supports("echo")).thenReturn(true);
        when(repository.save(job)).thenReturn(job);

        Job result = useCase.execute(job);

        assertThat(result).isEqualTo(job);
        verify(repository).save(job);
    }

    @Test
    @DisplayName("rechaza un jobType sin handler")
    void rejectsUnsupportedType() {
        Job job = pending(null);
        when(registry.supports("echo")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(job))
                .isInstanceOf(JobTypeNotSupportedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("idempotencia: si la clave ya existe, devuelve el job existente sin guardar")
    void returnsExistingOnDuplicateIdempotencyKey() {
        Job incoming = pending("key-1");
        Job existing = pending("key-1");
        when(registry.supports("echo")).thenReturn(true);
        when(repository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        Job result = useCase.execute(incoming);

        assertThat(result).isEqualTo(existing);
        verify(repository, never()).save(any());
    }
}
