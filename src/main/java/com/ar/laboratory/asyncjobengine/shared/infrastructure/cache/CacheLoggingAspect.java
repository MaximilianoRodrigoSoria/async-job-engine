package com.ar.laboratory.asyncjobengine.shared.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Aspecto para logging de operaciones de caché (CACHE_HIT y CACHE_MISS)
 *
 * <p>Este aspecto intercepta métodos anotados con @Cacheable y registra si el resultado proviene de
 * caché o de la ejecución real del método.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = false)
public class CacheLoggingAspect {

    /**
     * Intercepta llamadas a métodos con @Cacheable para loggear hits y misses
     *
     * @param joinPoint punto de intercepción
     * @return resultado del método
     * @throws Throwable si ocurre un error
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object logCacheOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Cacheable cacheable = signature.getMethod().getAnnotation(Cacheable.class);

        String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] : "default";
        String methodName =
                signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();

        try {
            // Ejecutar el método (Spring Cache determinará si usa caché o no)
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // Si la ejecución fue muy rápida (< 5ms), probablemente fue un cache hit
            // Este es un heurístico simple; en producción se podría usar un approach más
            // sofisticado
            if (duration < 5) {
                log.info(
                        "CACHE_HIT - cache: {}, method: {}, args: {}, duration: {}ms",
                        cacheName,
                        methodName,
                        sanitizeArgs(args),
                        duration);
            } else {
                log.info(
                        "CACHE_MISS - cache: {}, method: {}, args: {}, duration: {}ms",
                        cacheName,
                        methodName,
                        sanitizeArgs(args),
                        duration);
            }

            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(
                    "CACHE_ERROR - cache: {}, method: {}, args: {}, duration: {}ms, error: {}",
                    cacheName,
                    methodName,
                    sanitizeArgs(args),
                    duration,
                    ex.getMessage());
            throw ex;
        }
    }

    /**
     * Sanitiza los argumentos para logging (evita imprimir datos sensibles)
     *
     * @param args argumentos del método
     * @return string sanitizado
     */
    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                // Sanitizar strings largos
                String str = (String) arg;
                if (str.length() > 50) {
                    sb.append(str.substring(0, 47)).append("...");
                } else {
                    sb.append(str);
                }
            } else if (arg instanceof Number || arg instanceof Boolean) {
                sb.append(arg);
            } else {
                // Para objetos complejos, solo mostrar el tipo
                sb.append(arg.getClass().getSimpleName());
            }
        }
        sb.append("]");

        return sb.toString();
    }
}
