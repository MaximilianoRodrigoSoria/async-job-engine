package com.ar.laboratory.asyncjobengine.example.application.usecase;

import com.ar.laboratory.asyncjobengine.example.application.inbound.command.CreateExampleCommand;
import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleAlreadyExistsException;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Caso de uso para crear un Example - POJO puro sin framework */
@Slf4j
@RequiredArgsConstructor
public class CreateExampleUseCase implements CreateExampleCommand {

    private final ExampleRepositoryPort exampleRepositoryPort;

    @Override
    public Example execute(Example example) {
        log.info("Creando Example con DNI: {}", example.getDni());

        // Verificar si ya existe un Example con el mismo DNI
        if (exampleRepositoryPort.existsByDni(example.getDni())) {
            log.warn("Ya existe un Example con DNI: {}", example.getDni());
            throw new ExampleAlreadyExistsException(example.getDni());
        }

        // Guardar
        Example savedExample = exampleRepositoryPort.save(example);

        log.info("Example creado exitosamente con ID: {}", savedExample.getId());

        return savedExample;
    }
}
