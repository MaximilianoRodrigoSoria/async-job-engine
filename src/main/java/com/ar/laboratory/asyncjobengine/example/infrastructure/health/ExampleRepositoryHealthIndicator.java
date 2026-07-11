package com.ar.laboratory.asyncjobengine.example.infrastructure.health;

import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.repository.ExampleJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator de negocio para la tabla {@code app.example}.
 *
 * <p>A diferencia del {@code DataSourceHealthIndicator} estándar (que solo ejecuta {@code SELECT
 * 1}), este indicador verifica que el schema {@code app} y la tabla {@code example} existen y son
 * accesibles con el usuario configurado.
 *
 * <p>Aparece en {@code /actuator/health} como {@code exampleRepository}.
 */
@Slf4j
@Component("exampleRepository")
@RequiredArgsConstructor
public class ExampleRepositoryHealthIndicator implements HealthIndicator {

    private final ExampleJpaRepository exampleJpaRepository;

    @Override
    public Health health() {
        try {
            long count = exampleJpaRepository.count();
            return Health.up()
                    .withDetail("table", "app.example")
                    .withDetail("rowCount", count)
                    .build();
        } catch (Exception ex) {
            log.error("Health check de app.example falló: {}", ex.getMessage());
            return Health.down()
                    .withDetail("table", "app.example")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
