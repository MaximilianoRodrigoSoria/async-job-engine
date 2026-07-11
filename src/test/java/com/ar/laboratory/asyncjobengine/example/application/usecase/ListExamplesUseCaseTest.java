package com.ar.laboratory.asyncjobengine.example.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListExamplesUseCase — Tests unitarios")
class ListExamplesUseCaseTest {

    @Mock private ExampleRepositoryPort repositoryPort;

    private ListExamplesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListExamplesUseCase(repositoryPort);
    }

    @Test
    @DisplayName("execute — retorna la página con resultados del repositorio")
    void shouldReturnPageFromRepository() {
        // Given
        ExampleFilter filter = new ExampleFilter(null, null, null);
        Pageable pageable = PageRequest.of(0, 20);

        Example domain = Example.builder().id(1L).name("John Doe").dni("12345678").build();
        Page<Example> page = new PageImpl<>(List.of(domain), pageable, 1);

        when(repositoryPort.findAll(filter, pageable)).thenReturn(page);

        // When
        Page<Example> result = useCase.execute(filter, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("execute — retorna página vacía cuando el repositorio no tiene resultados")
    void shouldReturnEmptyPageWhenNoResults() {
        // Given
        ExampleFilter filter = new ExampleFilter("inexistente", null, null);
        Pageable pageable = PageRequest.of(0, 20);

        Page<Example> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repositoryPort.findAll(filter, pageable)).thenReturn(emptyPage);

        // When
        Page<Example> result = useCase.execute(filter, pageable);

        // Then
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("execute — delega al repositoryPort con el filter y pageable recibidos")
    void shouldDelegateToRepositoryPortWithCorrectArguments() {
        // Given
        ExampleFilter filter = new ExampleFilter(null, null, "John");
        Pageable pageable = PageRequest.of(1, 10);

        when(repositoryPort.findAll(any(ExampleFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        useCase.execute(filter, pageable);

        // Then
        verify(repositoryPort).findAll(filter, pageable);
    }

    @Test
    @DisplayName("execute — propaga el número de página y tamaño correctamente en el resultado")
    void shouldPreservePageableInformation() {
        // Given
        ExampleFilter filter = new ExampleFilter(null, null, null);
        Pageable pageable = PageRequest.of(2, 5);

        Example domain = Example.builder().id(1L).name("Test").dni("00000001").build();
        Page<Example> page = new PageImpl<>(List.of(domain), pageable, 11);

        when(repositoryPort.findAll(filter, pageable)).thenReturn(page);

        // When
        Page<Example> result = useCase.execute(filter, pageable);

        // Then
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }
}
