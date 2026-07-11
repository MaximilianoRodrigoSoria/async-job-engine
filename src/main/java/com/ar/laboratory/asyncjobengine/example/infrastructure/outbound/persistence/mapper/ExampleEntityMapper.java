package com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.mapper;

import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.entity.ExampleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct para convertir entre entidades JPA y modelos de dominio.
 *
 * <p>MapStruct genera la implementación en tiempo de compilación. Spring inyecta la instancia
 * generada como cualquier otro {@code @Component}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExampleEntityMapper {

    Example toDomain(ExampleEntity entity);

    ExampleEntity toEntity(Example domain);
}
