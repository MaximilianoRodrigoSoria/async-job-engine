package com.ar.laboratory.asyncjobengine.job.application.handler;

/**
 * Un {@link JobHandler} lanza esta excepción para indicar que el fallo es <b>definitivo</b> y el
 * job no debe reintentarse (va directo a FAILED), independientemente de los intentos restantes.
 */
public class NonRecoverableJobException extends RuntimeException {

    public NonRecoverableJobException(String message) {
        super(message);
    }

    public NonRecoverableJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
