package com.ar.laboratory.asyncjobengine.shared.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Excepciones de infraestructura compartida")
class SharedExceptionsTest {

    @Test
    @DisplayName("BadRequestException conserva el mensaje")
    void badRequestConMensaje() {
        BadRequestException ex = new BadRequestException("dato inválido");

        assertThat(ex.getMessage()).isEqualTo("dato inválido");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BadRequestException conserva mensaje y causa")
    void badRequestConMensajeYCausa() {
        Throwable cause = new IllegalArgumentException("raíz");
        BadRequestException ex = new BadRequestException("dato inválido", cause);

        assertThat(ex.getMessage()).isEqualTo("dato inválido");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("InfrastructureException conserva el mensaje")
    void infrastructureConMensaje() {
        InfrastructureException ex = new InfrastructureException("fallo de BD");

        assertThat(ex.getMessage()).isEqualTo("fallo de BD");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("InfrastructureException conserva mensaje y causa")
    void infrastructureConMensajeYCausa() {
        Throwable cause = new RuntimeException("raíz");
        InfrastructureException ex = new InfrastructureException("fallo de BD", cause);

        assertThat(ex.getMessage()).isEqualTo("fallo de BD");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
