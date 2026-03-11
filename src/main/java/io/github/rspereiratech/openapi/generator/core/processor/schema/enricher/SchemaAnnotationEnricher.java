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

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;

/**
 * Enriches OpenAPI component schemas with metadata from Swagger's
 * {@link io.swagger.v3.oas.annotations.media.Schema} annotation.
 *
 * <p>Swagger's {@link io.swagger.v3.core.converter.ModelConverters} engine does not
 * reliably propagate {@code @Schema} metadata for Java records. This enricher fills
 * that gap by reading the annotation directly from class and field declarations and
 * applying the metadata to the already-resolved schemas — but only when the schema
 * does not already carry a value for that attribute (non-overwriting policy).
 *
 * <p>The following attributes are handled:
 * <ul>
 *   <li><b>Class-level</b>:
 *     <ul>
 *       <li>{@code description} — sets {@code schema.description} if not already set</li>
 *       <li>{@code title} — sets {@code schema.title} if not already set</li>
 *       <li>{@code deprecated} — sets {@code schema.deprecated = true} if not already set</li>
 *     </ul>
 *   </li>
 *   <li><b>Field-level</b> (property schemas):
 *     <ul>
 *       <li>{@code description} — sets {@code property.description} if not already set</li>
 *       <li>{@code example} — sets {@code property.example} if not already set</li>
 *       <li>{@code deprecated} — sets {@code property.deprecated = true} if not already set</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Property names are resolved via {@link SchemaEnricherSupport#resolvePropertyName},
 * which honours {@link com.fasterxml.jackson.annotation.JsonProperty} overrides.
 *
 * <p>Does nothing when {@code schemas} is {@code null} or empty.
 *
 * @author ruispereira
 * @see SchemaEnricher
 * @see SchemaEnricherSupport
 */
public class SchemaAnnotationEnricher implements SchemaEnricher {

    /**
     * Applies {@link io.swagger.v3.oas.annotations.media.Schema} metadata from the fields
     * of all classes reachable from {@code type} to the corresponding schemas.
     *
     * <p>Starting from {@code type}, all user-defined classes reachable through field
     * traversal are collected. For each discovered class, the schema is looked up in
     * {@code schemas} by {@link Class#getSimpleName()} — the same key convention used by
     * Swagger {@code ModelConverters}. Classes with no matching schema entry are silently
     * skipped.
     *
     * <p>Does nothing when {@code schemas} is {@code null} or empty.
     *
     * @param type    the root type to start class traversal from; never {@code null}
     * @param schemas mutable map of schema-name → schema; enricher mutates values in-place
     */
    @Override
    @SuppressWarnings("java:S1452")
    public void apply(Type type, Map<String, Schema<?>> schemas) {
        if (schemas == null || schemas.isEmpty()) return;

        SchemaEnricherSupport.collectReachableClasses(type, new HashSet<>()).forEach(clazz -> {
            if (!(schemas.get(clazz.getSimpleName()) instanceof Schema<?> schema)) return;
            applyClassLevelAnnotation(clazz, schema);
            applyFieldLevelAnnotations(clazz, schema);
        });
    }

    // ------------------------------------------------------------------
    // Class-level annotation
    // ------------------------------------------------------------------

    /**
     * Reads the class-level {@link io.swagger.v3.oas.annotations.media.Schema} annotation
     * from {@code clazz} and applies {@code description}, {@code title}, and {@code deprecated}
     * to {@code schema}, skipping any attribute for which the schema already has a value.
     *
     * @param clazz  the class to inspect; must not be {@code null}
     * @param schema the OpenAPI schema to enrich; must not be {@code null}
     */
    @SuppressWarnings("java:S1452")
    private static void applyClassLevelAnnotation(Class<?> clazz, Schema<?> schema) {
        io.swagger.v3.oas.annotations.media.Schema ann =
                clazz.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (ann == null) return;

        if (!ann.description().isBlank() && schema.getDescription() == null) {
            schema.setDescription(ann.description());
        }
        if (!ann.title().isBlank() && schema.getTitle() == null) {
            schema.setTitle(ann.title());
        }
        if (ann.deprecated() && schema.getDeprecated() == null) {
            schema.setDeprecated(true);
        }
    }

    // ------------------------------------------------------------------
    // Field-level annotations
    // ------------------------------------------------------------------

    /**
     * Iterates over all non-static, non-synthetic fields of {@code clazz} (including
     * inherited fields) and applies {@code description}, {@code example}, and
     * {@code deprecated} from any field-level
     * {@link io.swagger.v3.oas.annotations.media.Schema} annotation to the matching
     * property schema.
     *
     * <p>Property name resolution honours {@link com.fasterxml.jackson.annotation.JsonProperty}.
     * Fields that have no matching property in {@code schema.getProperties()} are silently
     * skipped.
     *
     * @param clazz  the class whose fields are inspected; must not be {@code null}
     * @param schema the OpenAPI schema whose properties may be mutated; must not be {@code null}
     */
    @SuppressWarnings({"unchecked", "java:S1452"})
    private static void applyFieldLevelAnnotations(Class<?> clazz, Schema<?> schema) {
        if (schema.getProperties() == null) return;

        SchemaEnricherSupport.allDeclaredFields(clazz).forEach(field -> {
            io.swagger.v3.oas.annotations.media.Schema ann =
                    field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            if (ann == null) return;

            String propertyName = SchemaEnricherSupport.resolvePropertyName(field);
            if (!(schema.getProperties().get(propertyName) instanceof Schema<?> property)) return;

            if (!ann.description().isBlank() && property.getDescription() == null) {
                property.setDescription(ann.description());
            }
            if (!ann.example().isBlank() && property.getExample() == null) {
                property.setExample(ann.example());
            }
            if (ann.deprecated() && property.getDeprecated() == null) {
                property.setDeprecated(true);
            }
        });
    }
}
