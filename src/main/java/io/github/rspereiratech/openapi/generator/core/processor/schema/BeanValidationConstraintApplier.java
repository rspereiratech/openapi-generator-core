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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashSet;
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
 *   <li>{@link Size} → {@code minLength} / {@code maxLength}</li>
 *   <li>{@link NotNull} / {@link NotBlank} / {@link NotEmpty} → {@code nullable: false}</li>
 *   <li>{@link Pattern} → {@code pattern}</li>
 * </ul>
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
            String simpleName = clazz.getSimpleName();
            Object schemaObj = schemas.get(simpleName);
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

        for (Field field : clazz.getDeclaredFields()) {
            String propertyName = resolvePropertyName(field);
            Object propObj = properties.get(propertyName);
            if (!(propObj instanceof Schema<?> property)) continue;

            for (Annotation annotation : field.getAnnotations()) {
                applyConstraint(annotation, property);
            }
        }
    }

    @SuppressWarnings({"java:S1452", "java:S131"})
    private static void applyConstraint(Annotation annotation, Schema<?> property) {
        switch (annotation) {
            case Min min         -> property.setMinimum(BigDecimal.valueOf(min.value()));
            case Max max         -> property.setMaximum(BigDecimal.valueOf(max.value()));
            case DecimalMin dMin -> property.setMinimum(new BigDecimal(dMin.value()));
            case DecimalMax dMax -> property.setMaximum(new BigDecimal(dMax.value()));
            case Size size       -> {
                if (size.min() > 0) property.setMinLength(size.min());
                if (size.max() < Integer.MAX_VALUE) property.setMaxLength(size.max());
            }
            case NotNull ignored  -> property.setNullable(false);
            case NotBlank ignored -> property.setNullable(false);
            case NotEmpty ignored -> property.setNullable(false);
            case Pattern pat      -> property.setPattern(pat.regexp());
            default -> { /* annotation not handled */ }
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
    // Class traversal
    // ------------------------------------------------------------------

    /**
     * Collects all classes reachable from {@code type} by recursively walking declared fields.
     * Primitive types and classes whose package starts with {@code "java."} are skipped.
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

        for (Field field : clazz.getDeclaredFields()) {
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
