package com.ar.laboratory.asyncjobengine.job.application.usecase;

import com.ar.laboratory.asyncjobengine.job.application.inbound.command.GetJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.outbound.port.JobRepositoryPort;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobNotFoundException;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Consulta un job por id. POJO puro sin framework. */
@Slf4j
@RequiredArgsConstructor
public class GetJobUseCase implements GetJobCommand {

    private final JobRepositoryPort repository;

    @Override
    public Job execute(UUID id) {
        return repository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    }
}
