package com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para crear un Example */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExampleRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
    private String name;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(max = 20, message = "El DNI no puede tener más de 20 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El DNI debe ser alfanumérico")
    private String dni;
}
