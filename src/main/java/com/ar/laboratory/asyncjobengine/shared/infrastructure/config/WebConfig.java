package com.ar.laboratory.asyncjobengine.shared.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración global de CORS para la aplicación.
 *
 * <p>Los orígenes permitidos se leen desde {@code app.cors.allowed-origins} en application.yml, lo
 * que permite ajustar la política por perfil (local, dev, prod) sin necesidad de recompilar.
 */
@Configuration
@EnableConfigurationProperties(AppCorsProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(AppCorsProperties corsProperties) {
        this.allowedOrigins = corsProperties.allowedOrigins();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
