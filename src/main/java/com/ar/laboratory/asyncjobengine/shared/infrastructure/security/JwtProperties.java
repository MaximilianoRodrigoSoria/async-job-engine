package com.ar.laboratory.asyncjobengine.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración para JWT.
 *
 * <p>Se leen desde {@code app.security.jwt.*} en {@code application.yml}.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        /** Secret de firma (mínimo 256 bits en producción). */
        String secret,
        /** Tiempo de expiración del token en milisegundos. Default: 86400000 (24 h). */
        long expirationMs) {}
