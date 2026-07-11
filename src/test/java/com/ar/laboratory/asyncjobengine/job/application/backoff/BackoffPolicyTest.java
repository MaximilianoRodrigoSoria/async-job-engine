package com.ar.laboratory.asyncjobengine.job.application.backoff;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BackoffPolicy — backoff exponencial con jitter")
class BackoffPolicyTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("sin jitter, el retraso crece exponencialmente")
    void exponentialWithoutJitter() {
        BackoffPolicy policy =
                new BackoffPolicy(Duration.ofSeconds(1), Duration.ofMinutes(10), 2.0, 0.0);

        assertThat(policy.nextAttempt(NOW, 1)).isEqualTo(NOW.plusSeconds(1)); // 1s * 2^0
        assertThat(policy.nextAttempt(NOW, 2)).isEqualTo(NOW.plusSeconds(2)); // 1s * 2^1
        assertThat(policy.nextAttempt(NOW, 3)).isEqualTo(NOW.plusSeconds(4)); // 1s * 2^2
    }

    @Test
    @DisplayName("el retraso nunca supera el máximo configurado")
    void respectsMaxDelay() {
        BackoffPolicy policy =
                new BackoffPolicy(Duration.ofSeconds(1), Duration.ofSeconds(5), 2.0, 0.0);
        assertThat(policy.nextAttempt(NOW, 10)).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    @DisplayName("con jitter, el resultado queda dentro de la banda esperada")
    void jitterWithinBand() {
        BackoffPolicy policy =
                new BackoffPolicy(Duration.ofSeconds(10), Duration.ofMinutes(10), 2.0, 0.2);
        for (int i = 0; i < 50; i++) {
            Instant next = policy.nextAttempt(NOW, 1); // base 10s ± 20% → [8s, 12s]
            assertThat(next).isBetween(NOW.plusMillis(8000), NOW.plusMillis(12000));
        }
    }
}
