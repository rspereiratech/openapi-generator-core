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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rspereiratech.openapi.generator.core.utils.TypeUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Shared utilities for {@link SchemaEnricher} implementations.
 *
 * <p>Provides field traversal, class reachability, property-name resolution, and
 * Java built-in detection helpers that are used by both {@link ValidationSchemaEnricher}
 * and {@link SchemaAnnotationEnricher}.
 *
 * <p>This is a package-private utility class; it is not part of the public API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SchemaEnricherSupport {

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
    static List<Field> allDeclaredFields(Class<?> clazz) {
        return Stream.<Class<?>>iterate(clazz, c -> c != null && c != Object.class, Class::getSuperclass)
                .flatMap(c -> Arrays.stream(c.getDeclaredFields()))
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
                .toList();
    }

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
     * @param type    the type to start from; may be a plain {@link Class} or a
     *                {@link java.lang.reflect.ParameterizedType}
     * @param visited mutable accumulator; classes are added here as they are discovered —
     *                also acts as a cycle guard so the same class is never traversed twice
     * @return {@code visited} (the same instance), enriched with all newly discovered classes
     */
    static Set<Class<?>> collectReachableClasses(Type type, Set<Class<?>> visited) {
        Class<?> clazz = TypeUtils.toRawClass(type);
        if (clazz == null || clazz.isPrimitive() || isJavaBuiltin(clazz) || !visited.add(clazz)) {
            return visited;
        }

        allDeclaredFields(clazz)
                .forEach(field -> collectReachableClasses(field.getGenericType(), visited));

        return visited;
    }

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
    static String resolvePropertyName(Field field) {
        return Optional.ofNullable(field.getAnnotation(JsonProperty.class))
                .map(JsonProperty::value)
                .filter(v -> !v.isEmpty() && !JsonProperty.USE_DEFAULT_NAME.equals(v))
                .orElseGet(field::getName);
    }

    /**
     * Returns {@code true} if {@code clazz} belongs to the Java platform itself and
     * should be excluded from constraint traversal.
     *
     * <p>Matches classes whose package starts with {@code "java."} or {@code "javax."},
     * and anonymous / local classes that have no package name.
     *
     * @param clazz the class to test; must not be {@code null}
     * @return {@code true} if the class is a Java built-in and should be skipped
     */
    static boolean isJavaBuiltin(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        return pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.isEmpty();
    }
}
