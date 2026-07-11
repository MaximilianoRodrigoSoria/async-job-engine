package com.ar.laboratory.asyncjobengine.job.application.inbound.command;

import com.ar.laboratory.asyncjobengine.job.domain.model.Job;

/** Puerto de entrada: encolar un job para procesamiento asíncrono. */
public interface EnqueueJobCommand {

    /**
     * Encola un job. Si {@code job} trae una {@code idempotencyKey} ya usada, devuelve el job
     * existente sin crear uno nuevo (exactly-once desde la perspectiva del cliente).
     */
    Job execute(Job job);
}
