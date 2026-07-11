package com.ar.laboratory.asyncjobengine.shared.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que gestiona el Correlation ID para trazabilidad distribuida.
 *
 * <p>Por cada request HTTP:
 *
 * <ol>
 *   <li>Lee el header {@code X-Correlation-ID} si viene del cliente o gateway.
 *   <li>Si no existe, genera un UUID nuevo.
 *   <li>Registra el valor en el {@link MDC} bajo la clave {@code traceId}, haciendo que aparezca
 *       automáticamente en todos los logs del hilo.
 *   <li>Propaga el Correlation ID en el response header {@code X-Correlation-ID}.
 *   <li>Limpia el MDC en el bloque {@code finally} para evitar leaks entre requests en el pool de
 *       hilos.
 * </ol>
 *
 * <p>Este filtro corre con {@link Ordered#HIGHEST_PRECEDENCE} para que el {@code traceId} esté
 * disponible en el MDC antes que cualquier otro componente (incluido {@link LoggingFilter}).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_TRACE_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }
}
