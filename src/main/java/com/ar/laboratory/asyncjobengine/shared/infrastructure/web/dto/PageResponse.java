package com.ar.laboratory.asyncjobengine.shared.infrastructure.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envoltorio de respuesta paginada, independiente de la implementación interna de Spring Data.
 *
 * <p>Se utiliza para exponer la paginación en la API REST sin filtrar la abstracción de {@link
 * Page} al cliente.
 *
 * @param <T> tipo del contenido de la página
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    /**
     * Construye un {@code PageResponse} a partir de un {@link Page} de Spring Data.
     *
     * @param springPage página resultante de un repositorio JPA
     * @param <T> tipo del contenido
     * @return respuesta paginada serializable
     */
    public static <T> PageResponse<T> of(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast());
    }
}
