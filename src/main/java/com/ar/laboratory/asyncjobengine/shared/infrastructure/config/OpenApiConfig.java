package com.ar.laboratory.asyncjobengine.shared.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuración de OpenAPI/Swagger para documentación de la API */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Base API - REST API")
                                .version("1.0.0")
                                .description(
                                        "API REST para gestión de ejemplos con arquitectura"
                                                + " hexagonal")
                                .contact(
                                        new Contact()
                                                .name("Laboratorio")
                                                .email("maximilianorodrigosoria@gmail.com")
                                                .url(
                                                        "https://github.com/MaximilianoRodrigoSoria/async-job-engine"))
                                .license(
                                        new License()
                                                .name("MIT License")
                                                .url("https://opensource.org/licenses/MIT")))
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:8080/async-job-engine")
                                        .description("Servidor de Desarrollo")));
    }
}
