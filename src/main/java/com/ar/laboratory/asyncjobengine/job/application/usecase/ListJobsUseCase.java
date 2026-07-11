package com.ar.laboratory.asyncjobengine.job.application.usecase;

import com.ar.laboratory.asyncjobengine.job.application.inbound.command.ListJobsCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.application.query.JobFilter;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Lista jobs con filtros y paginación. POJO puro sin framework. */
@Slf4j
@RequiredArgsConstructor
public class ListJobsUseCase implements ListJobsCommand {

    private final JobRepositoryPort repository;

    @Override
    public Page<Job> execute(JobFilter filter, Pageable pageable) {
        return repository.findAll(filter, pageable);
    }
}
