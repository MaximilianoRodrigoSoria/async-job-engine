package com.ar.laboratory.asyncjobengine.job.application.inbound.command;

import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.util.UUID;

/** Puerto de entrada: consultar un job por id. */
public interface GetJobCommand {

    Job execute(UUID id);
}
