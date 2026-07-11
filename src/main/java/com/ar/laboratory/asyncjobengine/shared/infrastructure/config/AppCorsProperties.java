package com.ar.laboratory.asyncjobengine.shared.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades tipadas de CORS, leídas desde {@code app.cors.*} en {@code application.yml}.
 *
 * <p>Reemplaza el uso de {@code @Value} suelto en {@link WebConfig}, habilitando binding tipado y
 * por perfil (local / prod).
 */
@ConfigurationProperties(prefix = "app.cors")
public record AppCorsProperties(
        /** Orígenes permitidos. Acepta {@code *} o una lista separada por comas. */
        List<String> allowedOrigins) {}
