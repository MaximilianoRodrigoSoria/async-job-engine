package com.ar.laboratory.asyncjobengine.job.infrastructure.health;

import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.repository.JobJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator de negocio para la cola de jobs. Aparece en {@code /actuator/health} como {@code
 * jobQueue} y reporta la profundidad de cola pendiente y el tamaño de la DLQ.
 */
@Slf4j
@Component("jobQueue")
@RequiredArgsConstructor
public class JobQueueHealthIndicator implements HealthIndicator {

    private final JobJpaRepository repository;

    @Override
    public Health health() {
        try {
            long pending = repository.countByStatus(JobStatus.PENDING.name());
            long retrying = repository.countByStatus(JobStatus.RETRYING.name());
            long running = repository.countByStatus(JobStatus.RUNNING.name());
            long failed = repository.countByStatus(JobStatus.FAILED.name());
            return Health.up()
                    .withDetail("pending", pending)
                    .withDetail("retrying", retrying)
                    .withDetail("running", running)
                    .withDetail("deadLetter", failed)
                    .build();
        } catch (Exception ex) {
            log.error("Health check de la cola de jobs falló: {}", ex.getMessage());
            return Health.down().withDetail("error", ex.getMessage()).build();
        }
    }
}
