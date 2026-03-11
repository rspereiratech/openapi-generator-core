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
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link FluxTypeSchemaHandler}.
 *
 * <p>Verifies that {@code Flux<T>} types are correctly unwrapped to an {@code array}
 * schema whose {@code items} reference the resolved schema for {@code T}, and that
 * non-{@code Flux} types are not handled by this handler.
 */
@ExtendWith(MockitoExtension.class)
class FluxTypeSchemaHandlerTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private FluxTypeSchemaHandler handler;

    @SuppressWarnings("unused")
    static class TypeFixtures {
        public List<String>         listOfString()  { return null; }
        public Map<String, Integer> mapOfStringInt() { return null; }
    }

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.toSchema(any())).thenReturn(new Schema<>());
        handler = new FluxTypeSchemaHandler();
    }

    private Type type(String method) throws Exception {
        return TypeFixtures.class.getDeclaredMethod(method).getGenericReturnType();
    }

    // ==========================================================================
    // supports() — non-Flux types
    // ==========================================================================

    @Test
    void supports_rawClass_returnsFalse() {
        assertFalse(handler.supports(String.class),
                "A raw class is not a ParameterizedType — must return false");
    }

    @Test
    void supports_listOfString_returnsFalse() throws Exception {
        assertFalse(handler.supports(type("listOfString")),
                "List<String> is a ParameterizedType but not Flux — must return false");
    }

    @Test
    void supports_mapOfStringInt_returnsFalse() throws Exception {
        assertFalse(handler.supports(type("mapOfStringInt")),
                "Map<String,Integer> must return false");
    }

    @Test
    void supports_primitiveVoid_returnsFalse() {
        assertFalse(handler.supports(void.class));
    }

    // ==========================================================================
    // resolve() — structural assertions
    // ==========================================================================

    @Test
    void resolve_returnsArraySchema() throws Exception {
        // resolve() extracts the first type argument and wraps it — the raw type
        // name is irrelevant inside resolve(), so List<String> drives the test.
        Schema<?> result = handler.resolve(type("listOfString"), schemaProcessor);
        assertInstanceOf(ArraySchema.class, result, "resolve() must return an ArraySchema");
    }

    @Test
    void resolve_arraySchemaHasItems() throws Exception {
        Schema<?> result = handler.resolve(type("listOfString"), schemaProcessor);
        assertNotNull(((ArraySchema) result).getItems(), "ArraySchema must have an items schema");
    }

    @Test
    void resolve_delegatesToSchemaProcessorForItemType() throws Exception {
        Schema<?> itemSchema = new Schema<>().type("string");
        org.mockito.Mockito.doReturn(itemSchema).when(schemaProcessor).toSchema(any());

        Schema<?> result = handler.resolve(type("listOfString"), schemaProcessor);
        assertTrue(((ArraySchema) result).getItems() == itemSchema,
                "items schema must be the one returned by schemaProcessor.toSchema()");
    }
}
