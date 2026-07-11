package com.ar.laboratory.asyncjobengine.shared.infrastructure.web.api;

import com.ar.laboratory.asyncjobengine.shared.infrastructure.config.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Interfaz que define respuestas estándar de API para documentación OpenAPI.
 *
 * <p>Las interfaces que extiendan esta heredarán las respuestas comunes (500) con la estructura
 * real de {@link ErrorResponse}.
 */
@ApiResponses(
        value = {
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)))
        })
public interface StandardApiResponses {}
