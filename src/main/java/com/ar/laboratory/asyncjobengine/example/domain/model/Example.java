package com.ar.laboratory.asyncjobengine.example.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Modelo de dominio para Example */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Example {

    private Long id;
    private String name;
    private String dni;
}
