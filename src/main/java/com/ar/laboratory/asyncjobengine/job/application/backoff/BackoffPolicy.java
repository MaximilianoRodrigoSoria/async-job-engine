package com.ar.laboratory.asyncjobengine.job.application.backoff;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Política de backoff exponencial con jitter para los reintentos.
 *
 * <p>El retraso del intento {@code n} (1-based) es:
 *
 * <pre>
 *   delay = min(maxDelay, baseDelay * multiplier^(n-1)) ± jitter
 * </pre>
 *
 * El jitter (aleatorio, acotado por {@code jitterFactor}) evita las "tormentas de reintentos"
 * cuando muchos jobs fallan a la vez.
 */
public class BackoffPolicy {

    private final Duration baseDelay;
    private final Duration maxDelay;
    private final double multiplier;
    private final double jitterFactor;

    public BackoffPolicy(
            Duration baseDelay, Duration maxDelay, double multiplier, double jitterFactor) {
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
        this.multiplier = multiplier;
        this.jitterFactor = Math.max(0.0, Math.min(1.0, jitterFactor));
    }

    /**
     * Calcula el instante del próximo intento a partir de {@code now} y el número de intento.
     *
     * @param now instante base.
     * @param attemptNumber número de intento que se va a agendar (1-based).
     */
    public Instant nextAttempt(Instant now, int attemptNumber) {
        long baseMillis = baseDelay.toMillis();
        int exponent = Math.max(0, attemptNumber - 1);

        double raw = baseMillis * Math.pow(multiplier, exponent);
        long capped = (long) Math.min((double) maxDelay.toMillis(), raw);

        long jitter = 0L;
        if (jitterFactor > 0 && capped > 0) {
            long bound = (long) (capped * jitterFactor);
            if (bound > 0) {
                jitter = ThreadLocalRandom.current().nextLong(-bound, bound + 1);
            }
        }
        long delay = Math.max(0L, capped + jitter);
        return now.plusMillis(delay);
    }
}
