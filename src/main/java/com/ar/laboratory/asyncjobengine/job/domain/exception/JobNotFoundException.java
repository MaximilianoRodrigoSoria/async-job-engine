package com.ar.laboratory.asyncjobengine.job.domain.exception;

import java.util.UUID;

/** Se lanza cuando no se encuentra un Job por su identificador. */
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID id) {
        super("Job no encontrado con id: " + id);
    }
}
