package com.ar.laboratory.asyncjobengine.job.domain.exception;

/** Se lanza cuando se encola un jobType para el que no hay ningún handler registrado. */
public class JobTypeNotSupportedException extends RuntimeException {

    public JobTypeNotSupportedException(String jobType) {
        super("No hay handler registrado para el jobType: " + jobType);
    }
}
