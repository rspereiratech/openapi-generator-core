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
package io.github.rspereiratech.openapi.generator.core.processor.schema.enricher;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SchemaAnnotationEnricher}.
 *
 * <p>Verifies that {@link io.swagger.v3.oas.annotations.media.Schema} annotations
 * declared at class and field level are correctly propagated to the corresponding
 * OpenAPI schema objects, and that the enricher does not overwrite already-set values.
 */
class SchemaAnnotationEnricherTest {

    private final SchemaAnnotationEnricher enricher = new SchemaAnnotationEnricher();

    // ==========================================================================
    // Fixtures
    // ==========================================================================

    @io.swagger.v3.oas.annotations.media.Schema(
            description = "A person record",
            title = "PersonSchema",
            deprecated = true)
    record PersonRecord(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Full name", example = "Alice") String name,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Age in years") int age
    ) {}

    /** Without annotations — enricher must be a no-op. */
    record PlainRecord(String value) {}

    // ==========================================================================
    // Helpers
    // ==========================================================================

    /**
     * Builds a schema map with one entry for the class, containing empty property schemas
     * for each declared field.
     */
    @SuppressWarnings("rawtypes")
    private static Map<String, Schema<?>> schemasFor(Class<?> clazz) {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        Schema<?> parent = new Schema<>();
        Map<String, Schema> props = new LinkedHashMap<>();
        for (var field : clazz.getDeclaredFields()) {
            props.put(field.getName(), new Schema<>());
        }
        parent.setProperties(new LinkedHashMap<>(props));
        schemas.put(clazz.getSimpleName(), parent);
        return schemas;
    }

    @SuppressWarnings("unchecked")
    private static Schema<?> prop(Map<String, Schema<?>> schemas, Class<?> clazz, String field) {
        return (Schema<?>) schemas.get(clazz.getSimpleName()).getProperties().get(field);
    }

    // ==========================================================================
    // Class-level: description
    // ==========================================================================

    @Test
    void classLevelDescription_isApplied() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("A person record", schemas.get("PersonRecord").getDescription());
    }

    // ==========================================================================
    // Class-level: title
    // ==========================================================================

    @Test
    void classLevelTitle_isApplied() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("PersonSchema", schemas.get("PersonRecord").getTitle());
    }

    // ==========================================================================
    // Class-level: deprecated
    // ==========================================================================

    @Test
    void classLevelDeprecated_isApplied() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(schemas.get("PersonRecord").getDeprecated(),
                "deprecated=true on @Schema must set schema.deprecated = true");
    }

    // ==========================================================================
    // Field-level: description
    // ==========================================================================

    @Test
    void fieldLevelDescription_isApplied() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("Full name", prop(schemas, PersonRecord.class, "name").getDescription());
    }

    // ==========================================================================
    // Field-level: example
    // ==========================================================================

    @Test
    void fieldLevelExample_isApplied() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("Alice", prop(schemas, PersonRecord.class, "name").getExample());
    }

    // ==========================================================================
    // Non-overwriting policy
    // ==========================================================================

    @Test
    void existingDescription_isNotOverwritten() {
        var schemas = schemasFor(PersonRecord.class);
        schemas.get("PersonRecord").setDescription("already set");
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("already set", schemas.get("PersonRecord").getDescription(),
                "Enricher must not overwrite an already-set description");
    }

    // ==========================================================================
    // No annotation — no-op
    // ==========================================================================

    @Test
    void noAnnotation_schemasUnchanged() {
        var schemas = schemasFor(PlainRecord.class);
        enricher.apply(PlainRecord.class, schemas);
        Schema<?> schema = schemas.get("PlainRecord");
        assertNull(schema.getDescription(), "No @Schema on PlainRecord — description must stay null");
        assertNull(schema.getTitle(),       "No @Schema on PlainRecord — title must stay null");
        assertNull(schema.getDeprecated(),  "No @Schema on PlainRecord — deprecated must stay null");
    }

    // ==========================================================================
    // Null / empty schemas — no-op
    // ==========================================================================

    @Test
    void nullSchemas_doesNotThrow() {
        assertDoesNotThrow(() -> enricher.apply(PersonRecord.class, null));
    }

    @Test
    void emptySchemas_doesNotThrow() {
        assertDoesNotThrow(() -> enricher.apply(PersonRecord.class, new LinkedHashMap<>()));
    }
}
