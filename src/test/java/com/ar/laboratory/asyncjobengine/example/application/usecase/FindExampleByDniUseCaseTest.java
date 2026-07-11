package com.ar.laboratory.asyncjobengine.example.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleNotFoundException;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindExampleByDniUseCase — Tests unitarios")
class FindExampleByDniUseCaseTest {

    @Mock private ExampleRepositoryPort repositoryPort;

    private FindExampleByDniUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindExampleByDniUseCase(repositoryPort);
    }

    @Test
    @DisplayName("execute — cuando el DNI existe, retorna el Example correspondiente")
    void shouldReturnExampleWhenDniExists() {
        // Given
        Example domain = Example.builder().id(1L).name("John Doe").dni("12345678").build();

        when(repositoryPort.findByDni("12345678")).thenReturn(Optional.of(domain));

        // When
        Example result = useCase.execute("12345678");

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getDni()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("execute — cuando el DNI no existe, lanza ExampleNotFoundException")
    void shouldThrowNotFoundWhenDniDoesNotExist() {
        // Given
        when(repositoryPort.findByDni("00000000")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute("00000000"))
                .isInstanceOf(ExampleNotFoundException.class)
                .hasMessageContaining("00000000");
    }

    @Test
    @DisplayName("execute — delega la búsqueda al repositoryPort con el mismo DNI")
    void shouldDelegateQueryToRepositoryPort() {
        // Given
        String dni = "11111111";
        Example domain = Example.builder().id(2L).name("Ana García").dni(dni).build();

        when(repositoryPort.findByDni(dni)).thenReturn(Optional.of(domain));

        // When
        useCase.execute(dni);

        // Then
        verify(repositoryPort).findByDni(dni);
    }
}
