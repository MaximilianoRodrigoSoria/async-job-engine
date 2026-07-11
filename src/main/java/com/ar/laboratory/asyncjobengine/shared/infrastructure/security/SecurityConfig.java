package com.ar.laboratory.asyncjobengine.shared.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security con autenticación JWT.
 *
 * <p><strong>Activación:</strong> esta clase solo se carga cuando la propiedad {@code
 * app.security.enabled=true} está presente en {@code application.yml}. Por default está desactivada
 * para facilitar el desarrollo local.
 *
 * <pre>{@code
 * # application.yml
 * app:
 *   security:
 *     enabled: true        # activar en prod
 *     jwt:
 *       secret: "<256-bit-secret>"
 *       expiration-ms: 86400000
 * }</pre>
 *
 * <p><strong>Endpoints públicos:</strong>
 *
 * <ul>
 *   <li>{@code /actuator/**} — probes de K8s y métricas (no requieren auth)
 *   <li>{@code /api-docs/**}, {@code /swagger-ui/**} — documentación OpenAPI
 * </ul>
 *
 * <p>Todos los demás endpoints requieren un token JWT válido en el header {@code Authorization:
 * Bearer <token>}.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
public class SecurityConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties props) {
        log.info("Spring Security habilitado — JWT activo");
        return new JwtTokenProvider(props);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        return new JwtAuthenticationFilter(tokenProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {

        return http
                // Sin estado: no se crean sesiones HTTP
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Deshabilitar CSRF (API stateless con JWT, no necesita CSRF)
                .csrf(AbstractHttpConfigurer::disable)
                // Reglas de autorización
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Actuator: probes y métricas
                                        .requestMatchers("/actuator/**")
                                        .permitAll()
                                        // OpenAPI / Swagger UI
                                        .requestMatchers(
                                                "/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        // Todo lo demás requiere autenticación
                                        .anyRequest()
                                        .authenticated())
                // JWT filter antes del filtro de autenticación estándar
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
