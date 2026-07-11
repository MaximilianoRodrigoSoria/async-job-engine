package com.ar.laboratory.asyncjobengine.shared.infrastructure.exception;

/** Excepción técnica para peticiones inválidas */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
