package com.ar.laboratory.asyncjobengine.shared.infrastructure.exception;

/**
 * Excepción lanzada cuando falla la serialización o deserialización de JSON.
 *
 * <p>Es una excepción unchecked (extiende {@link RuntimeException}) para no contaminar las firmas
 * de los métodos de la capa de aplicación con detalles de infraestructura.
 */
public class JsonParsingException extends RuntimeException {

    public JsonParsingException(String message) {
        super(message);
    }

    public JsonParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
