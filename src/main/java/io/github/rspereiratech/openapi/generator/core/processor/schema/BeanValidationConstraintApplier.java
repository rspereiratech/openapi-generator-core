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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that propagates Jakarta Bean Validation constraints to the corresponding
 * OpenAPI schema properties.
 *
 * <p>Swagger's {@link io.swagger.v3.core.converter.ModelConverters} does not always carry
 * constraint metadata (e.g. {@code minimum}/{@code maximum}) into the generated schema,
 * particularly for Java records. This class fills that gap by reflectively reading
 * constraint annotations from class fields and record components and applying them to the
 * already-resolved schemas.
 *
 * <p>Supported constraints:
 * <ul>
 *   <li>{@link Min} → {@code minimum}</li>
 *   <li>{@link Max} → {@code maximum}</li>
 *   <li>{@link DecimalMin} → {@code minimum}</li>
 *   <li>{@link DecimalMax} → {@code maximum}</li>
 *   <li>{@link Positive} → {@code minimum: 0, exclusiveMinimum: true}</li>
 *   <li>{@link PositiveOrZero} → {@code minimum: 0}</li>
 *   <li>{@link Negative} → {@code maximum: 0, exclusiveMaximum: true}</li>
 *   <li>{@link NegativeOrZero} → {@code maximum: 0}</li>
 *   <li>{@link Size} on strings → {@code minLength} / {@code maxLength}</li>
 *   <li>{@link Size} on collections / maps / arrays → {@code minItems} / {@code maxItems}</li>
 *   <li>{@link NotNull} / {@link NotBlank} / {@link NotEmpty} → {@code nullable: false}</li>
 *   <li>{@link Pattern} → {@code pattern}</li>
 *   <li>{@link Email} → {@code format: email}</li>
 * </ul>
 *
 * <p>Field traversal walks the full superclass hierarchy and skips static and synthetic
 * members. Schema lookup uses the class simple name, matching the key convention used by
 * Swagger {@code ModelConverters}.
 *
 * <p>This class cannot be instantiated.
 *
 * @author ruispereira
 */
public final class BeanValidationConstraintApplier {

    private BeanValidationConstraintApplier() {
        // utility class — not instantiable
    }

    /**
     * Applies Jakarta Bean Validation constraints from the fields of all classes reachable
     * from {@code type} to the corresponding properties in {@code schemas}.
     *
     * <p>Each entry in {@code schemas} whose name matches a reachable class is processed.
     * Property names are resolved by checking {@link JsonProperty} first, then falling back
     * to the field name.
     *
     * @param type    the top-level type being resolved; used as the root for class traversal
     * @param schemas the mutable map of schema name → schema from {@code ModelConverters}
     *                (accepts raw {@code Map<String, Schema>} via unchecked cast)
     */
    @SuppressWarnings({"java:S1452", "unchecked"})
    public static void apply(Type type, Map<String, ?> schemas) {
        if (schemas == null || schemas.isEmpty()) return;

        Set<Class<?>> reachable = collectReachableClasses(type, new HashSet<>());

        for (Class<?> clazz : reachable) {
            Object schemaObj = schemas.get(clazz.getSimpleName());
            if (!(schemaObj instanceof Schema<?> schema)) continue;
            if (schema.getProperties() == null) continue;

            applyConstraintsFromClass(clazz, schema);
        }
    }

    // ------------------------------------------------------------------
    // Constraint application
    // ------------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes", "java:S1452"})
    private static void applyConstraintsFromClass(Class<?> clazz, Schema<?> schema) {
        Map<String, ?> properties = schema.getProperties();

        for (Field field : allDeclaredFields(clazz)) {
            String propertyName = resolvePropertyName(field);
            Object propObj = properties.get(propertyName);
            if (!(propObj instanceof Schema<?> property)) continue;

            for (Annotation annotation : field.getAnnotations()) {
                applyConstraint(annotation, field.getGenericType(), property);
            }
        }
    }

