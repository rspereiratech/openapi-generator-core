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

import com.google.common.base.Preconditions;
import io.github.rspereiratech.openapi.generator.core.utils.AnnotationAttributeUtils;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
 * <p><b>Class-level attributes handled:</b>
 * {@code description}, {@code title}, {@code format}, {@code example},
 * {@code defaultValue}, {@code nullable}, {@code readOnly}, {@code writeOnly},
 * {@code accessMode}, {@code deprecated}, {@code externalDocs}.
 *
 * <p><b>Field-level attributes handled:</b>
 * {@code description}, {@code example}, {@code format}, {@code defaultValue},
 * {@code nullable}, {@code readOnly}, {@code writeOnly}, {@code accessMode},
 * {@code deprecated}, {@code pattern}, {@code minimum}, {@code maximum},
 * {@code exclusiveMinimum}, {@code exclusiveMaximum}, {@code minLength},
 * {@code maxLength}, {@code minProperties}, {@code maxProperties},
 * {@code multipleOf}, {@code allowableValues} (enum), {@code hidden}.
 *
 * <p>Hidden fields ({@code @Schema(hidden = true)}) are removed from the parent
 * schema's {@code properties} map and {@code required} list.
 *
 * <p>The following attributes are intentionally not handled here, as they are
 * already managed by {@link io.swagger.v3.core.converter.ModelConverters} or
 * other enrichers: {@code implementation}, {@code ref}, {@code name},
 * {@code allOf}/{@code anyOf}/{@code oneOf}/{@code not}, {@code subTypes},
 * {@code discriminatorProperty}, {@code requiredProperties}, {@code requiredMode}.
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
     * @param type    the root type to start class traversal from; must not be {@code null}
     * @param schemas mutable map of schema-name → schema; enricher mutates values in-place
     * @throws NullPointerException if {@code type} is {@code null}
     */
    @Override
    public void apply(Type type, Map<String, Schema<?>> schemas) {
        Preconditions.checkNotNull(type, "'type' must not be null");
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
     * and applies scalar attributes to {@code schema}, skipping any attribute for which
     * the schema already has a value (non-overwriting policy).
     *
     * @param clazz  the class to inspect; must not be {@code null}
     * @param schema the OpenAPI schema to enrich; must not be {@code null}
     */
    private static void applyClassLevelAnnotation(Class<?> clazz, Schema<?> schema) {
        var ann = clazz.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (ann == null) return;

        if (!ann.description().isBlank()  && schema.getDescription() == null) schema.setDescription(ann.description());
        if (!ann.title().isBlank()         && schema.getTitle()       == null) schema.setTitle(ann.title());
        if (!ann.format().isBlank()        && schema.getFormat()      == null) schema.setFormat(ann.format());
        if (!ann.example().isBlank()       && schema.getExample()     == null) schema.setExample(ann.example());
        if (!ann.defaultValue().isBlank()  && schema.getDefault()     == null) schema.setDefault(ann.defaultValue());

        if (ann.deprecated()  && schema.getDeprecated() == null) schema.setDeprecated(true);
        if (ann.nullable()    && schema.getNullable()   == null) schema.setNullable(true);

        applyAccessMode(ann.readOnly(), ann.writeOnly(), ann.accessMode(), schema);
        applyExternalDocs(ann.externalDocs(), schema);
    }

    // ------------------------------------------------------------------
    // Field-level annotations
    // ------------------------------------------------------------------

    /**
     * Iterates over all declared fields of {@code clazz} (including inherited) and applies
     * field-level {@link io.swagger.v3.oas.annotations.media.Schema} attributes to the
     * matching property schema.
     *
     * <p>Fields annotated with {@code @Schema(hidden = true)} are removed from the parent
     * schema's {@code properties} map and {@code required} list rather than being enriched.
     *
     * <p>Property name resolution honours {@link com.fasterxml.jackson.annotation.JsonProperty}.
     * Fields with no matching property in {@code schema.getProperties()} are silently skipped.
     *
     * @param clazz  the class whose fields are inspected; must not be {@code null}
     * @param schema the OpenAPI schema whose properties may be mutated; must not be {@code null}
     */
    private static void applyFieldLevelAnnotations(Class<?> clazz, Schema<?> schema) {
        if (schema.getProperties() == null) return;

        SchemaEnricherSupport.allDeclaredFields(clazz).forEach(field -> {
            var ann = field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            if (ann == null) return;

            String propertyName = SchemaEnricherSupport.resolvePropertyName(field);

            if (ann.hidden()) {
                removeHiddenField(schema, propertyName);
                return;
            }

            if (schema.getProperties().get(propertyName) instanceof Schema<?> prop) {
                applyPropertyAnnotation(ann, prop);
            }
        });
    }

    /**
     * Removes {@code propertyName} from the parent schema's {@code properties} map
     * and, if present, from its {@code required} list.
     *
     * @param schema       the parent schema to mutate; must not be {@code null}
     * @param propertyName the name of the property to remove
     */
    private static void removeHiddenField(Schema<?> schema, String propertyName) {
        schema.getProperties().remove(propertyName);
        if (schema.getRequired() != null) schema.getRequired().remove(propertyName);
    }

    /**
     * Applies all supported {@link io.swagger.v3.oas.annotations.media.Schema} field-level
     * attributes to {@code prop}, delegating to the attribute-group helpers.
     * Non-overwriting policy is respected by each helper.
     *
     * @param ann  the field annotation to read from; must not be {@code null}
     * @param prop the property schema to enrich; must not be {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // Schema.setEnum takes raw List
    private static void applyPropertyAnnotation(io.swagger.v3.oas.annotations.media.Schema ann, Schema<?> prop) {
        applyTextAttributes(ann, prop);
        applyFlagAttributes(ann, prop);
        applyNumericConstraints(ann, prop);
        if (ann.allowableValues().length > 0 && prop.getEnum() == null) {
            prop.setEnum((List) Arrays.asList(ann.allowableValues()));
        }
    }

    /**
     * Applies string-valued attributes — {@code description}, {@code example}, {@code format},
     * {@code pattern}, and {@code defaultValue} — from {@code ann} to {@code prop}.
     * Each attribute is only set when the annotation value is non-blank and the schema
     * does not already carry a value for that attribute.
     *
     * @param ann  the field annotation to read from; must not be {@code null}
     * @param prop the property schema to enrich; must not be {@code null}
     */
    private static void applyTextAttributes(io.swagger.v3.oas.annotations.media.Schema ann, Schema<?> prop) {
        if (!ann.description().isBlank()  && prop.getDescription() == null) prop.setDescription(ann.description());
        if (!ann.example().isBlank()       && prop.getExample()     == null) prop.setExample(ann.example());
        if (!ann.format().isBlank()        && prop.getFormat()      == null) prop.setFormat(ann.format());
        if (!ann.pattern().isBlank()       && prop.getPattern()     == null) prop.setPattern(ann.pattern());
        if (!ann.defaultValue().isBlank()  && prop.getDefault()     == null) prop.setDefault(ann.defaultValue());
    }

    /**
     * Applies boolean flag attributes — {@code deprecated}, {@code nullable},
     * {@code exclusiveMinimum}, {@code exclusiveMaximum}, {@code readOnly}, and
     * {@code writeOnly} (via {@code accessMode}) — from {@code ann} to {@code prop}.
     * Each flag is only set when {@code true} in the annotation and absent on the schema.
     *
     * @param ann  the field annotation to read from; must not be {@code null}
     * @param prop the property schema to enrich; must not be {@code null}
     */
    private static void applyFlagAttributes(io.swagger.v3.oas.annotations.media.Schema ann, Schema<?> prop) {
        if (ann.deprecated()        && prop.getDeprecated()         == null) prop.setDeprecated(true);
        if (ann.nullable()          && prop.getNullable()           == null) prop.setNullable(true);
        if (ann.exclusiveMinimum()  && prop.getExclusiveMinimum()   == null) prop.setExclusiveMinimum(true);
        if (ann.exclusiveMaximum()  && prop.getExclusiveMaximum()   == null) prop.setExclusiveMaximum(true);
        applyAccessMode(ann.readOnly(), ann.writeOnly(), ann.accessMode(), prop);
    }

    /**
     * Applies numeric constraint attributes — {@code minimum}, {@code maximum},
     * {@code minLength}, {@code maxLength}, {@code minProperties}, {@code maxProperties},
     * and {@code multipleOf} — from {@code ann} to {@code prop}.
     *
     * <p>{@code minimum} and {@code maximum} are parsed via
     * {@link AnnotationAttributeUtils#tryParse} and silently ignored when the string is not
     * a valid {@link BigDecimal}. Integer constraints use their annotation defaults
     * ({@code 0} / {@link Integer#MAX_VALUE}) as sentinels to detect "not set".
     *
     * @param ann  the field annotation to read from; must not be {@code null}
     * @param prop the property schema to enrich; must not be {@code null}
     */
    private static void applyNumericConstraints(io.swagger.v3.oas.annotations.media.Schema ann, Schema<?> prop) {
        if (!ann.minimum().isBlank() && prop.getMinimum() == null) AnnotationAttributeUtils.tryParse(ann.minimum(), BigDecimal::new).ifPresent(prop::setMinimum);
        if (!ann.maximum().isBlank() && prop.getMaximum() == null) AnnotationAttributeUtils.tryParse(ann.maximum(), BigDecimal::new).ifPresent(prop::setMaximum);
        if (ann.minLength() > 0              && prop.getMinLength()     == null) prop.setMinLength(ann.minLength());
        if (ann.maxLength() < Integer.MAX_VALUE && prop.getMaxLength()  == null) prop.setMaxLength(ann.maxLength());
        if (ann.minProperties() > 0          && prop.getMinProperties() == null) prop.setMinProperties(ann.minProperties());
        if (ann.maxProperties() > 0          && prop.getMaxProperties() == null) prop.setMaxProperties(ann.maxProperties());
        if (ann.multipleOf() != 0            && prop.getMultipleOf()    == null) prop.setMultipleOf(BigDecimal.valueOf(ann.multipleOf()));
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    /**
     * Applies {@code readOnly} / {@code writeOnly} to {@code schema} from the explicit flags
     * and the {@code accessMode} attribute. {@link io.swagger.v3.oas.annotations.media.Schema.AccessMode#AUTO}
     * is a no-op; {@code READ_ONLY} implies {@code readOnly: true}; {@code WRITE_ONLY} implies
     * {@code writeOnly: true}. Non-overwriting policy is respected.
     *
     * @param readOnly   value of {@code @Schema(readOnly = ...)}
     * @param writeOnly  value of {@code @Schema(writeOnly = ...)}
     * @param accessMode value of {@code @Schema(accessMode = ...)}
     * @param schema     the schema to mutate
     */
    private static void applyAccessMode(boolean readOnly, boolean writeOnly,
                                        io.swagger.v3.oas.annotations.media.Schema.AccessMode accessMode,
                                        Schema<?> schema) {
        boolean effectiveRead  = readOnly  || accessMode == io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
        boolean effectiveWrite = writeOnly || accessMode == io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
        if (effectiveRead  && schema.getReadOnly()  == null) schema.setReadOnly(true);
        if (effectiveWrite && schema.getWriteOnly() == null) schema.setWriteOnly(true);
    }

    /**
     * Converts the {@code externalDocs} attribute of a {@code @Schema} annotation to an
     * OpenAPI {@link ExternalDocumentation} model and sets it on {@code schema}, if
     * either {@code url} or {@code description} is non-blank and the schema does not
     * already have an {@code externalDocs} value.
     *
     * @param extDoc the annotation attribute; must not be {@code null}
     * @param schema the schema to mutate
     */
    private static void applyExternalDocs(
            io.swagger.v3.oas.annotations.ExternalDocumentation extDoc, Schema<?> schema) {
        if (schema.getExternalDocs() != null) return;
        if (extDoc.url().isBlank() && extDoc.description().isBlank()) return;

        var model = new ExternalDocumentation();
        if (!extDoc.url().isBlank())         model.setUrl(extDoc.url());
        if (!extDoc.description().isBlank()) model.setDescription(extDoc.description());
        schema.setExternalDocs(model);
    }

}
