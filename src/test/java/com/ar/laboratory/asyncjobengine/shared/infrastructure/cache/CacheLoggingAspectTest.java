package com.ar.laboratory.asyncjobengine.shared.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.annotation.Cacheable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CacheLoggingAspect Tests")
class CacheLoggingAspectTest {

    private final CacheLoggingAspect aspect = new CacheLoggingAspect();

    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;

    /** Clase de apoyo con un método anotado con @Cacheable para reflexión. */
    static class Sample {
        @Cacheable("examplesCache")
        public String fetch(String key) {
            return key;
        }
    }

    private Method sampleMethod() throws NoSuchMethodException {
        return Sample.class.getMethod("fetch", String.class);
    }

    private void stubSignature() throws NoSuchMethodException {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(sampleMethod());
        when(signature.getDeclaringType()).thenReturn(Sample.class);
        when(signature.getName()).thenReturn("fetch");
    }

    @Test
    @DisplayName("Devuelve el resultado del método interceptado")
    void devuelveResultado() throws Throwable {
        stubSignature();
        when(joinPoint.getArgs()).thenReturn(new Object[] {"abc"});
        when(joinPoint.proceed()).thenReturn("resultado");

        assertThat(aspect.logCacheOperation(joinPoint)).isEqualTo("resultado");
    }

    @Test
    @DisplayName("Propaga la excepción y sanitiza distintos tipos de argumentos")
    void propagaExcepcionYSanitizaArgs() throws Throwable {
        stubSignature();
        when(joinPoint.getArgs()).thenReturn(new Object[] {null, 42, "x".repeat(60), new Object()});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.logCacheOperation(joinPoint))
                .isInstanceOf(IllegalStateException.class);
    }
}
