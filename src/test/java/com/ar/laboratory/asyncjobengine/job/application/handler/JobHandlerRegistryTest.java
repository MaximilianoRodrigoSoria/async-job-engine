package com.ar.laboratory.asyncjobengine.job.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ar.laboratory.asyncjobengine.job.domain.exception.JobTypeNotSupportedException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JobHandlerRegistry")
class JobHandlerRegistryTest {

    private JobHandler handler(String type) {
        return new JobHandler() {
            @Override
            public String jobType() {
                return type;
            }

            @Override
            public String handle(String payload) {
                return payload;
            }
        };
    }

    @Test
    @DisplayName("supports reconoce los tipos registrados")
    void supportsRegisteredTypes() {
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of(handler("echo")));
        assertThat(registry.supports("echo")).isTrue();
        assertThat(registry.supports("desconocido")).isFalse();
    }

    @Test
    @DisplayName("handlerFor devuelve el handler o lanza si no existe")
    void handlerForResolvesOrThrows() {
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of(handler("echo")));
        assertThat(registry.handlerFor("echo").jobType()).isEqualTo("echo");
        assertThatThrownBy(() -> registry.handlerFor("nope"))
                .isInstanceOf(JobTypeNotSupportedException.class);
    }
}
