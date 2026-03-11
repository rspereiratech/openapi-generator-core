/*
 *   ___                   _   ___ ___
 *  / _ \ _ __  ___ _ _   /_\ | _ \_ _|
 * | (_) | '_ \/ -_) ' \ / _ \|  _/| |
 *  \___/| .__/\___|_||_/_/ \_\_| |___|   Generator
 *       |_|
 *
 * MIT License - Copyright (c) 2026 Rui Pereira
 * See LICENSE in the project root for full license information.
 */
package io.github.rspereiratech.openapi.generator.core.processor.schema;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for {@link SchemaProcessorImpl}.
 *
 * <p>Covers scalar type mapping, generic types ({@code List}, {@code Map},
 * {@code ResponseEntity}, {@code Optional}), void handling, and complex DTO
 * resolution via the chain-of-responsibility handler chain.
 */
class SchemaProcessorTest {

    private SchemaProcessor processor;

    record SimpleDto(String name, int value) {}

    record NestedDto(String label, SimpleDto inner) {}

    @SuppressWarnings("unused")
    static class TypeFixtures {
        public List<String>           stringList()     { return null; }
        public Map<String, Integer>   stringMap()      { return null; }
        public ResponseEntity<String> responseEntity() { return null; }
        public Optional<String>       optional()       { return null; }
        public void                   voidReturn()     {}
    }

    @BeforeEach
    void setUp() {
        processor = new SchemaProcessorImpl();
    }

    // ==========================================================================
    // Scalar types
    // ==========================================================================

    @Test
    void toSchema_string_returnsStringType() {
        assertEquals("string", processor.toSchema(String.class).getType());
    }

    @Test
    void toSchema_integer_returnsIntegerType() {
        assertEquals("integer", processor.toSchema(Integer.class).getType());
    }

    @Test
    void toSchema_long_returnsInt64Format() {
        Schema<?> schema = processor.toSchema(Long.class);
        assertEquals("integer", schema.getType());
        assertEquals("int64", schema.getFormat());
    }

    @Test
    void toSchema_boolean_returnsBooleanType() {
        assertEquals("boolean", processor.toSchema(Boolean.class).getType());
    }

    @Test
    void toSchema_double_returnsNumberType() {
        assertEquals("number", processor.toSchema(Double.class).getType());
    }

    // ==========================================================================
    // Void
    // ==========================================================================

    @Test
    void toSchema_primitiveVoid_returnsNull() {
        assertNull(processor.toSchema(void.class));
    }

    @Test
    void toSchema_boxedVoid_returnsNull() {
        assertNull(processor.toSchema(Void.class));
    }

    // ==========================================================================
    // Wrapper-type unwrapping
    // ==========================================================================

    @Test
    void toSchema_responseEntityOfString_unwrapsToString() throws Exception {
        Type type = TypeFixtures.class.getMethod("responseEntity").getGenericReturnType();
        Schema<?> schema = processor.toSchema(type);
        assertNotNull(schema);
        assertEquals("string", schema.getType());
    }

    @Test
    void toSchema_optionalOfString_unwrapsToString() throws Exception {
        Type type = TypeFixtures.class.getMethod("optional").getGenericReturnType();
        Schema<?> schema = processor.toSchema(type);
        assertNotNull(schema);
        assertEquals("string", schema.getType());
    }

    // ==========================================================================
    // Collection types
    // ==========================================================================

    @Test
    void toSchema_listOfString_returnsArraySchemaWithItems() throws Exception {
        Type type = TypeFixtures.class.getMethod("stringList").getGenericReturnType();
        Schema<?> schema = processor.toSchema(type);
        assertNotNull(schema);
        assertNotNull(schema.getItems(), "Array schema must carry an items schema");
    }

    // ==========================================================================
    // Complex / DTO types
    // ==========================================================================

    @Test
    void toSchema_complexType_returnsRefSchema() {
        Schema<?> schema = processor.toSchema(SimpleDto.class);
        assertNotNull(schema);
        assertNotNull(schema.get$ref(), "Complex type should be returned as a $ref schema");
    }

    @Test
    void toSchema_complexType_registersSchemaInRegistry() {
        processor.toSchema(SimpleDto.class);
        assertTrue(processor.getSchemaRegistry().containsKey("SimpleDto"),
                "SimpleDto must be registered in the schema registry after resolution");
    }

    @Test
    void toSchema_primitiveType_doesNotPollutRegistry() {
        processor.toSchema(String.class);
        assertTrue(processor.getSchemaRegistry().isEmpty(),
                "Scalar types must not add entries to the schema registry");
    }

    @Test
    void toSchema_sameComplexTypeTwice_registeredOnlyOnce() {
        processor.toSchema(SimpleDto.class);
        int sizeAfterFirst = processor.getSchemaRegistry().size();
        processor.toSchema(SimpleDto.class);
        assertEquals(sizeAfterFirst, processor.getSchemaRegistry().size(),
                "Re-resolving the same type must not create duplicate registry entries");
    }

    // ==========================================================================
    // Map types
    // ==========================================================================

    @Test
    void toSchema_mapOfStringInt_returnsNonNullSchema() throws Exception {
        Type type = TypeFixtures.class.getMethod("stringMap").getGenericReturnType();
        Schema<?> schema = processor.toSchema(type);
        assertNotNull(schema);
    }

    // ==========================================================================
    // Additional scalar types
    // ==========================================================================

    @Test
    void toSchema_bigDecimal_returnsNumberType() {
        Schema<?> schema = processor.toSchema(BigDecimal.class);
        assertNotNull(schema);
        assertEquals("number", schema.getType());
    }

    @Test
    void toSchema_uuid_returnsStringType() {
        Schema<?> schema = processor.toSchema(UUID.class);
        assertNotNull(schema);
        assertEquals("string", schema.getType());
    }

    // ==========================================================================
    // Nested DTO schema registration
    // ==========================================================================

    @Test
    void toSchema_nestedDto_registersAllNestedSchemas() {
        processor.toSchema(NestedDto.class);
        // Both NestedDto and SimpleDto (its field type) must be registered
        assertTrue(processor.getSchemaRegistry().containsKey("NestedDto"),
                "NestedDto must be registered");
        assertTrue(processor.getSchemaRegistry().containsKey("SimpleDto"),
                "Nested SimpleDto must be registered as a referenced schema");
    }

    // ==========================================================================
    // Schema registry
    // ==========================================================================

    @Test
    void getSchemaRegistry_initiallyEmpty() {
        assertTrue(processor.getSchemaRegistry().isEmpty());
    }

    @Test
    void getSchemaRegistry_afterScalarResolution_remainsEmpty() {
        processor.toSchema(String.class);
        processor.toSchema(Integer.class);
        processor.toSchema(Boolean.class);
        assertTrue(processor.getSchemaRegistry().isEmpty(),
                "Registry must stay empty after resolving only scalar types");
    }

    // ==========================================================================
    // SchemaProcessor interface — default getSchemaRegistry()
    // ==========================================================================

    @Test
    void schemaProcessor_defaultGetSchemaRegistry_returnsEmptyMap() {
        // Anonymous implementation that only implements toSchema() — exercises the default method
        SchemaProcessor minimalImpl = type -> null;
        assertTrue(minimalImpl.getSchemaRegistry().isEmpty(),
                "Default getSchemaRegistry() must return an empty map");
    }

    // ==========================================================================
    // Constructor preconditions
    // ==========================================================================

    @Test
    void constructor_nullHandlers_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new SchemaProcessorImpl(null));
    }

    @Test
    void constructor_emptyHandlers_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SchemaProcessorImpl(List.of()));
    }
}
