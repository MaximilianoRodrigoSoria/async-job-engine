package com.ar.laboratory.asyncjobengine.job.application.inbound.command;

import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import java.util.UUID;

/** Puerto de entrada: cancelar un job pendiente o en espera de reintento. */
public interface CancelJobCommand {

    Job execute(UUID id);
}
