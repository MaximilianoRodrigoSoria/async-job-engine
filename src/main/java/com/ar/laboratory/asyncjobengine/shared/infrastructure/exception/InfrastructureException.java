package com.ar.laboratory.asyncjobengine.shared.infrastructure.exception;

/** Excepción técnica de infraestructura */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(String message) {
        super(message);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
