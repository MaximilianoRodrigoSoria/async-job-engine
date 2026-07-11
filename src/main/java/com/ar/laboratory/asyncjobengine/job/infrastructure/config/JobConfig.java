package com.ar.laboratory.asyncjobengine.job.infrastructure.config;

import com.ar.laboratory.asyncjobengine.job.application.backoff.BackoffPolicy;
import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandler;
import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandlerRegistry;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.CancelJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.EnqueueJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.GetJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.ListJobsCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.ProcessAvailableJobsCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.RetryJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.application.usecase.CancelJobUseCase;
import com.ar.laboratory.asyncjobengine.job.application.usecase.EnqueueJobUseCase;
import com.ar.laboratory.asyncjobengine.job.application.usecase.GetJobUseCase;
import com.ar.laboratory.asyncjobengine.job.application.usecase.ListJobsUseCase;
import com.ar.laboratory.asyncjobengine.job.application.usecase.ProcessAvailableJobsUseCase;
import com.ar.laboratory.asyncjobengine.job.application.usecase.RetryJobUseCase;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wiring de beans del feature Job. Los casos de uso son POJOs sin framework; aquí se los conecta con
 * sus puertos. Habilita también el scheduling que usa el worker.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(JobWorkerProperties.class)
public class JobConfig {

    @Bean
    public JobHandlerRegistry jobHandlerRegistry(List<JobHandler> handlers) {
        return new JobHandlerRegistry(handlers);
    }

    @Bean
    public BackoffPolicy jobBackoffPolicy(JobWorkerProperties props) {
        JobWorkerProperties.Backoff b = props.getBackoff();
        return new BackoffPolicy(
                Duration.ofMillis(b.getBaseMs()),
                Duration.ofMillis(b.getMaxMs()),
                b.getMultiplier(),
                b.getJitter());
    }

    @Bean
    public EnqueueJobCommand enqueueJobCommand(
            JobRepositoryPort repository, JobHandlerRegistry registry) {
        return new EnqueueJobUseCase(repository, registry);
    }

    @Bean
    public GetJobCommand getJobCommand(JobRepositoryPort repository) {
        return new GetJobUseCase(repository);
    }

    @Bean
    public ListJobsCommand listJobsCommand(JobRepositoryPort repository) {
        return new ListJobsUseCase(repository);
    }

    @Bean
    public CancelJobCommand cancelJobCommand(JobRepositoryPort repository) {
        return new CancelJobUseCase(repository);
    }

    @Bean
    public RetryJobCommand retryJobCommand(JobRepositoryPort repository) {
        return new RetryJobUseCase(repository);
    }

    @Bean
    public ProcessAvailableJobsCommand processAvailableJobsCommand(
            JobRepositoryPort repository,
            JobHandlerRegistry registry,
            BackoffPolicy backoffPolicy,
            JobWorkerProperties props) {
        return new ProcessAvailableJobsUseCase(
                repository, registry, backoffPolicy, buildWorkerId(), props.getBatchSize());
    }

    /** Identificador estable-por-instancia del worker: host + sufijo aleatorio corto. */
    private static String buildWorkerId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "worker";
        }
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
