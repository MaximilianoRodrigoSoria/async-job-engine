package com.ar.laboratory.asyncjobengine.job.application.handler;

/**
 * Contrato de un procesador de jobs (patrón Strategy). Cada implementación atiende un {@code
 * jobType} distinto; agregar un tipo nuevo no toca el núcleo del motor.
 *
 * <p>Las implementaciones concretas viven en la capa de infraestructura, pero implementan esta
 * interfaz de aplicación para no acoplar el motor a detalles de infraestructura.
 */
public interface JobHandler {

    /** Identificador del tipo de job que este handler sabe procesar. */
    String jobType();

    /**
     * Procesa el job.
     *
     * @param payload datos de entrada del job (cadena JSON).
     * @return resultado del procesamiento como cadena JSON (o {@code null} si no hay resultado).
     * @throws NonRecoverableJobException si el error es definitivo y NO debe reintentarse.
     * @throws Exception cualquier otra excepción se considera recuperable y dispara reintento.
     */
    String handle(String payload) throws Exception;
}
