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
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.ConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.DecimalMaxConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.DecimalMinConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.EmailConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.MaxConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.MinConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.NegativeConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.NegativeOrZeroConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.NotBlankConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.NotEmptyConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.NotNullConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.PatternConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.PositiveConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.PositiveOrZeroConstraintHandler;
import io.github.rspereiratech.openapi.generator.core.processor.schema.constraints.SizeConstraintHandler;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Propagates Jakarta Bean Validation constraints to the corresponding
 * OpenAPI schema properties.
 *
 * <p>Swagger's {@link io.swagger.v3.core.converter.ModelConverters} does not always carry
 * constraint metadata (e.g. {@code minimum}/{@code maximum}) into the generated schema,
 * particularly for Java records. This class fills that gap by reflectively reading
 * constraint annotations from class fields and applying them to the already-resolved schemas.
 *
 * <p>Constraint mapping is handled by a Chain of Responsibility of {@link ConstraintHandler}s.
 * The default chain covers all standard Jakarta Bean Validation constraints. A custom chain
 * can be supplied via {@link #ValidationSchemaEnricher(List)} to add or replace handlers
 * (e.g. for Hibernate Validator extensions such as {@code @Length}).
 *
 * <p>Supported constraints (default chain):
 * <ul>
 *   <li>{@link jakarta.validation.constraints.Min} → {@code minimum}</li>
 *   <li>{@link jakarta.validation.constraints.Max} → {@code maximum}</li>
 *   <li>{@link jakarta.validation.constraints.DecimalMin} → {@code minimum}</li>
 *   <li>{@link jakarta.validation.constraints.DecimalMax} → {@code maximum}</li>
 *   <li>{@link jakarta.validation.constraints.Positive} → {@code minimum: 0, exclusiveMinimum: true}</li>
 *   <li>{@link jakarta.validation.constraints.PositiveOrZero} → {@code minimum: 0}</li>
 *   <li>{@link jakarta.validation.constraints.Negative} → {@code maximum: 0, exclusiveMaximum: true}</li>
 *   <li>{@link jakarta.validation.constraints.NegativeOrZero} → {@code maximum: 0}</li>
 *   <li>{@link jakarta.validation.constraints.Size} on strings → {@code minLength} / {@code maxLength}</li>
 *   <li>{@link jakarta.validation.constraints.Size} on collections / maps / arrays → {@code minItems} / {@code maxItems}</li>
 *   <li>{@link jakarta.validation.constraints.NotNull} → {@code nullable: false}</li>
 *   <li>{@link jakarta.validation.constraints.NotBlank} → {@code nullable: false, minLength: 1}</li>
 *   <li>{@link jakarta.validation.constraints.NotEmpty} → {@code nullable: false, minLength: 1} or {@code minItems: 1}</li>
 *   <li>{@link jakarta.validation.constraints.Pattern} → {@code pattern}</li>
 *   <li>{@link jakarta.validation.constraints.Email} → {@code format: email}</li>
 * </ul>
 *
 * <p>Field traversal walks the full superclass hierarchy and skips static and synthetic
 * members. Schema lookup uses the class simple name, matching the key convention used by
 * Swagger {@code ModelConverters}.
 *
 * @author ruispereira
 * @see ConstraintHandler
 */
public class ValidationSchemaEnricher implements SchemaEnricher {

    /**
     * Constraints that make a property mandatory.
     *
     * <p>Swagger's {@code ModelConverters} stopped deriving {@code required} from these in
     * swagger-core 2.2.29 — it still emits {@code nullable: false} and {@code minLength},
     * but no longer marks the property as required. This enricher restores that, so the
     * generated contract does not silently depend on the swagger-core version in use.
     */
    private static final List<Class<? extends Annotation>> REQUIRED_CONSTRAINTS =
            List.of(NotNull.class, NotBlank.class, NotEmpty.class);

    /** Ordered chain of handlers consulted in sequence; the first match wins. */
    private final List<ConstraintHandler> handlers;

    /**
     * Creates an instance with the default handler chain covering all standard
     * Jakarta Bean Validation constraints.
     */
    public ValidationSchemaEnricher() {
        this(List.of(
                new MinConstraintHandler(),
                new MaxConstraintHandler(),
                new DecimalMinConstraintHandler(),
                new DecimalMaxConstraintHandler(),
                new PositiveConstraintHandler(),
                new PositiveOrZeroConstraintHandler(),
                new NegativeConstraintHandler(),
                new NegativeOrZeroConstraintHandler(),
                new SizeConstraintHandler(),
                new NotNullConstraintHandler(),
                new NotBlankConstraintHandler(),
                new NotEmptyConstraintHandler(),
                new PatternConstraintHandler(),
                new EmailConstraintHandler()
        ));
    }

    /**
     * Creates an instance with a custom handler chain.
     *
     * <p>Handlers are tried in list order; the first matching handler wins.
     * Unrecognised annotations are silently ignored when no handler matches.
     *
     * @param handlers ordered list of constraint handlers; must not be {@code null} or empty
     * @throws NullPointerException     if {@code handlers} is {@code null}
     * @throws IllegalArgumentException if {@code handlers} is empty
     */
    public ValidationSchemaEnricher(List<ConstraintHandler> handlers) {
        Preconditions.checkNotNull(handlers, "'handlers' must not be null");
        Preconditions.checkArgument(!handlers.isEmpty(), "'handlers' must not be empty");
        this.handlers = List.copyOf(handlers);
    }

    /**
     * Applies Jakarta Bean Validation constraints from the fields of all classes reachable
     * from {@code type} to the corresponding properties in {@code schemas}.
     *
     * <p>Starting from {@code type}, all user-defined classes reachable through field
     * traversal are collected. For each discovered class, the schema is looked up in
     * {@code schemas} by {@link Class#getSimpleName()} — the same key convention used by
     * Swagger {@code ModelConverters}. Classes with no matching schema entry, or whose
     * schema has no properties, are silently skipped.
     *
     * <p>Does nothing when {@code schemas} is {@code null} or empty.
     *
     * @param type    the root type to start class traversal from; must not be {@code null}
     * @param schemas the mutable map of schema name → schema produced by {@code ModelConverters}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    @Override
    public void apply(Type type, Map<String, Schema<?>> schemas) {
        Preconditions.checkNotNull(type, "'type' must not be null");
        if (schemas == null || schemas.isEmpty()) {
            return;
        }

        SchemaEnricherSupport.collectReachableClasses(type, new HashSet<>()).forEach(clazz -> {
            Object schemaObj = schemas.get(clazz.getSimpleName());
            if (!(schemaObj instanceof Schema<?> schema)) {
                return;
            }
            if (schema.getProperties() == null) {
                return;
            }
            applyConstraintsFromClass(clazz, schema);
        });
    }

    // ------------------------------------------------------------------
    // Constraint application
    // ------------------------------------------------------------------

    /**
     * Applies all Jakarta Bean Validation constraints declared on the fields of {@code clazz}
     * to the matching properties in {@code schema}.
     *
     * <p>For each field, the property name is resolved via
     * {@link SchemaEnricherSupport#resolvePropertyName} and looked up in {@code schema}'s
     * property map. Fields whose name does not match an existing schema property are silently
     * skipped. Each annotation on a matched field is then dispatched to
     * {@link #applyConstraint}.
     *
     * @param clazz  the class whose fields are inspected; must not be {@code null}
     * @param schema the OpenAPI schema whose properties are mutated; must not be {@code null}
     */
    private void applyConstraintsFromClass(Class<?> clazz, Schema<?> schema) {
        applyConstraintsFromClass(clazz, schema, new HashSet<>());
        sortRequired(schema);
    }

    /**
     * Applies the constraints declared on {@code clazz} to {@code schema}, following
     * {@link JsonUnwrapped} fields into the flattened type.
     *
     * @param clazz   the class whose fields are inspected
     * @param schema  the schema whose properties are mutated
     * @param visited classes already traversed on this branch; guards against cycles
     *                introduced by self-referencing unwrapped types
     */
    private void applyConstraintsFromClass(Class<?> clazz, Schema<?> schema, Set<Class<?>> visited) {
        if (!visited.add(clazz)) {
            return;
        }
        Map<String, ?> properties = schema.getProperties();

        SchemaEnricherSupport.allDeclaredFields(clazz).forEach(field -> {
            if (field.isAnnotationPresent(JsonUnwrapped.class)) {
                Class<?> unwrapped = TypeUtils.toRawClass(field.getGenericType());
                if (unwrapped != null) {
                    applyConstraintsFromClass(unwrapped, schema, visited);
                }
                return;
            }
            String propertyName = SchemaEnricherSupport.resolvePropertyName(field);
            if (!(properties.get(propertyName) instanceof Schema<?> property)) {
                return;
            }
            Arrays.stream(field.getAnnotations())
                    .forEach(ann -> applyConstraint(ann, field.getGenericType(), property));
            if (impliesRequired(field)) {
                markRequired(schema, propertyName);
            }
        });
    }

    /**
     * Reports whether any annotation on {@code field} makes the property mandatory.
     *
     * <p>{@code @NotNull}, {@code @NotBlank} and {@code @NotEmpty} all reject a missing
     * value, which is what {@code required} means in OpenAPI.
     *
     * @param field the field to inspect
     * @return {@code true} when the field carries a presence constraint
     */
    private static boolean impliesRequired(Field field) {
        return REQUIRED_CONSTRAINTS.stream().anyMatch(field::isAnnotationPresent);
    }

    /**
     * Adds {@code propertyName} to {@code schema}'s {@code required} list, if absent.
     *
     * @param schema       the schema to mutate
     * @param propertyName the property to mark as required
     */
    private static void markRequired(Schema<?> schema, String propertyName) {
        if (schema.getRequired() == null || !schema.getRequired().contains(propertyName)) {
            schema.addRequiredItem(propertyName);
        }
    }

    /**
     * Sorts the {@code required} list so the emitted spec does not depend on the order in
     * which the JVM happens to report declared fields.
     *
     * @param schema the schema whose required list is normalised
     */
    private static void sortRequired(Schema<?> schema) {
        if (schema.getRequired() != null) {
            schema.getRequired().sort(null);
        }
    }

    /**
     * Dispatches a single annotation to the first matching {@link ConstraintHandler}.
     * Unrecognised annotations are silently ignored.
     *
     * @param annotation the annotation to dispatch; must not be {@code null}
     * @param fieldType  the declared type of the annotated field
     * @param property   the OpenAPI schema property to mutate; must not be {@code null}
     */
    private void applyConstraint(Annotation annotation, Type fieldType, Schema<?> property) {
        handlers.stream()
                .filter(h -> h.supports(annotation))
                .findFirst()
                .ifPresent(h -> h.apply(annotation, fieldType, property));
    }
}
