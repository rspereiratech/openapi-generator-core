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
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
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
     * <p>Starting from {@code type}, all user-defined classes reachable through field
     * traversal are collected. For each discovered class, the schema is looked up in
     * {@code schemas} by {@link Class#getSimpleName()} — the same key convention used by
     * Swagger {@code ModelConverters}. Classes with no matching schema entry, or whose
     * schema has no properties, are silently skipped.
     *
     * <p>Does nothing when {@code schemas} is {@code null} or empty.
     *
     * @param type    the root type to start class traversal from; must not be {@code null}
     * @param schemas the mutable map of schema name → schema produced by {@code ModelConverters};
     *                must be a {@code Map<String, Schema>} at runtime (unchecked cast applied)
     */
    @SuppressWarnings({"java:S1452", "unchecked"})
    public static void apply(Type type, Map<String, ?> schemas) {
        if (schemas == null || schemas.isEmpty()) return;

        collectReachableClasses(type, new HashSet<>()).forEach(clazz -> {
            if (!(schemas.get(clazz.getSimpleName()) instanceof Schema<?> schema)) return;
            if (schema.getProperties() == null) return;
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
     * <p>For each field, the property name is resolved via {@link #resolvePropertyName} and
     * looked up in {@code schema}'s property map. Fields whose name does not match an existing
     * schema property are silently skipped. Each annotation on a matched field is then
     * dispatched to {@link #applyConstraint}.
     *
     * @param clazz  the class whose fields are inspected; must not be {@code null}
     * @param schema the OpenAPI schema whose properties are mutated; must not be {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes", "java:S1452"})
    private static void applyConstraintsFromClass(Class<?> clazz, Schema<?> schema) {
        Map<String, ?> properties = schema.getProperties();

        allDeclaredFields(clazz).forEach(field -> {
            if (!(properties.get(resolvePropertyName(field)) instanceof Schema<?> property)) return;
            Arrays.stream(field.getAnnotations())
                    .forEach(ann -> applyConstraint(ann, field.getGenericType(), property));
        });
    }

    /**
     * Maps a single Jakarta Bean Validation annotation to its OpenAPI schema equivalent.
     *
     * <p>Handled annotations and their OpenAPI mappings:
     * <table border="1">
     *   <tr><th>Annotation</th><th>OpenAPI field(s)</th></tr>
     *   <tr><td>{@link Min}</td><td>{@code minimum}</td></tr>
     *   <tr><td>{@link Max}</td><td>{@code maximum}</td></tr>
     *   <tr><td>{@link DecimalMin}</td><td>{@code minimum}; also {@code exclusiveMinimum: true} when {@code inclusive=false}</td></tr>
     *   <tr><td>{@link DecimalMax}</td><td>{@code maximum}; also {@code exclusiveMaximum: true} when {@code inclusive=false}</td></tr>
     *   <tr><td>{@link Positive}</td><td>{@code minimum: 0, exclusiveMinimum: true}</td></tr>
     *   <tr><td>{@link PositiveOrZero}</td><td>{@code minimum: 0}</td></tr>
     *   <tr><td>{@link Negative}</td><td>{@code maximum: 0, exclusiveMaximum: true}</td></tr>
     *   <tr><td>{@link NegativeOrZero}</td><td>{@code maximum: 0}</td></tr>
     *   <tr><td>{@link Size}</td><td>{@code minLength}/{@code maxLength} or {@code minItems}/{@code maxItems} — see {@link #applySize}</td></tr>
     *   <tr><td>{@link NotNull}</td><td>{@code nullable: false}</td></tr>
     *   <tr><td>{@link NotBlank}</td><td>{@code nullable: false, minLength: 1}</td></tr>
     *   <tr><td>{@link NotEmpty}</td><td>{@code nullable: false}; also {@code minLength: 1} for strings or {@code minItems: 1} for collections/arrays</td></tr>
     *   <tr><td>{@link Pattern}</td><td>{@code pattern}</td></tr>
     *   <tr><td>{@link Email}</td><td>{@code format: email}</td></tr>
     * </table>
     *
     * <p>Unrecognised annotations are silently ignored.
     *
     * @param annotation the annotation to map; must not be {@code null}
     * @param fieldType  the declared type of the annotated field; passed to {@link #applySize}
     *                   to distinguish string-like from multi-valued types
     * @param property   the OpenAPI schema property to mutate; must not be {@code null}
     */
    @SuppressWarnings({"java:S1452", "java:S131"})
    private static void applyConstraint(Annotation annotation, Type fieldType, Schema<?> property) {
        switch (annotation) {
            case Min min             -> property.setMinimum(BigDecimal.valueOf(min.value()));
            case Max max             -> property.setMaximum(BigDecimal.valueOf(max.value()));
            case DecimalMin dMin     -> {
                property.setMinimum(new BigDecimal(dMin.value()));
                if (!dMin.inclusive()) property.setExclusiveMinimum(true);
            }
            case DecimalMax dMax     -> {
                property.setMaximum(new BigDecimal(dMax.value()));
                if (!dMax.inclusive()) property.setExclusiveMaximum(true);
            }
            case Positive ignored    -> { property.setMinimum(BigDecimal.ZERO); property.setExclusiveMinimum(true); }
            case PositiveOrZero ignored -> property.setMinimum(BigDecimal.ZERO);
            case Negative ignored    -> { property.setMaximum(BigDecimal.ZERO); property.setExclusiveMaximum(true); }
            case NegativeOrZero ignored -> property.setMaximum(BigDecimal.ZERO);
            case Size size           -> applySize(size, fieldType, property);
            case NotNull ignored     -> property.setNullable(false);
            case NotBlank ignored    -> { property.setNullable(false); property.setMinLength(1); }
            case NotEmpty ignored    -> {
                property.setNullable(false);
                if (isMultiValued(fieldType)) property.setMinItems(1);
                else                          property.setMinLength(1);
            }
            case Pattern pat         -> property.setPattern(pat.regexp());
            case Email ignored       -> property.setFormat("email");
            default -> { /* annotation not handled */ }
        }
    }

    /**
     * Applies a {@link Size} constraint to an OpenAPI schema property.
     *
     * <p>The constraint is mapped differently depending on the field type:
     * <ul>
     *   <li>{@link Collection}, {@link Map}, or array → {@code minItems} / {@code maxItems}</li>
     *   <li>All other types (typically {@link String}) → {@code minLength} / {@code maxLength}</li>
     * </ul>
     *
     * <p>Boundary values that carry no information are suppressed:
     * {@code min = 0} (the default) is not written, and {@code max = Integer.MAX_VALUE}
     * (the default) is not written.
     *
     * @param size      the constraint to apply; must not be {@code null}
     * @param fieldType the declared type of the annotated field; used to select the mapping
     * @param property  the OpenAPI schema property to mutate; must not be {@code null}
     */
    private static void applySize(Size size, Type fieldType, Schema<?> property) {
        Consumer<Integer> setMin = isMultiValued(fieldType) ? property::setMinItems : property::setMinLength;
        Consumer<Integer> setMax = isMultiValued(fieldType) ? property::setMaxItems : property::setMaxLength;

        if (size.min() > 0)                setMin.accept(size.min());
        if (size.max() < Integer.MAX_VALUE) setMax.accept(size.max());
    }

    /**
     * Returns {@code true} if {@code type} represents a multi-valued container:
     * a {@link Collection}, a {@link Map}, or an array.
     *
     * <p>Used by {@link #applySize} to decide whether {@code @Size} maps to item-count
     * constraints ({@code minItems}/{@code maxItems}) or length constraints
     * ({@code minLength}/{@code maxLength}).
     *
     * @param type the field type to test; must not be {@code null}
     * @return {@code true} if the type is a known multi-valued container
     */
    private static boolean isMultiValued(Type type) {
        Class<?> raw = toRawClass(type);
        return raw != null
                && (Collection.class.isAssignableFrom(raw)
                ||  Map.class.isAssignableFrom(raw)
                ||  raw.isArray());
    }

    // ------------------------------------------------------------------
    // Property name resolution
    // ------------------------------------------------------------------

    /**
     * Resolves the JSON property name for a field.
     *
     * <p>Uses the value declared on {@link JsonProperty} when present and explicit;
     * falls back to the field's declared name otherwise. The {@link JsonProperty} value
     * is considered explicit when it is non-empty and not equal to
     * {@link JsonProperty#USE_DEFAULT_NAME}.
     *
     * @param field the field to resolve the name for; must not be {@code null}
     * @return the serialised property name; never {@code null}
     */
    private static String resolvePropertyName(Field field) {
        return Optional.ofNullable(field.getAnnotation(JsonProperty.class))
                .map(JsonProperty::value)
                .filter(v -> !v.isEmpty() && !JsonProperty.USE_DEFAULT_NAME.equals(v))
                .orElseGet(field::getName);
    }

    // ------------------------------------------------------------------
    // Field traversal
    // ------------------------------------------------------------------

    /**
     * Returns all non-static, non-synthetic declared fields of {@code clazz} and every
     * class in its superclass chain, up to (but not including) {@link Object}.
     *
     * <p>Fields are ordered from the most-derived class down to the root, matching the
     * natural declaration order within each class. Static and synthetic fields (e.g.
     * compiler-generated members in inner classes) are excluded.
     *
     * @param clazz the class to inspect; must not be {@code null}
     * @return unmodifiable list of eligible instance fields in hierarchy order
     */
    private static List<Field> allDeclaredFields(Class<?> clazz) {
        return Stream.<Class<?>>iterate(clazz, c -> c != null && c != Object.class, Class::getSuperclass)
                .flatMap(c -> Arrays.stream(c.getDeclaredFields()))
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
                .toList();
    }

    // ------------------------------------------------------------------
    // Class traversal
    // ------------------------------------------------------------------

    /**
     * Collects all user-defined classes reachable from {@code type} by recursively walking
     * the non-static, non-synthetic declared fields of each discovered class across its full
     * superclass hierarchy.
     *
     * <p>A class is skipped — and its fields are not traversed — when any of the following
     * conditions holds:
     * <ul>
     *   <li>The type cannot be reduced to a raw {@link Class} (e.g. wildcards, type variables).</li>
     *   <li>The class is a primitive type.</li>
     *   <li>The class belongs to the Java platform ({@link #isJavaBuiltin}).</li>
     *   <li>The class has already been added to {@code visited} (cycle guard).</li>
     * </ul>
     *
     * @param type    the type to start from; may be a plain {@link Class} or a {@link java.lang.reflect.ParameterizedType}
     * @param visited mutable accumulator; classes are added here as they are discovered —
     *                also acts as a cycle guard so the same class is never traversed twice
     * @return {@code visited} (the same instance), enriched with all newly discovered classes
     */
    private static Set<Class<?>> collectReachableClasses(Type type, Set<Class<?>> visited) {
        Class<?> clazz = toRawClass(type);
        if (clazz == null || clazz.isPrimitive() || isJavaBuiltin(clazz) || !visited.add(clazz)) {
            return visited;
        }

        allDeclaredFields(clazz)
                .forEach(field -> collectReachableClasses(field.getGenericType(), visited));

        return visited;
    }

    /**
     * Extracts the raw {@link Class} from a {@link Type}.
     *
     * <p>Handles two cases:
     * <ul>
     *   <li>A plain {@link Class} — returned directly.</li>
     *   <li>A {@link ParameterizedType} whose raw type is a {@link Class} — the raw class is returned.</li>
     * </ul>
     * All other type kinds (type variables, wildcards, generic arrays) return {@code null}.
     *
     * @param type the type to inspect; must not be {@code null}
     * @return the raw {@link Class}, or {@code null} if the type cannot be reduced to one
     */
    private static Class<?> toRawClass(Type type) {
        return switch (type) {
            case Class<?> c                                              -> c;
            case ParameterizedType pt when pt.getRawType() instanceof Class<?> c -> c;
            default                                                      -> null;
        };
    }

    /**
     * Returns {@code true} if {@code clazz} belongs to the Java platform itself and
     * should be excluded from constraint traversal.
     *
     * <p>Matches classes whose package starts with {@code "java."} or {@code "javax."},
     * and anonymous / local classes that have no package name.</p>
     *
     * @param clazz the class to test; must not be {@code null}
     * @return {@code true} if the class is a Java built-in and should be skipped
     */
    private static boolean isJavaBuiltin(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        return pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.isEmpty();
    }
}
