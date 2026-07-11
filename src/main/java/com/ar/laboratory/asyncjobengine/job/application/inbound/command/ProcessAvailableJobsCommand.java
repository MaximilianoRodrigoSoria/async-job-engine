package com.ar.laboratory.asyncjobengine.job.application.inbound.command;

/**
 * Puerto de entrada disparado por el worker: toma un lote de jobs elegibles y los procesa.
 *
 * @return cantidad de jobs procesados en esta pasada.
 */
public interface ProcessAvailableJobsCommand {

    int execute();
}
