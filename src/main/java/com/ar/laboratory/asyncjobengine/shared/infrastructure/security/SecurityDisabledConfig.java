package com.ar.laboratory.asyncjobengine.shared.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad en modo "todo permitido".
 *
 * <p>Se activa cuando {@code app.security.enabled=false} (o cuando la propiedad no está definida,
 * que es el caso por default). Su único propósito es evitar que la auto-configuración de Spring
 * Security bloquee los endpoints al estar la dependencia en el classpath.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "false", matchIfMissing = true)
public class SecurityDisabledConfig {

    @Bean
    public SecurityFilterChain permissiveFilterChain(HttpSecurity http) throws Exception {
        log.warn(
                "Spring Security desactivado (app.security.enabled=false). "
                        + "Activar en producción.");
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
