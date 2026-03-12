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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            description  = "A person record",
            title        = "PersonSchema",
            format       = "object",
            example      = "{}",
            defaultValue = "none",
            nullable     = true,
            readOnly     = true,
            deprecated   = true,
            externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                    url         = "https://example.com",
                    description = "More info"))
    record PersonRecord(
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "Full name",
                    example     = "Alice",
                    format      = "varchar",
                    pattern     = "[A-Za-z]+",
                    minLength   = 1,
                    maxLength   = 100,
                    nullable    = true,
                    deprecated  = true)
            String name,

            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "Age in years",
                    minimum     = "0",
                    maximum     = "150",
                    defaultValue = "18")
            int age,

            @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"ACTIVE", "INACTIVE"})
            String status,

            @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
            String secret,

            @io.swagger.v3.oas.annotations.media.Schema(writeOnly = true)
            String password,

            @io.swagger.v3.oas.annotations.media.Schema(
                    accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY)
            String readField
    ) {}

    /** Without annotations — enricher must be a no-op. */
    record PlainRecord(String value) {}

    // ==========================================================================
    // Helpers
    // ==========================================================================

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Schema<?>> schemasFor(Class<?> clazz) {
        Map<String, Schema<?>> schemas = new LinkedHashMap<>();
        var parent = new Schema<>();
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
    // Class-level: description, title, format, example, defaultValue
    // ==========================================================================

    @Test
    void apply_classSchema_setsDescription() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("A person record", schemas.get("PersonRecord").getDescription());
    }

    @Test
    void apply_classSchema_setsTitle() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("PersonSchema", schemas.get("PersonRecord").getTitle());
    }

    @Test
    void apply_classSchema_setsFormat() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("object", schemas.get("PersonRecord").getFormat());
    }

    @Test
    void apply_classSchema_setsExample() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("{}", schemas.get("PersonRecord").getExample());
    }

    @Test
    void apply_classSchema_setsDefaultValue() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("none", schemas.get("PersonRecord").getDefault());
    }

    // ==========================================================================
    // Class-level: booleans
    // ==========================================================================

    @Test
    void apply_classSchema_setsNullable() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(schemas.get("PersonRecord").getNullable());
    }

    @Test
    void apply_classSchema_setsReadOnly() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(schemas.get("PersonRecord").getReadOnly());
    }

    @Test
    void apply_classSchema_setsDeprecated() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(schemas.get("PersonRecord").getDeprecated());
    }

    // ==========================================================================
    // Class-level: externalDocs
    // ==========================================================================

    @Test
    void apply_classSchema_setsExternalDocs() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        var extDoc = schemas.get("PersonRecord").getExternalDocs();
        assertNotNull(extDoc);
        assertEquals("https://example.com", extDoc.getUrl());
        assertEquals("More info", extDoc.getDescription());
    }

    // ==========================================================================
    // Field-level: description, example, format, pattern
    // ==========================================================================

    @Test
    void apply_fieldSchema_setsDescription() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("Full name", prop(schemas, PersonRecord.class, "name").getDescription());
    }

    @Test
    void apply_fieldSchema_setsExample() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("Alice", prop(schemas, PersonRecord.class, "name").getExample());
    }

    @Test
    void apply_fieldSchema_setsFormat() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("varchar", prop(schemas, PersonRecord.class, "name").getFormat());
    }

    @Test
    void apply_fieldSchema_setsPattern() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("[A-Za-z]+", prop(schemas, PersonRecord.class, "name").getPattern());
    }

    // ==========================================================================
    // Field-level: length constraints
    // ==========================================================================

    @Test
    void apply_fieldSchema_setsMinLength() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals(1, prop(schemas, PersonRecord.class, "name").getMinLength());
    }

    @Test
    void apply_fieldSchema_setsMaxLength() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals(100, prop(schemas, PersonRecord.class, "name").getMaxLength());
    }

    // ==========================================================================
    // Field-level: numeric constraints
    // ==========================================================================

    @Test
    void apply_fieldSchema_setsMinimum() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals(BigDecimal.ZERO, prop(schemas, PersonRecord.class, "age").getMinimum());
    }

    @Test
    void apply_fieldSchema_setsMaximum() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals(new BigDecimal("150"), prop(schemas, PersonRecord.class, "age").getMaximum());
    }

    @Test
    void apply_fieldSchema_setsDefaultValue() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("18", prop(schemas, PersonRecord.class, "age").getDefault());
    }

    // ==========================================================================
    // Field-level: booleans
    // ==========================================================================

    @Test
    void apply_fieldSchema_setsNullable() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(prop(schemas, PersonRecord.class, "name").getNullable());
    }

    @Test
    void apply_fieldSchema_setsDeprecated() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(prop(schemas, PersonRecord.class, "name").getDeprecated());
    }

    @Test
    void apply_fieldSchema_setsWriteOnly() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(prop(schemas, PersonRecord.class, "password").getWriteOnly());
    }

    // ==========================================================================
    // Field-level: accessMode
    // ==========================================================================

    @Test
    void apply_fieldSchema_accessModeReadOnly_setsReadOnly() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertTrue(prop(schemas, PersonRecord.class, "readField").getReadOnly());
    }

    // ==========================================================================
    // Field-level: allowableValues (enum)
    // ==========================================================================

    @Test
    void apply_fieldSchema_setsAllowableValuesAsEnum() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        var enumValues = prop(schemas, PersonRecord.class, "status").getEnum();
        assertNotNull(enumValues);
        assertTrue(enumValues.contains("ACTIVE"));
        assertTrue(enumValues.contains("INACTIVE"));
    }

    // ==========================================================================
    // Field-level: hidden
    // ==========================================================================

    @Test
    void apply_hiddenField_isRemovedFromProperties() {
        var schemas = schemasFor(PersonRecord.class);
        enricher.apply(PersonRecord.class, schemas);
        assertFalse(schemas.get("PersonRecord").getProperties().containsKey("secret"),
                "Hidden field must be removed from schema properties");
    }

    // ==========================================================================
    // Non-overwriting policy
    // ==========================================================================

    @Test
    void apply_existingDescription_isNotOverwritten() {
        var schemas = schemasFor(PersonRecord.class);
        schemas.get("PersonRecord").setDescription("already set");
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("already set", schemas.get("PersonRecord").getDescription());
    }

    @Test
    void apply_existingExample_isNotOverwritten() {
        var schemas = schemasFor(PersonRecord.class);
        schemas.get("PersonRecord").setExample("existing");
        enricher.apply(PersonRecord.class, schemas);
        assertEquals("existing", schemas.get("PersonRecord").getExample());
    }

    // ==========================================================================
    // No annotation — no-op
    // ==========================================================================

    @Test
    void apply_classWithoutSchemaAnnotation_remainsUnchanged() {
        var schemas = schemasFor(PlainRecord.class);
        enricher.apply(PlainRecord.class, schemas);
        var schema = schemas.get("PlainRecord");
        assertNull(schema.getDescription());
        assertNull(schema.getTitle());
        assertNull(schema.getDeprecated());
    }

    // ==========================================================================
    // Guard conditions
    // ==========================================================================

    @Test
    void apply_nullSchemas_doesNotThrow() {
        assertDoesNotThrow(() -> enricher.apply(PersonRecord.class, null));
    }

    @Test
    void apply_emptySchemas_doesNotThrow() {
        assertDoesNotThrow(() -> enricher.apply(PersonRecord.class, new LinkedHashMap<>()));
    }

    @Test
    void apply_nullType_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> enricher.apply(null, schemasFor(PersonRecord.class)));
    }
}
