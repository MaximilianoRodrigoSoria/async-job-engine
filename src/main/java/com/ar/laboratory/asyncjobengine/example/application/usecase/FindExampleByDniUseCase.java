package com.ar.laboratory.asyncjobengine.example.application.usecase;

import com.ar.laboratory.asyncjobengine.example.application.inbound.command.FindExampleByDniCommand;
import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleNotFoundException;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Caso de uso para buscar Example por DNI - POJO puro sin framework */
@Slf4j
@RequiredArgsConstructor
public class FindExampleByDniUseCase implements FindExampleByDniCommand {

    private final ExampleRepositoryPort exampleRepositoryPort;

    @Override
    public Example execute(String dni) {
        log.info("Buscando Example por DNI: {}", dni);

        Example example =
                exampleRepositoryPort
                        .findByDni(dni)
                        .orElseThrow(
                                () -> {
                                    log.error("Example no encontrado con DNI: {}", dni);
                                    return new ExampleNotFoundException(
                                            "Example no encontrado con DNI: " + dni);
                                });

        log.info("Example encontrado: {}", example.getName());

        return example;
    }
}
