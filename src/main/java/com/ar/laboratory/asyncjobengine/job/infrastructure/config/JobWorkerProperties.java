package com.ar.laboratory.asyncjobengine.job.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del worker de jobs (prefijo {@code app.worker}). */
@Data
@ConfigurationProperties(prefix = "app.worker")
public class JobWorkerProperties {

    /** Habilita el procesamiento en background. En tests se desactiva para controlarlo a mano. */
    private boolean enabled = true;

    /** Cantidad máxima de jobs tomados por pasada del poller. */
    private int batchSize = 10;

    /** Intervalo del poller, en milisegundos. */
    private long pollIntervalMs = 1000;

    /** Intervalo del reaper de jobs zombie, en milisegundos. */
    private long reaperIntervalMs = 60000;

    /** Antigüedad del lock (ms) a partir de la cual un job RUNNING se considera zombie. */
    private long stuckAfterMs = 120000;

    private Backoff backoff = new Backoff();

    /** Parámetros del backoff exponencial de reintentos. */
    @Data
    public static class Backoff {
        private long baseMs = 1000;
        private long maxMs = 300000;
        private double multiplier = 2.0;
        private double jitter = 0.2;
    }
}
