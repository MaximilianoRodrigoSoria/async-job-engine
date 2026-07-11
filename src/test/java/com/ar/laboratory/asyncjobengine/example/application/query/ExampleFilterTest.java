package com.ar.laboratory.asyncjobengine.example.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExampleFilter — Tests unitarios")
class ExampleFilterTest {

    @Test
    @DisplayName("isEmpty — retorna true cuando todos los campos son null")
    void shouldReturnTrueWhenAllFieldsAreNull() {
        assertThat(new ExampleFilter(null, null, null).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty — retorna true cuando todos los campos son cadenas vacías")
    void shouldReturnTrueWhenAllFieldsAreEmpty() {
        assertThat(new ExampleFilter("", "", "").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty — retorna true cuando todos los campos son solo espacios")
    void shouldReturnTrueWhenAllFieldsAreBlank() {
        assertThat(new ExampleFilter("   ", "  ", " ").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty — retorna false cuando name tiene valor")
    void shouldReturnFalseWhenNameHasValue() {
        assertThat(new ExampleFilter("John", null, null).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty — retorna false cuando dni tiene valor")
    void shouldReturnFalseWhenDniHasValue() {
        assertThat(new ExampleFilter(null, "12345678", null).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty — retorna false cuando search tiene valor")
    void shouldReturnFalseWhenSearchHasValue() {
        assertThat(new ExampleFilter(null, null, "keyword").isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty — retorna false cuando solo uno de varios campos tiene valor")
    void shouldReturnFalseWhenAnyFieldHasValue() {
        assertThat(new ExampleFilter(null, null, "x").isEmpty()).isFalse();
        assertThat(new ExampleFilter("x", null, null).isEmpty()).isFalse();
        assertThat(new ExampleFilter(null, "x", null).isEmpty()).isFalse();
    }
}
