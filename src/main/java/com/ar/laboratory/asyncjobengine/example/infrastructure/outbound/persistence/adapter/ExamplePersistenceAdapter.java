package com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.adapter;

import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.entity.ExampleEntity;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.mapper.ExampleEntityMapper;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.repository.ExampleJpaRepository;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.specification.ExampleSpecification;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.InfrastructureException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia para Example.
 *
 * <p>Todas las operaciones de BD están protegidas por:
 *
 * <ul>
 *   <li>{@code @Retry("example-db")} — reintenta hasta 3 veces en errores transitorios.
 *   <li>{@code @CircuitBreaker("example-db")} — abre el circuito si ≥ 50% de las llamadas fallan,
 *       devolviendo un fallback seguro para no propagar la falla a la capa de dominio.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamplePersistenceAdapter implements ExampleRepositoryPort {

    private static final String RESILIENCE_INSTANCE = "example-db";

    private final ExampleJpaRepository jpaRepository;
    private final ExampleEntityMapper entityMapper;

    // =========================================================================
    // Escritura
    // =========================================================================

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "saveFallback")
    public Example save(Example example) {
        try {
            ExampleEntity entity = entityMapper.toEntity(example);
            ExampleEntity saved = jpaRepository.save(entity);
            return entityMapper.toDomain(saved);
        } catch (Exception e) {
            log.error("Error guardando Example: {}", example, e);
            throw new InfrastructureException("Error guardando Example", e);
        }
    }

    // =========================================================================
    // Lectura — lista completa (compatibilidad hacia atrás)
    // =========================================================================

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "findAllFallback")
    public List<Example> findAll() {
        try {
            return jpaRepository.findAll().stream()
                    .map(entityMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error listando Examples", e);
            throw new InfrastructureException("Error listando Examples", e);
        }
    }

    // =========================================================================
    // Lectura — paginada con filtros dinámicos
    // =========================================================================

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "findAllPagedFallback")
    public Page<Example> findAll(ExampleFilter filter, Pageable pageable) {
        try {
            Page<ExampleEntity> page =
                    jpaRepository.findAll(ExampleSpecification.of(filter), pageable);
            return page.map(entityMapper::toDomain);
        } catch (Exception e) {
            log.error("Error listando Examples con filtro: {}", filter, e);
            throw new InfrastructureException("Error listando Examples con filtro", e);
        }
    }

    // =========================================================================
    // Lectura — por ID y DNI
    // =========================================================================

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "findByIdFallback")
    public Optional<Example> findById(Long id) {
        try {
            return jpaRepository.findById(id).map(entityMapper::toDomain);
        } catch (Exception e) {
            log.error("Error buscando Example por ID: {}", id, e);
            throw new InfrastructureException("Error buscando Example por ID", e);
        }
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "findByDniFallback")
    public Optional<Example> findByDni(String dni) {
        try {
            return jpaRepository.findByDni(dni).map(entityMapper::toDomain);
        } catch (Exception e) {
            log.error("Error buscando Example por DNI: {}", dni, e);
            throw new InfrastructureException("Error buscando Example por DNI", e);
        }
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "existsByDniFallback")
    public boolean existsByDni(String dni) {
        try {
            return jpaRepository.existsByDni(dni);
        } catch (Exception e) {
            log.error("Error verificando existencia de DNI: {}", dni, e);
            throw new InfrastructureException("Error verificando existencia de DNI", e);
        }
    }

    // =========================================================================
    // Fallbacks — invocados cuando el circuito está abierto
    // =========================================================================

    @SuppressWarnings("unused")
    private Example saveFallback(Example example, Throwable t) {
        log.warn(
                "[CircuitBreaker] save fallback activado para Example DNI={}: {}",
                example.getDni(),
                t.getMessage());
        throw new InfrastructureException(
                "Servicio de persistencia no disponible temporalmente", t);
    }

    @SuppressWarnings("unused")
    private List<Example> findAllFallback(Throwable t) {
        log.warn("[CircuitBreaker] findAll fallback activado: {}", t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private Page<Example> findAllPagedFallback(
            ExampleFilter filter, Pageable pageable, Throwable t) {
        log.warn("[CircuitBreaker] findAllPaged fallback activado: {}", t.getMessage());
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @SuppressWarnings("unused")
    private Optional<Example> findByIdFallback(Long id, Throwable t) {
        log.warn("[CircuitBreaker] findById fallback activado para ID={}: {}", id, t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private Optional<Example> findByDniFallback(String dni, Throwable t) {
        log.warn(
                "[CircuitBreaker] findByDni fallback activado para DNI={}: {}",
                dni,
                t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private boolean existsByDniFallback(String dni, Throwable t) {
        log.warn(
                "[CircuitBreaker] existsByDni fallback activado para DNI={}: {}",
                dni,
                t.getMessage());
        // En caso de duda, reportar false para no bloquear operaciones de escritura
        return false;
    }
}
