package com.ar.laboratory.asyncjobengine.job.application.inbound.command;

import com.ar.laboratory.asyncjobengine.job.application.query.JobFilter;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Puerto de entrada: listar jobs con filtros y paginación. */
public interface ListJobsCommand {

    Page<Job> execute(JobFilter filter, Pageable pageable);
}
