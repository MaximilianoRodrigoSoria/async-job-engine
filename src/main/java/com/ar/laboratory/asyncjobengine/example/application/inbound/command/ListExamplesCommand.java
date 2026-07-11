package com.ar.laboratory.asyncjobengine.example.application.inbound.command;

import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Puerto de entrada para listar Examples con paginación y filtros dinámicos */
public interface ListExamplesCommand {

    /**
     * Lista Examples aplicando filtros opcionales y paginación.
     *
     * @param filter criterios de búsqueda (name, dni, search) — todos opcionales
     * @param pageable configuración de página (número, tamaño, ordenamiento)
     * @return página de resultados
     */
    Page<Example> execute(ExampleFilter filter, Pageable pageable);
}
