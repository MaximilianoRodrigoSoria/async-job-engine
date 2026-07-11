package com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entidad JPA para Example */
@Entity
@Table(
        name = "example",
        schema = "app",
        uniqueConstraints = {@UniqueConstraint(name = "uk_example_dni", columnNames = "dni")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "dni", nullable = false, unique = true, length = 20)
    private String dni;
}
