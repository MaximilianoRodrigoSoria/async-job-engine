package com.ar.laboratory.asyncjobengine.example.infrastructure.config;

import com.ar.laboratory.asyncjobengine.example.application.inbound.command.CreateExampleCommand;
import com.ar.laboratory.asyncjobengine.example.application.inbound.command.FindExampleByDniCommand;
import com.ar.laboratory.asyncjobengine.example.application.inbound.command.ListExamplesCommand;
import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.application.usecase.CreateExampleUseCase;
import com.ar.laboratory.asyncjobengine.example.application.usecase.FindExampleByDniUseCase;
import com.ar.laboratory.asyncjobengine.example.application.usecase.ListExamplesUseCase;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuración de beans para el feature Example
 *
 * <p>Aquí se realiza el wiring de: - Commands (puertos de entrada) - UseCases (implementaciones
 * puras) - Adaptadores transaccionales y de caché (preocupaciones de infraestructura)
 */
@Configuration
public class ExampleConfig {

    /**
     * Bean para crear Examples
     *
     * <p>Aplica transaccionalidad y eviction de caché en infraestructura
     */
    @Bean
    @Transactional
    @CacheEvict(value = "examplesByDni", key = "#example.dni")
    public CreateExampleCommand createExampleCommand(ExampleRepositoryPort repositoryPort) {
        return new CreateExampleUseCase(repositoryPort);
    }

    /**
     * Bean para buscar Example por DNI
     *
     * <p>Aplica caché de lectura en infraestructura
     */
    @Bean
    @Cacheable(value = "examplesByDni", key = "#dni")
    @Transactional(readOnly = true)
    public FindExampleByDniCommand findExampleByDniCommand(ExampleRepositoryPort repositoryPort) {
        return new FindExampleByDniUseCase(repositoryPort);
    }

    /**
     * Bean para listar todos los Examples
     *
     * <p>Aplica caché de lectura en infraestructura
     */
    @Bean
    @Cacheable(value = "examplesCache", key = "'all'")
    @Transactional(readOnly = true)
    public ListExamplesCommand listExamplesCommand(ExampleRepositoryPort repositoryPort) {
        return new ListExamplesUseCase(repositoryPort);
    }
}
