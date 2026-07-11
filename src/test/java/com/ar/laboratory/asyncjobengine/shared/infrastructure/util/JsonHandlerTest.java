package com.ar.laboratory.asyncjobengine.shared.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.JsonParsingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JsonHandler — Tests unitarios")
class JsonHandlerTest {

    private JsonHandler jsonHandler;

    @BeforeEach
    void setUp() {
        jsonHandler = new JsonHandler();
    }

    /** DTO auxiliar para los tests — POJO sin Lombok (Lombok es compileOnly en main). */
    static class SampleDto {
        private Long id;
        private String name;
        private LocalDateTime createdAt;

        SampleDto() {}

        SampleDto(Long id, String name, LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    // =========================================================================
    // toJson
    // =========================================================================

    @Nested
    @DisplayName("toJson()")
    class ToJson {

        @Test
        @DisplayName("Debe serializar un objeto a JSON correctamente")
        void shouldSerializeObject() {
            SampleDto dto = new SampleDto(1L, "Test", null);
            String json = jsonHandler.toJson(dto);
            assertThat(json).contains("\"id\":1", "\"name\":\"Test\"");
        }

        @Test
        @DisplayName("Debe retornar 'null' cuando el objeto es null")
        void shouldReturnNullStringForNullObject() {
            assertThat(jsonHandler.toJson(null)).isEqualTo("null");
        }

        @Test
        @DisplayName("Debe serializar LocalDateTime como ISO-8601 (no timestamp numérico)")
        void shouldSerializeDateAsIso() {
            SampleDto dto = new SampleDto(1L, "Test", LocalDateTime.of(2024, 1, 15, 10, 30));
            String json = jsonHandler.toJson(dto);
            assertThat(json).contains("2024-01-15");
            assertThat(json).doesNotContain("1705");
        }
    }

    // =========================================================================
    // toPrettyJson
    // =========================================================================

    @Nested
    @DisplayName("toPrettyJson()")
    class ToPrettyJson {

        @Test
        @DisplayName("Debe serializar con indentación")
        void shouldProducePrettyJson() {
            SampleDto dto = new SampleDto(1L, "Test", null);
            String pretty = jsonHandler.toPrettyJson(dto);
            assertThat(pretty).contains("\n");
        }

        @Test
        @DisplayName("Debe retornar 'null' para objeto null")
        void shouldReturnNullStringForNull() {
            assertThat(jsonHandler.toPrettyJson(null)).isEqualTo("null");
        }
    }

    // =========================================================================
    // fromJson(String, Class)
    // =========================================================================

    @Nested
    @DisplayName("fromJson(String, Class)")
    class FromJsonClass {

        @Test
        @DisplayName("Debe deserializar JSON al tipo indicado")
        void shouldDeserializeToType() {
            String json = "{\"id\":1,\"name\":\"Test\",\"createdAt\":null}";
            SampleDto dto = jsonHandler.fromJson(json, SampleDto.class);
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("Debe ignorar campos desconocidos sin lanzar excepción")
        void shouldIgnoreUnknownFields() {
            String json = "{\"id\":1,\"name\":\"Test\",\"unknownField\":\"value\"}";
            SampleDto dto = jsonHandler.fromJson(json, SampleDto.class);
            assertThat(dto.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Debe lanzar JsonParsingException si el JSON es null")
        void shouldThrowWhenJsonIsNull() {
            assertThatThrownBy(() -> jsonHandler.fromJson(null, SampleDto.class))
                    .isInstanceOf(JsonParsingException.class);
        }

        @Test
        @DisplayName("Debe lanzar JsonParsingException si el JSON está vacío")
        void shouldThrowWhenJsonIsBlank() {
            assertThatThrownBy(() -> jsonHandler.fromJson("  ", SampleDto.class))
                    .isInstanceOf(JsonParsingException.class);
        }

        @Test
        @DisplayName("Debe lanzar JsonParsingException si el JSON es inválido")
        void shouldThrowWhenJsonIsMalformed() {
            assertThatThrownBy(() -> jsonHandler.fromJson("{invalid}", SampleDto.class))
                    .isInstanceOf(JsonParsingException.class);
        }
    }

    // =========================================================================
    // fromJson(String, TypeReference)
    // =========================================================================

    @Nested
    @DisplayName("fromJson(String, TypeReference)")
    class FromJsonTypeReference {

        @Test
        @DisplayName("Debe deserializar a tipo genérico como List<SampleDto>")
        void shouldDeserializeToGenericType() {
            String json = "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"}]";
            List<SampleDto> list = jsonHandler.fromJson(json, new TypeReference<>() {});
            assertThat(list).hasSize(2);
            assertThat(list.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Debe deserializar a Map<String, Object>")
        void shouldDeserializeToMap() {
            String json = "{\"key\":\"value\",\"number\":42}";
            Map<String, Object> map = jsonHandler.fromJson(json, new TypeReference<>() {});
            assertThat(map).containsEntry("key", "value");
        }

        @Test
        @DisplayName("Debe lanzar JsonParsingException si el JSON es null")
        void shouldThrowWhenJsonIsNull() {
            assertThatThrownBy(
                            () ->
                                    jsonHandler.fromJson(
                                            (String) null, new TypeReference<SampleDto>() {}))
                    .isInstanceOf(JsonParsingException.class);
        }
    }

    // =========================================================================
    // toJsonNode
    // =========================================================================

    @Nested
    @DisplayName("toJsonNode()")
    class ToJsonNode {

        @Test
        @DisplayName("Debe convertir objeto a JsonNode")
        void shouldConvertObjectToJsonNode() {
            SampleDto dto = new SampleDto(1L, "Test", null);
            JsonNode node = jsonHandler.toJsonNode(dto);
            assertThat(node.get("id").asLong()).isEqualTo(1L);
            assertThat(node.get("name").asText()).isEqualTo("Test");
        }

        @Test
        @DisplayName("Debe parsear cadena JSON a JsonNode")
        void shouldParseStringToJsonNode() {
            String json = "{\"id\":1,\"name\":\"Test\"}";
            JsonNode node = jsonHandler.toJsonNode(json);
            assertThat(node.get("name").asText()).isEqualTo("Test");
        }

        @Test
        @DisplayName("Debe lanzar JsonParsingException si la cadena JSON es inválida")
        void shouldThrowForInvalidJson() {
            assertThatThrownBy(() -> jsonHandler.toJsonNode("{not valid"))
                    .isInstanceOf(JsonParsingException.class);
        }
    }

    // =========================================================================
    // fromObject
    // =========================================================================

    @Nested
    @DisplayName("fromObject()")
    class FromObject {

        @Test
        @DisplayName("Debe convertir Map a DTO")
        void shouldConvertMapToDto() {
            Map<String, Object> map = Map.of("id", 1, "name", "Test");
            SampleDto dto = jsonHandler.fromObject(map, SampleDto.class);
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("Debe retornar null cuando el objeto es null")
        void shouldReturnNullForNullInput() {
            assertThat(jsonHandler.fromObject(null, SampleDto.class)).isNull();
        }
    }

    // =========================================================================
    // createObjectNode / createArrayNode
    // =========================================================================

    @Nested
    @DisplayName("Nodos de construcción")
    class NodeBuilders {

        @Test
        @DisplayName("createObjectNode() debe retornar ObjectNode vacío")
        void shouldCreateEmptyObjectNode() {
            ObjectNode node = jsonHandler.createObjectNode();
            assertThat(node).isNotNull();
            assertThat(node.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("createArrayNode() debe retornar ArrayNode vacío")
        void shouldCreateEmptyArrayNode() {
            ArrayNode node = jsonHandler.createArrayNode();
            assertThat(node).isNotNull();
            assertThat(node.isEmpty()).isTrue();
        }
    }

    // =========================================================================
    // getInstance (singleton estático)
    // =========================================================================

    @Test
    @DisplayName("getInstance() debe retornar siempre la misma instancia")
    void shouldReturnSameInstanceEveryTime() {
        JsonHandler a = JsonHandler.getInstance();
        JsonHandler b = JsonHandler.getInstance();
        assertThat(a).isSameAs(b);
    }
}
