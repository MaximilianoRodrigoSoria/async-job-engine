package com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests de integración del {@link JobPersistenceAdapter} contra un PostgreSQL real (Testcontainers).
 * Verifica el mapeo jsonb, el claim atómico ({@code FOR UPDATE SKIP LOCKED}), el respeto de la
 * ventana de ejecución y la recuperación de jobs zombie.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("JobPersistenceAdapter - Integration Tests")
class JobPersistenceAdapterIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private JobRepositoryPort repository;

    private Job pending(String type, Priority priority, Instant scheduledAt) {
        return Job.createPending(
                type, "{\"k\":\"v\"}", priority, 3, null, scheduledAt, Instant.now());
    }

    @Test
    @DisplayName("save + findById conserva payload jsonb y estado")
    void saveAndFindRoundTrip() {
        Job saved = repository.save(pending("echo", Priority.NORMAL, null));

        Job found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(found.getPayload()).contains("\"k\"");
        assertThat(found.getJobType()).isEqualTo("echo");
    }

    @Test
    @DisplayName("claimBatch toma jobs elegibles, los marca RUNNING y respeta la prioridad")
    void claimMarksRunningInPriorityOrder() {
        repository.save(pending("echo", Priority.LOW, null));
        repository.save(pending("echo", Priority.HIGH, null));

        List<Job> claimed = repository.claimBatch("worker-A", Instant.now(), 10);

        assertThat(claimed).isNotEmpty();
        assertThat(claimed).allMatch(j -> j.getStatus() == JobStatus.RUNNING);
        assertThat(claimed).allMatch(j -> "worker-A".equals(j.getLockedBy()));
        // El de mayor prioridad se toma primero.
        assertThat(claimed.get(0).getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("claimBatch NO toma jobs con scheduledAt en el futuro")
    void claimSkipsFutureScheduledJobs() {
        Job future =
                repository.save(
                        pending("echo", Priority.NORMAL, Instant.now().plus(1, ChronoUnit.HOURS)));

        List<Job> claimed = repository.claimBatch("worker-B", Instant.now(), 10);

        assertThat(claimed).noneMatch(j -> j.getId().equals(future.getId()));
        Job stillPending = repository.findById(future.getId()).orElseThrow();
        assertThat(stillPending.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    @DisplayName("requeueStuckJobs reencola jobs RUNNING con lock viejo")
    void requeuesStuckJobs() {
        Instant old = Instant.now().minus(10, ChronoUnit.MINUTES);
        Job stuck =
                repository.save(
                        Job.builder()
                                .id(UUID.randomUUID())
                                .jobType("echo")
                                .payload("{}")
                                .status(JobStatus.RUNNING)
                                .priority(Priority.NORMAL)
                                .attempts(0)
                                .maxAttempts(3)
                                .lockedBy("dead-worker")
                                .lockedAt(old)
                                .createdAt(old)
                                .updatedAt(old)
                                .build());

        int recovered =
                repository.requeueStuckJobs(
                        Instant.now().minus(5, ChronoUnit.MINUTES), Instant.now());

        assertThat(recovered).isGreaterThanOrEqualTo(1);
        Job requeued = repository.findById(stuck.getId()).orElseThrow();
        assertThat(requeued.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(requeued.getLockedBy()).isNull();
    }

    @Test
    @DisplayName("findAll pagina y filtra por estado")
    void findAllFiltersByStatus() {
        repository.save(pending("echo", Priority.NORMAL, null));
        var page =
                repository.findAll(
                        new com.ar.laboratory.asyncjobengine.job.application.query.JobFilter(
                                JobStatus.PENDING, null, null),
                        PageRequest.of(0, 50));
        assertThat(page.getContent()).allMatch(j -> j.getStatus() == JobStatus.PENDING);
    }
}
