package com.ar.laboratory.asyncjobengine.shared.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filtro que captura y loguea todas las peticiones HTTP con sanitización de datos sensibles.
 *
 * <p>Este filtro intercepta requests y responses, extrae sus bodies, aplica sanitización mediante
 * {@link LogSanitizer} y loguea la información de forma segura.
 *
 * <p>Características:
 *
 * <ul>
 *   <li>Captura request y response bodies usando wrappers de Spring
 *   <li>Sanitiza datos sensibles antes de loguear (passwords, tokens, DNIs, etc.)
 *   <li>Omite endpoints de actuator, swagger y otros paths configurados
 *   <li>No loguea archivos binarios ni multipart
 *   <li>Trunca bodies grandes para evitar saturar logs
 *   <li>Mide duración de cada request
 * </ul>
 *
 * <p>Orden de ejecución: Corre después de {@link MdcFilter} (HIGHEST_PRECEDENCE + 1) para que el
 * {@code traceId} ya esté disponible en el MDC al momento de loguear.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    /** Tamaño máximo del body a loguear (10KB) para evitar logs gigantes. */
    private static final int MAX_BODY_SIZE = 10_000;

    /**
     * Prefijos de paths que no se deben loguear para evitar ruido en logs.
     *
     * <p>Típicamente endpoints de monitoreo, documentación y recursos estáticos.
     */
    private static final Set<String> SKIP_PATH_PREFIXES =
            Set.of("/actuator", "/swagger-ui", "/v3/api-docs", "/swagger-resources", "/webjars");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Wrappear request y response para poder leer los bodies múltiples veces
        ContentCachingRequestWrapper wrappedRequest =
                request instanceof ContentCachingRequestWrapper
                        ? (ContentCachingRequestWrapper) request
                        : new ContentCachingRequestWrapper(request, MAX_BODY_SIZE);
        ContentCachingResponseWrapper wrappedResponse =
                response instanceof ContentCachingResponseWrapper
                        ? (ContentCachingResponseWrapper) response
                        : new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Continuar con la cadena de filtros
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // Siempre loguear, incluso si hubo excepción
            logRequestResponse(wrappedRequest, wrappedResponse, startTime);

            // CRÍTICO: copiar el body de vuelta al response
            // Sin esto, el cliente recibiría response vacío
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestResponse(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long startTime) {

        long durationMs = System.currentTimeMillis() - startTime;

        String requestBody = extractBody(request.getContentAsByteArray(), request.getContentType());
        String responseBody =
                extractBody(response.getContentAsByteArray(), response.getContentType());

        // Sanitizar antes de loguear - primera línea de defensa
        requestBody = LogSanitizer.sanitize(requestBody);
        responseBody = LogSanitizer.sanitize(responseBody);

        // traceId ya está en el MDC (puesto por MdcFilter) y aparece en el pattern de Logback.
        // Se incluye también explícitamente en el mensaje para facilitar búsquedas en texto plano.
        String traceId = MDC.get(MdcFilter.MDC_TRACE_ID_KEY);

        log.info(
                "HTTP {} {} | status={} | duration={}ms | traceId={} | timestamp={}\n"
                        + "REQUEST: {}\n"
                        + "RESPONSE: {}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                traceId,
                Instant.now(),
                requestBody.isEmpty() ? "[empty]" : requestBody,
                responseBody.isEmpty() ? "[empty]" : responseBody);
    }

    /**
     * Extrae el body de un request/response como string.
     *
     * <p>Maneja casos especiales:
     *
     * <ul>
     *   <li>Bodies vacíos
     *   <li>Contenido binario (retorna placeholder)
     *   <li>Multipart (retorna placeholder)
     *   <li>Bodies muy grandes (trunca)
     * </ul>
     *
     * @param bytes bytes del body
     * @param contentType content-type del mensaje
     * @return body como string o placeholder si no es apropiado loguearlo
     */
    private String extractBody(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        // Evitar loguear binarios - genera logs ilegibles y pesados
        if (contentType != null) {
            try {
                MediaType mediaType = MediaType.parseMediaType(contentType);

                if (MediaType.APPLICATION_OCTET_STREAM.includes(mediaType)) {
                    return "[binary content - not logged]";
                }

                if (MediaType.MULTIPART_FORM_DATA.includes(mediaType)) {
                    return "[multipart content - not logged]";
                }

                if (mediaType.getType().equals("image") || mediaType.getType().equals("video")) {
                    return "[" + mediaType + " content - not logged]";
                }
            } catch (Exception e) {
                log.debug("Error parsing content type: {}", contentType, e);
            }
        }

        // Truncar bodies grandes para evitar saturar logs
        int lengthToRead = Math.min(bytes.length, MAX_BODY_SIZE);
        String body = new String(bytes, 0, lengthToRead, StandardCharsets.UTF_8);

        if (bytes.length > MAX_BODY_SIZE) {
            body += "\n...[truncated - original size: " + bytes.length + " bytes]";
        }

        return body;
    }
}
