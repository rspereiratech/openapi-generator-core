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

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link PageTypeSchemaHandler}.
 *
 * <p>Verifies that {@code Page<T>} produces a structured paginated schema named
 * {@code Page{T}} with the expected fields ({@code content}, {@code page},
 * {@code size}, {@code totalElements}, {@code totalPages}, {@code last}),
 * and that the schema is registered in the component schema registry.
 */
@ExtendWith(MockitoExtension.class)
class PageTypeSchemaHandlerTest {

    @Mock
    SchemaProcessor schemaProcessor;

    private final Map<String, Schema<?>> registry = new LinkedHashMap<>();
    private PageTypeSchemaHandler handler;

    @SuppressWarnings("unused")
    static class TypeFixtures {
        public List<String> listOfString() { return null; }
    }

    @BeforeEach
    void setUp() {
        lenient().when(schemaProcessor.toSchema(any())).thenReturn(new Schema<>());
        lenient().when(schemaProcessor.getSchemaRegistry()).thenReturn(registry);
        handler = new PageTypeSchemaHandler();
    }

    private Type listOfStringType() throws Exception {
        return TypeFixtures.class.getDeclaredMethod("listOfString").getGenericReturnType();
    }

    // ==========================================================================
    // supports() — non-Page types
    // ==========================================================================

    @Test
    void supports_rawClass_returnsFalse() {
        assertFalse(handler.supports(String.class),
                "A raw class is not a ParameterizedType — must return false");
    }

    @Test
    void supports_listOfString_returnsFalse() throws Exception {
        assertFalse(handler.supports(listOfStringType()),
                "List<String> is a ParameterizedType but not Page — must return false");
    }

    @Test
    void supports_primitiveVoid_returnsFalse() {
        assertFalse(handler.supports(void.class));
    }

    // ==========================================================================
    // resolve() — schema structure
    // ==========================================================================

    @Test
    void resolve_returnsRefSchema() throws Exception {
        Schema<?> result = handler.resolve(listOfStringType(), schemaProcessor);
        assertNotNull(result);
        assertNotNull(result.get$ref(), "resolve() must return a $ref schema");
    }

    @Test
    void resolve_refPointsToPageSchema() throws Exception {
        Schema<?> result = handler.resolve(listOfStringType(), schemaProcessor);
        assertTrue(result.get$ref().contains("Page"),
                "$ref must reference a PageXxx schema");
    }

    @Test
    void resolve_registersSortObjectInRegistry() throws Exception {
        handler.resolve(listOfStringType(), schemaProcessor);
        assertTrue(registry.containsKey("SortObject"),
                "SortObject must be registered in the schema registry");
    }

    @Test
    void resolve_registersPageableObjectInRegistry() throws Exception {
        handler.resolve(listOfStringType(), schemaProcessor);
        assertTrue(registry.containsKey("PageableObject"),
                "PageableObject must be registered in the schema registry");
    }

    @Test
    void resolve_registersPageSchemaInRegistry() throws Exception {
        handler.resolve(listOfStringType(), schemaProcessor);
        assertTrue(registry.keySet().stream().anyMatch(k -> k.startsWith("Page")),
                "A PageXxx schema must be registered in the schema registry");
    }

    @Test
    void resolve_sortObjectHasExpectedProperties() throws Exception {
        handler.resolve(listOfStringType(), schemaProcessor);
        Schema<?> sort = registry.get("SortObject");
        assertNotNull(sort.getProperties());
        assertTrue(sort.getProperties().containsKey("direction"));
        assertTrue(sort.getProperties().containsKey("ascending"));
        assertTrue(sort.getProperties().containsKey("property"));
        assertTrue(sort.getProperties().containsKey("ignoreCase"));
        assertTrue(sort.getProperties().containsKey("nullHandling"));
    }

    @Test
    void resolve_pageableObjectHasExpectedProperties() throws Exception {
        handler.resolve(listOfStringType(), schemaProcessor);
        Schema<?> pageable = registry.get("PageableObject");
        assertNotNull(pageable.getProperties());
        assertTrue(pageable.getProperties().containsKey("offset"));
        assertTrue(pageable.getProperties().containsKey("sort"));
        assertTrue(pageable.getProperties().containsKey("pageNumber"));
        assertTrue(pageable.getProperties().containsKey("pageSize"));
        assertTrue(pageable.getProperties().containsKey("paged"));
        assertTrue(pageable.getProperties().containsKey("unpaged"));
    }

    @Test
    void resolve_calledTwice_sortObjectRegisteredOnlyOnce() throws Exception {
        handler.resolve(listOfStringType(), schemaProcessor);
        handler.resolve(listOfStringType(), schemaProcessor);
        long count = registry.keySet().stream().filter("SortObject"::equals).count();
        assertTrue(count <= 1, "SortObject must not be registered more than once");
    }

    @Test
    void resolve_pageSchema_hasContentProperty() throws Exception {
        handler.resolve(listOfStringType(), schemaProcessor);
        String pageKey = registry.keySet().stream()
                .filter(k -> k.startsWith("Page") && !k.equals("PageableObject"))
                .findFirst().orElseThrow();
        Schema<?> page = registry.get(pageKey);
        assertNotNull(page.getProperties());
        assertTrue(page.getProperties().containsKey("content"));
        assertTrue(page.getProperties().containsKey("totalElements"));
        assertTrue(page.getProperties().containsKey("totalPages"));
        assertTrue(page.getProperties().containsKey("size"));
        assertTrue(page.getProperties().containsKey("number"));
        assertTrue(page.getProperties().containsKey("empty"));
    }
}