    @SuppressWarnings({"java:S1452", "java:S131"})
    private static void applyConstraint(Annotation annotation, Type fieldType, Schema<?> property) {
        switch (annotation) {
            case Min min             -> property.setMinimum(BigDecimal.valueOf(min.value()));
            case Max max             -> property.setMaximum(BigDecimal.valueOf(max.value()));
            case DecimalMin dMin     -> property.setMinimum(new BigDecimal(dMin.value()));
            case DecimalMax dMax     -> property.setMaximum(new BigDecimal(dMax.value()));
            case Positive ignored    -> { property.setMinimum(BigDecimal.ZERO); property.setExclusiveMinimum(true); }
            case PositiveOrZero ignored -> property.setMinimum(BigDecimal.ZERO);
            case Negative ignored    -> { property.setMaximum(BigDecimal.ZERO); property.setExclusiveMaximum(true); }
            case NegativeOrZero ignored -> property.setMaximum(BigDecimal.ZERO);
            case Size size           -> applySize(size, fieldType, property);
            case NotNull ignored     -> property.setNullable(false);
            case NotBlank ignored    -> property.setNullable(false);
            case NotEmpty ignored    -> property.setNullable(false);
            case Pattern pat         -> property.setPattern(pat.regexp());
            case Email ignored       -> property.setFormat("email");
            default -> { /* annotation not handled */ }
        }
    }

    private static void applySize(Size size, Type fieldType, Schema<?> property) {
        Class<?> raw = toRawClass(fieldType);
        boolean isMultiValued = raw != null
                && (Collection.class.isAssignableFrom(raw) || Map.class.isAssignableFrom(raw) || raw.isArray());

        if (isMultiValued) {
            if (size.min() > 0)                       property.setMinItems(size.min());
            if (size.max() < Integer.MAX_VALUE)        property.setMaxItems(size.max());
        } else {
            if (size.min() > 0)                       property.setMinLength(size.min());
            if (size.max() < Integer.MAX_VALUE)        property.setMaxLength(size.max());
        }
    }

    // ------------------------------------------------------------------
    // Property name resolution
    // ------------------------------------------------------------------

    private static String resolvePropertyName(Field field) {
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isEmpty()
                && !JsonProperty.USE_DEFAULT_NAME.equals(jsonProperty.value())) {
            return jsonProperty.value();
        }
        return field.getName();
    }

    // ------------------------------------------------------------------
    // Field traversal
    // ------------------------------------------------------------------

    /**
     * Returns all non-static, non-synthetic declared fields of {@code clazz} and every
     * class in its superclass chain up to (but not including) {@link Object}.
     *
     * @param clazz the class to inspect
     * @return ordered list of eligible fields
     */
    private static List<Field> allDeclaredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                    fields.add(f);
                }
            }
        }
        return fields;
    }

    // ------------------------------------------------------------------
    // Class traversal
    // ------------------------------------------------------------------

    /**
     * Collects all classes reachable from {@code type} by recursively walking non-static,
     * non-synthetic declared fields across the full superclass hierarchy.
     * Primitive types and classes whose package starts with {@code "java."} or
     * {@code "javax."} are skipped.
     *
     * @param type    the root type to start from
     * @param visited accumulator of already-visited classes; prevents infinite loops
     * @return the set of reachable user-defined classes (including {@code type} itself if applicable)
     */
    private static Set<Class<?>> collectReachableClasses(Type type, Set<Class<?>> visited) {
        Class<?> clazz = toRawClass(type);
        if (clazz == null || clazz.isPrimitive() || isJavaBuiltin(clazz) || !visited.add(clazz)) {
            return visited;
        }

        for (Field field : allDeclaredFields(clazz)) {
            collectReachableClasses(field.getGenericType(), visited);
        }

        return visited;
    }

    private static Class<?> toRawClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) return c;
        return null;
    }

    private static boolean isJavaBuiltin(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        return pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.isEmpty();
    }
}
