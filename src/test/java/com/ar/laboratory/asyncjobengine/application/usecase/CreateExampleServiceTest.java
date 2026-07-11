package com.ar.laboratory.asyncjobengine.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.application.usecase.CreateExampleUseCase;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleAlreadyExistsException;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateExampleUseCase Tests")
class CreateExampleServiceTest {

    @Mock private ExampleRepositoryPort exampleRepositoryPort;

    @InjectMocks private CreateExampleUseCase createExampleUseCase;

    private Example validExample;

    @BeforeEach
    void setUp() {
        validExample = Example.builder().name("John Doe").dni("12345678").build();
    }

    @Test
    @DisplayName("Debe crear un Example exitosamente")
    void shouldCreateExampleSuccessfully() {
        // Given
        Example savedExample =
                Example.builder()
                        .id(1L)
                        .name(validExample.getName())
                        .dni(validExample.getDni())
                        .build();

        when(exampleRepositoryPort.existsByDni(anyString())).thenReturn(false);
        when(exampleRepositoryPort.save(any(Example.class))).thenReturn(savedExample);

        // When
        Example response = createExampleUseCase.execute(validExample);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo(validExample.getName());
        assertThat(response.getDni()).isEqualTo(validExample.getDni());

        verify(exampleRepositoryPort, times(1)).existsByDni(validExample.getDni());
        verify(exampleRepositoryPort, times(1)).save(any(Example.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el DNI ya existe")
    void shouldThrowExceptionWhenDniAlreadyExists() {
        // Given
        when(exampleRepositoryPort.existsByDni(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> createExampleUseCase.execute(validExample))
                .isInstanceOf(ExampleAlreadyExistsException.class)
                .hasMessageContaining("Ya existe un Example con DNI");

        verify(exampleRepositoryPort, times(1)).existsByDni(validExample.getDni());
        verify(exampleRepositoryPort, never()).save(any(Example.class));
    }
}
