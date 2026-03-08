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
package io.github.rspereiratech.openapi.generator.core.processor.schema.handlers;

import io.github.rspereiratech.openapi.generator.core.processor.schema.SchemaProcessor;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ModelConvertersTypeSchemaHandlerTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private final Map<String, Schema<?>> registry = new LinkedHashMap<>();
    private ModelConvertersTypeSchemaHandler handler;

    record SimpleDto(String name, int value) {}
    record NestedDto(String label, SimpleDto inner) {}

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.getSchemaRegistry()).thenReturn(registry);
        handler = new ModelConvertersTypeSchemaHandler();
    }

    // ==========================================================================
    // supports() — catch-all
    // ==========================================================================

    @Test
    void supports_string_returnsTrue() {
        assertTrue(handler.supports(String.class));
    }

    @Test
    void supports_integer_returnsTrue() {
        assertTrue(handler.supports(Integer.class));
    }

    @Test
    void supports_void_returnsTrue() {
        assertTrue(handler.supports(void.class));
    }

    @Test
    void supports_complexType_returnsTrue() {
        assertTrue(handler.supports(SimpleDto.class));
    }

    // ==========================================================================
    // resolve() — scalar types
    // ==========================================================================

    @Test
    void resolve_string_returnsNonNullSchema() {
        Schema<?> result = handler.resolve(String.class, schemaProcessor);
        assertNotNull(result);
    }

    @Test
    void resolve_integer_returnsNonNullSchema() {
        Schema<?> result = handler.resolve(Integer.class, schemaProcessor);
        assertNotNull(result);
    }

    // ==========================================================================
    // resolve() — complex types register referenced schemas
    // ==========================================================================

    @Test
    void resolve_complexType_returnsNonNullSchema() {
        Schema<?> result = handler.resolve(SimpleDto.class, schemaProcessor);
        assertNotNull(result);
    }

    @Test
    void resolve_complexType_registersSchemaInRegistry() {
        handler.resolve(SimpleDto.class, schemaProcessor);
        assertTrue(registry.containsKey("SimpleDto"),
                "ModelConverters must register SimpleDto in the schema registry");
    }

    @Test
    void resolve_nestedComplexType_registersAllReferencedSchemas() {
        handler.resolve(NestedDto.class, schemaProcessor);
        assertTrue(registry.containsKey("NestedDto"),
                "NestedDto must be registered");
        assertTrue(registry.containsKey("SimpleDto"),
                "Nested SimpleDto must also be registered as a referenced schema");
    }

    @Test
    void resolve_sameTypeTwice_doesNotDuplicateRegistration() {
        handler.resolve(SimpleDto.class, schemaProcessor);
        int sizeAfterFirst = registry.size();
        handler.resolve(SimpleDto.class, schemaProcessor);
        assertTrue(registry.size() <= sizeAfterFirst + 1,
                "Re-resolving the same type must not grow the registry unboundedly");
    }
}
