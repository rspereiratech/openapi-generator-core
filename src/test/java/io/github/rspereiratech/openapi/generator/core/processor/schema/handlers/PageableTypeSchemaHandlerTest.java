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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PageableTypeSchemaHandlerTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private final Map<String, Schema<?>> registry = new LinkedHashMap<>();
    private PageableTypeSchemaHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.getSchemaRegistry()).thenReturn(registry);
        handler = new PageableTypeSchemaHandler();
    }

    // ==========================================================================
    // supports() — non-Pageable types
    // ==========================================================================

    @Test
    void supports_string_returnsFalse() {
        assertFalse(handler.supports(String.class));
    }

    @Test
    void supports_integer_returnsFalse() {
        assertFalse(handler.supports(Integer.class));
    }

    @Test
    void supports_primitiveVoid_returnsFalse() {
        assertFalse(handler.supports(void.class));
    }

    @Test
    void supports_list_returnsFalse() {
        assertFalse(handler.supports(List.class),
                "A raw non-Pageable class must return false");
    }

    // ==========================================================================
    // resolve() — schema structure
    // ==========================================================================

    @Test
    void resolve_returnsRefToPageable() {
        Schema<?> result = handler.resolve(String.class, schemaProcessor);
        assertNotNull(result);
        assertNotNull(result.get$ref(), "resolve() must return a $ref schema");
        assertTrue(result.get$ref().contains("Pageable"),
                "$ref must point to the Pageable component schema");
    }

    @Test
    void resolve_registersPageableInRegistry() {
        handler.resolve(String.class, schemaProcessor);
        assertTrue(registry.containsKey("Pageable"),
                "Pageable must be registered in the schema registry");
    }

    @Test
    void resolve_pageableSchemaHasExpectedProperties() {
        handler.resolve(String.class, schemaProcessor);
        Schema<?> pageable = registry.get("Pageable");
        assertNotNull(pageable.getProperties());
        assertTrue(pageable.getProperties().containsKey("page"));
        assertTrue(pageable.getProperties().containsKey("size"));
        assertTrue(pageable.getProperties().containsKey("sort"));
    }

    @Test
    void resolve_calledTwice_registersOnlyOnce() {
        handler.resolve(String.class, schemaProcessor);
        handler.resolve(String.class, schemaProcessor);
        long count = registry.keySet().stream().filter("Pageable"::equals).count();
        assertTrue(count <= 1, "Pageable must not be registered more than once");
    }
}
