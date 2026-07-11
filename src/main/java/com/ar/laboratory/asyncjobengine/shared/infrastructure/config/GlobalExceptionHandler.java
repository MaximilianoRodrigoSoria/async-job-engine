package com.ar.laboratory.asyncjobengine.shared.infrastructure.config;

import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleAlreadyExistsException;
import com.ar.laboratory.asyncjobengine.example.domain.exception.ExampleNotFoundException;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.BadRequestException;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.InfrastructureException;
import com.ar.laboratory.asyncjobengine.job.domain.exception.InvalidJobTransitionException;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobNotFoundException;
import com.ar.laboratory.asyncjobengine.job.domain.exception.JobTypeNotSupportedException;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.logging.MdcFilter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/** Manejador global de excepciones */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExampleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleExampleNotFoundException(
            ExampleNotFoundException ex, WebRequest request) {

        log.error("ExampleNotFoundException: {}", ex.getMessage());

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ExampleAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleExampleAlreadyExistsException(
            ExampleAlreadyExistsException ex, WebRequest request) {

        log.error("ExampleAlreadyExistsException: {}", ex.getMessage());

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error(HttpStatus.CONFLICT.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, WebRequest request) {

        log.error("BadRequestException: {}", ex.getMessage());

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(
            JobNotFoundException ex, WebRequest request) {
        log.warn("JobNotFoundException: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(JobTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleJobTypeNotSupported(
            JobTypeNotSupportedException ex, WebRequest request) {
        log.warn("JobTypeNotSupportedException: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidJobTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJobTransition(
            InvalidJobTransitionException ex, WebRequest request) {
        log.warn("InvalidJobTransitionException: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.error("MethodArgumentNotValidException: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(
                        (error) -> {
                            String fieldName = ((FieldError) error).getField();
                            String errorMessage = error.getDefaultMessage();
                            errors.put(fieldName, errorMessage);
                        });

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message("Error de validación")
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .validationErrors(errors)
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRequestNotPermitted(
            RequestNotPermitted ex, WebRequest request) {

        log.warn("Rate limit superado: {}", ex.getMessage());

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                        .message(
                                "Demasiadas solicitudes. Por favor, intentá de nuevo en un"
                                        + " momento.")
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ErrorResponse> handleInfrastructureException(
            InfrastructureException ex, WebRequest request) {

        log.error("InfrastructureException: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .message("Error interno del servidor")
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {

        log.error("Exception no controlada: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .message("Error interno del servidor")
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /** Construye una respuesta de error estándar para un status y mensaje dados. */
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String message, WebRequest request) {
        ErrorResponse errorResponse =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .path(getPath(request))
                        .traceId(generateTraceId())
                        .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    /**
     * Obtiene el traceId del MDC (inyectado por {@link MdcFilter}). Si por alguna razón no está
     * disponible (e.g. llamada fuera de un request HTTP), genera un UUID como fallback.
     */
    private String generateTraceId() {
        String traceId = MDC.get(MdcFilter.MDC_TRACE_ID_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
