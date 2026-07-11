package com.ar.laboratory.asyncjobengine.shared.infrastructure.util;

import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.JsonParsingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utilidad centralizada para serialización y deserialización JSON.
 *
 * <h3>Uso como bean de Spring (preferido)</h3>
 *
 * <pre>{@code
 * @Autowired
 * private JsonHandler jsonHandler;
 * }</pre>
 *
 * <h3>Uso estático (fuera del contexto de Spring)</h3>
 *
 * <pre>{@code
 * JsonHandler.getInstance().toJson(myObject);
 * }</pre>
 *
 * <p>El {@link ObjectMapper} está configurado con:
 *
 * <ul>
 *   <li>{@code JavaTimeModule} — soporte para {@code LocalDate}, {@code LocalDateTime}, etc.
 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS = false} — fechas como cadenas ISO-8601.
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false} — tolerante a campos desconocidos en el JSON
 *       entrante.
 *   <li>{@code FAIL_ON_EMPTY_BEANS = false} — evita excepciones con objetos sin propiedades.
 * </ul>
 *
 * <p>{@link ObjectMapper} es thread-safe una vez configurado; la instancia se puede compartir sin
 * sincronización adicional.
 */
@Slf4j
@Component
public class JsonHandler {

    private final ObjectMapper objectMapper;

    /** Constructor de Spring — el contenedor inyecta esta instancia donde sea necesario. */
    public JsonHandler() {
        this.objectMapper = buildObjectMapper();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Acceso estático (para contextos sin DI)
    // ──────────────────────────────────────────────────────────────────────────

    private static final class Holder {
        private static final JsonHandler INSTANCE = new JsonHandler();
    }

    /**
     * Retorna la instancia singleton para uso fuera del contexto de Spring.
     *
     * <p>Dentro de Spring, preferir la inyección de dependencias.
     */
    public static JsonHandler getInstance() {
        return Holder.INSTANCE;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Serialización → JSON
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Serializa un objeto a una cadena JSON.
     *
     * @throws JsonParsingException si el objeto no puede serializarse.
     */
    public <T> String toJson(T object) {
        if (object == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error(
                    "Error serializando objeto de tipo {}: {}",
                    object.getClass().getName(),
                    e.getMessage());
            throw new JsonParsingException(
                    "No se pudo serializar el objeto de tipo " + object.getClass().getName(), e);
        }
    }

    /**
     * Serializa un objeto a JSON con formato indentado (legible por humanos).
     *
     * @throws JsonParsingException si el objeto no puede serializarse.
     */
    public <T> String toPrettyJson(T object) {
        if (object == null) {
            return "null";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializando objeto a pretty JSON: {}", e.getMessage());
            throw new JsonParsingException("No se pudo serializar el objeto a pretty JSON", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Deserialización ← JSON
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Deserializa una cadena JSON al tipo indicado.
     *
     * @throws JsonParsingException si el JSON no puede deserializarse al tipo dado.
     */
    public <T> T fromJson(String json, Class<T> type) {
        validateJson(json);
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("Error deserializando JSON a {}: {}", type.getName(), e.getMessage());
            throw new JsonParsingException(
                    "No se pudo deserializar el JSON al tipo " + type.getName(), e);
        }
    }

    /**
     * Deserializa una cadena JSON a un tipo genérico (ej. {@code List<MyDto>}).
     *
     * <pre>{@code
     * List<MyDto> list = jsonHandler.fromJson(json, new TypeReference<List<MyDto>>() {});
     * }</pre>
     *
     * @throws JsonParsingException si el JSON no puede deserializarse.
     */
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        validateJson(json);
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error(
                    "Error deserializando JSON a {}: {}",
                    typeReference.getType().getTypeName(),
                    e.getMessage());
            throw new JsonParsingException(
                    "No se pudo deserializar el JSON al tipo "
                            + typeReference.getType().getTypeName(),
                    e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Conversión entre tipos
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Convierte un objeto a {@link JsonNode}.
     *
     * @throws JsonParsingException si la conversión falla.
     */
    public <T> JsonNode toJsonNode(T object) {
        return toJsonNode(toJson(object));
    }

    /**
     * Parsea una cadena JSON como {@link JsonNode}.
     *
     * @throws JsonParsingException si el JSON es inválido.
     */
    public JsonNode toJsonNode(String json) {
        validateJson(json);
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Error mapeando JSON a JsonNode: {}", e.getMessage());
            throw new JsonParsingException("No se pudo mapear el JSON a JsonNode", e);
        }
    }

    /**
     * Convierte un objeto a otro tipo usando el {@link ObjectMapper} como intermediario. Útil para
     * convertir {@code Map<String, Object>} a un DTO y viceversa.
     *
     * @throws JsonParsingException si la conversión falla.
     */
    public <T> T fromObject(Object object, Class<T> type) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(object, type);
        } catch (IllegalArgumentException e) {
            log.error("Error convirtiendo objeto a {}: {}", type.getName(), e.getMessage());
            throw new JsonParsingException(
                    "No se pudo convertir el objeto al tipo " + type.getName(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Nodos de construcción
    // ──────────────────────────────────────────────────────────────────────────

    /** Retorna un {@link ObjectNode} vacío para construcción dinámica de JSON. */
    public ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    /** Retorna un {@link ArrayNode} vacío para construcción dinámica de JSON. */
    public ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Interno
    // ──────────────────────────────────────────────────────────────────────────

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    private static void validateJson(String json) {
        if (json == null || json.isBlank()) {
            throw new JsonParsingException("El JSON proporcionado no puede ser nulo o vacío");
        }
    }
}
