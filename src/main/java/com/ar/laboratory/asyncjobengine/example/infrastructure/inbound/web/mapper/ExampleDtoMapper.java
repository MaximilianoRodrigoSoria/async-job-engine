package com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.mapper;

import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.CreateExampleRequest;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.ExampleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct para convertir entre DTOs web y modelos de dominio.
 *
 * <p>MapStruct genera la implementación en tiempo de compilación. Spring inyecta la instancia
 * generada como cualquier otro {@code @Component}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExampleDtoMapper {

    ExampleResponse toResponse(Example domain);

    /**
     * Convierte un request de creación al modelo de dominio.
     *
     * <p>{@code id} se ignora explícitamente: la capa de persistencia lo asigna al persistir.
     */
    @Mapping(target = "id", ignore = true)
    Example toDomain(CreateExampleRequest request);
}
