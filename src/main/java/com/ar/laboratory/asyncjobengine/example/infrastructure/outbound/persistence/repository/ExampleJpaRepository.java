package com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.repository;

import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.entity.ExampleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Repositorio JPA para ExampleEntity */
public interface ExampleJpaRepository
        extends JpaRepository<ExampleEntity, Long>, JpaSpecificationExecutor<ExampleEntity> {

    Optional<ExampleEntity> findByDni(String dni);

    boolean existsByDni(String dni);
}
