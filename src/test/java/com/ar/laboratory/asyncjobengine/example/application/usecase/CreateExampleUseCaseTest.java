package com.ar.laboratory.asyncjobengine.example.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleAlreadyExistsException;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateExampleUseCase — Tests unitarios")
class CreateExampleUseCaseTest {

    @Mock private ExampleRepositoryPort repositoryPort;

    private CreateExampleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateExampleUseCase(repositoryPort);
    }

    @Test
    @DisplayName("execute — cuando el DNI no existe, crea y retorna el Example guardado")
    void shouldCreateAndReturnExampleWhenDniIsNew() {
        // Given
        Example input = Example.builder().name("John Doe").dni("12345678").build();
        Example saved = Example.builder().id(1L).name("John Doe").dni("12345678").build();

        when(repositoryPort.existsByDni("12345678")).thenReturn(false);
        when(repositoryPort.save(input)).thenReturn(saved);

        // When
        Example result = useCase.execute(input);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getDni()).isEqualTo("12345678");
        verify(repositoryPort).save(input);
    }

    @Test
    @DisplayName("execute — cuando el DNI ya existe, lanza ExampleAlreadyExistsException")
    void shouldThrowExceptionWhenDniAlreadyExists() {
        // Given
        Example input = Example.builder().name("John Doe").dni("12345678").build();

        when(repositoryPort.existsByDni("12345678")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(ExampleAlreadyExistsException.class);

        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("execute — no llama a save si el DNI ya existe")
    void shouldNotPersistWhenDniAlreadyExists() {
        // Given
        Example input = Example.builder().name("Jane Doe").dni("99999999").build();

        when(repositoryPort.existsByDni("99999999")).thenReturn(true);

        // When
        try {
            useCase.execute(input);
        } catch (ExampleAlreadyExistsException ignored) {
        }

        // Then
        verify(repositoryPort, never()).save(any(Example.class));
    }

    @Test
    @DisplayName("execute — delega el guardado con el mismo objeto de dominio recibido")
    void shouldDelegateSaveWithSameDomainObject() {
        // Given
        Example input = Example.builder().name("Maria Lopez").dni("87654321").build();
        Example saved = Example.builder().id(5L).name("Maria Lopez").dni("87654321").build();

        when(repositoryPort.existsByDni("87654321")).thenReturn(false);
        when(repositoryPort.save(input)).thenReturn(saved);

        // When
        useCase.execute(input);

        // Then
        verify(repositoryPort).save(input);
    }
}
