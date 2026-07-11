package com.ar.laboratory.asyncjobengine.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ar.laboratory.asyncjobengine.example.application.outbound.port.ExampleRepositoryPort;
import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.application.usecase.ListExamplesUseCase;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListExamplesUseCase Tests")
class ListExamplesServiceTest {

    @Mock private ExampleRepositoryPort exampleRepositoryPort;

    @InjectMocks private ListExamplesUseCase listExamplesUseCase;

    @Test
    @DisplayName("Debe listar todos los Examples exitosamente")
    void shouldListAllExamplesSuccessfully() {
        // Given
        List<Example> examples =
                List.of(
                        Example.builder().id(1L).name("John Doe").dni("12345678").build(),
                        Example.builder().id(2L).name("Jane Smith").dni("87654321").build());

        Pageable pageable = PageRequest.of(0, 20);
        Page<Example> page = new PageImpl<>(examples, pageable, examples.size());

        when(exampleRepositoryPort.findAll(any(ExampleFilter.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        ExampleFilter filter = new ExampleFilter(null, null, null);
        Page<Example> result = listExamplesUseCase.execute(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Jane Smith");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(exampleRepositoryPort, times(1))
                .findAll(any(ExampleFilter.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Debe retornar página vacía cuando no hay Examples")
    void shouldReturnEmptyPageWhenNoExamples() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Example> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(exampleRepositoryPort.findAll(any(ExampleFilter.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        ExampleFilter filter = new ExampleFilter(null, null, null);
        Page<Example> result = listExamplesUseCase.execute(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(exampleRepositoryPort, times(1))
                .findAll(any(ExampleFilter.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Debe filtrar por search cuando se provee el parámetro")
    void shouldFilterBySearchTerm() {
        // Given
        List<Example> filtered =
                List.of(Example.builder().id(1L).name("John Doe").dni("12345678").build());

        Pageable pageable = PageRequest.of(0, 20);
        Page<Example> page = new PageImpl<>(filtered, pageable, 1);

        when(exampleRepositoryPort.findAll(any(ExampleFilter.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        ExampleFilter filter = new ExampleFilter(null, null, "John");
        Page<Example> result = listExamplesUseCase.execute(filter, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");

        verify(exampleRepositoryPort, times(1))
                .findAll(any(ExampleFilter.class), any(Pageable.class));
    }
}
