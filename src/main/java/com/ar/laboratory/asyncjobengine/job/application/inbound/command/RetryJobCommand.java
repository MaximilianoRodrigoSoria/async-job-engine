package com.ar.laboratory.asyncjobengine.job.application.inbound.command;

import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.util.UUID;

/** Puerto de entrada: reencolar manualmente un job fallido o cancelado (reproceso de DLQ). */
public interface RetryJobCommand {

    Job execute(UUID id);
}
