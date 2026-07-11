package com.ar.laboratory.asyncjobengine.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.application.usecase.FindExampleByDniUseCase;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleNotFoundException;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindExampleByDniServiceTest {

    @Mock private ExampleRepositoryPort exampleRepositoryPort;

    @InjectMocks private FindExampleByDniUseCase findExampleByDniUseCase;

    private Example example;
    private String testDni;

    @BeforeEach
    void setUp() {
        testDni = "12345678";
        example = Example.builder().id(1L).name("Test Example").dni(testDni).build();
    }

    @Test
    @DisplayName("Debe encontrar un Example por DNI exitosamente")
    void shouldFindExampleByDniSuccessfully() {
        // Arrange
        when(exampleRepositoryPort.findByDni(testDni)).thenReturn(Optional.of(example));

        // Act
        Example result = findExampleByDniUseCase.execute(testDni);

        // Assert
        assertNotNull(result);
        assertEquals(example.getId(), result.getId());
        assertEquals(example.getName(), result.getName());
        assertEquals(example.getDni(), result.getDni());

        verify(exampleRepositoryPort, times(1)).findByDni(testDni);
    }

    @Test
    @DisplayName("Debe lanzar ExampleNotFoundException cuando no encuentra el DNI")
    void shouldThrowExampleNotFoundExceptionWhenDniNotFound() {
        // Arrange
        String nonExistentDni = "99999999";
        when(exampleRepositoryPort.findByDni(nonExistentDni)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ExampleNotFoundException.class,
                () -> findExampleByDniUseCase.execute(nonExistentDni));

        verify(exampleRepositoryPort, times(1)).findByDni(nonExistentDni);
    }

    @Test
    @DisplayName("Debe retornar el Example correcto con todos sus campos")
    void shouldReturnCompleteExampleData() {
        // Arrange
        Example completeExample =
                Example.builder().id(10L).name("Complete Example").dni("87654321").build();

        when(exampleRepositoryPort.findByDni("87654321")).thenReturn(Optional.of(completeExample));

        // Act
        Example result = findExampleByDniUseCase.execute("87654321");

        // Assert
        assertEquals(10L, result.getId());
        assertEquals("Complete Example", result.getName());
        assertEquals("87654321", result.getDni());
    }
}
