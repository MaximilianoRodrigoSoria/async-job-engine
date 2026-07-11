package com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.entity.ExampleEntity;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.mapper.ExampleEntityMapper;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.repository.ExampleJpaRepository;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.InfrastructureException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExamplePersistenceAdapter Tests")
class ExamplePersistenceAdapterTest {

    @Mock private ExampleJpaRepository jpaRepository;
    @Mock private ExampleEntityMapper entityMapper;

    @InjectMocks private ExamplePersistenceAdapter adapter;

    private final Example domain = Example.builder().id(1L).name("John").dni("123").build();
    private final ExampleEntity entity = mock(ExampleEntity.class);

    @Test
    @DisplayName("save devuelve el dominio persistido")
    void saveOk() {
        when(entityMapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.save(domain)).isEqualTo(domain);
    }

    @Test
    @DisplayName("save envuelve los errores en InfrastructureException")
    void saveError() {
        when(entityMapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> adapter.save(domain)).isInstanceOf(InfrastructureException.class);
    }

    @Test
    @DisplayName("findAll mapea la lista de entidades a dominio")
    void findAllOk() {
        when(jpaRepository.findAll()).thenReturn(List.of(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAll()).containsExactly(domain);
    }

    @Test
    @DisplayName("findAll envuelve los errores en InfrastructureException")
    void findAllError() {
        when(jpaRepository.findAll()).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> adapter.findAll()).isInstanceOf(InfrastructureException.class);
    }

    @Test
    @DisplayName("findAll paginado devuelve una página de dominio")
    void findAllPagedOk() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ExampleEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(jpaRepository.findAll(
                        ArgumentMatchers.<Specification<ExampleEntity>>any(), eq(pageable)))
                .thenReturn(page);
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        Page<Example> result = adapter.findAll(new ExampleFilter(null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(domain);
    }

    @Test
    @DisplayName("findAll paginado envuelve los errores en InfrastructureException")
    void findAllPagedError() {
        Pageable pageable = PageRequest.of(0, 10);
        when(jpaRepository.findAll(
                        ArgumentMatchers.<Specification<ExampleEntity>>any(), eq(pageable)))
                .thenThrow(new RuntimeException());

        assertThatThrownBy(() -> adapter.findAll(new ExampleFilter(null, null, null), pageable))
                .isInstanceOf(InfrastructureException.class);
    }

    @Test
    @DisplayName("findById devuelve el dominio cuando existe")
    void findByIdOk() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(1L)).contains(domain);
    }

    @Test
    @DisplayName("findById envuelve los errores en InfrastructureException")
    void findByIdError() {
        when(jpaRepository.findById(1L)).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> adapter.findById(1L)).isInstanceOf(InfrastructureException.class);
    }

    @Test
    @DisplayName("findByDni devuelve el dominio cuando existe")
    void findByDniOk() {
        when(jpaRepository.findByDni("123")).thenReturn(Optional.of(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByDni("123")).contains(domain);
    }

    @Test
    @DisplayName("findByDni envuelve los errores en InfrastructureException")
    void findByDniError() {
        when(jpaRepository.findByDni("123")).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> adapter.findByDni("123"))
                .isInstanceOf(InfrastructureException.class);
    }

    @Test
    @DisplayName("existsByDni delega en el repositorio")
    void existsByDniOk() {
        when(jpaRepository.existsByDni("123")).thenReturn(true);

        assertThat(adapter.existsByDni("123")).isTrue();
    }

    @Test
    @DisplayName("existsByDni envuelve los errores en InfrastructureException")
    void existsByDniError() {
        when(jpaRepository.existsByDni("123")).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> adapter.existsByDni("123"))
                .isInstanceOf(InfrastructureException.class);
    }
}
