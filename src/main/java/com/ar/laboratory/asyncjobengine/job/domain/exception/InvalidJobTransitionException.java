package com.ar.laboratory.asyncjobengine.job.domain.exception;

import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;

/** Se lanza cuando se intenta una transición de estado no permitida por la máquina de estados. */
public class InvalidJobTransitionException extends RuntimeException {

    public InvalidJobTransitionException(JobStatus from, JobStatus to) {
        super("Transición de estado inválida: " + from + " → " + to);
    }
}
