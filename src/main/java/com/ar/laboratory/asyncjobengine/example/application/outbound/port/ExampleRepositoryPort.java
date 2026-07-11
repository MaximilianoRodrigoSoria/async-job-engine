package com.ar.laboratory.asyncjobengine.example.application.outbound.port;

import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Puerto de salida para persistencia de Example */
public interface ExampleRepositoryPort {

    Example save(Example example);

    /** Retorna todos los Examples sin paginación (compatibilidad hacia atrás). */
    List<Example> findAll();

    /**
     * Retorna una página de Examples aplicando filtros dinámicos.
     *
     * @param filter criterios de búsqueda opcionales (name, dni, search)
     * @param pageable configuración de página y ordenamiento
     * @return página con los resultados
     */
    Page<Example> findAll(ExampleFilter filter, Pageable pageable);

    Optional<Example> findById(Long id);

    Optional<Example> findByDni(String dni);

    boolean existsByDni(String dni);
}
