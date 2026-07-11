package com.ar.laboratory.asyncjobengine.example.application.usecase;

import com.ar.laboratory.asyncjobengine.example.application.inbound.command.ListExamplesCommand;
import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Caso de uso para listar Examples con paginación y filtros dinámicos. POJO puro sin framework. */
@Slf4j
@RequiredArgsConstructor
public class ListExamplesUseCase implements ListExamplesCommand {

    private final ExampleRepositoryPort exampleRepositoryPort;

    @Override
    public Page<Example> execute(ExampleFilter filter, Pageable pageable) {
        log.info(
                "Listando Examples — página={}, tamaño={}, filtro={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                filter);
        Page<Example> page = exampleRepositoryPort.findAll(filter, pageable);
        log.info(
                "Resultado: {} elements en página {}/{}",
                page.getNumberOfElements(),
                page.getNumber() + 1,
                page.getTotalPages());
        return page;
    }
}
