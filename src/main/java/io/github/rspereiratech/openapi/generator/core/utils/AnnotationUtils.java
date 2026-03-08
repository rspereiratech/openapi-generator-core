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
package io.github.rspereiratech.openapi.generator.core.utils;

import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reflection helpers for finding and merging annotations across type hierarchies.
 *
 * <p>All methods operate via the loaded annotation instances so that this
 * class stays decoupled from the Spring API version at compile time.
 *
 * @author ruispereira
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AnnotationUtils {

    /**
     * Finds an annotation of the given type on {@code element}, checking meta-annotations
     * one level deep.
     * <p>
     * First checks for a direct annotation; if absent, scans the element's annotations for
     * one that is itself meta-annotated with {@code annotationType} (e.g. {@code @GetMapping}
     * is meta-annotated with {@code @RequestMapping}).
     *
     * @param <A>            the annotation type to look for
     * @param element        the annotated element to inspect (method, class, parameter, …)
     * @param annotationType the annotation class to find
     * @return the annotation if found, or {@link Optional#empty()} otherwise
     */
    public static <A extends Annotation> Optional<A> findAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return Optional.ofNullable(element.getAnnotation(annotationType))
                .or(() -> Arrays.stream(element.getAnnotations())
                        .map(ann -> ann.annotationType().getAnnotation(annotationType))
                        .filter(Objects::nonNull)
                        .findFirst());
    }

    /**
     * Returns all annotations present on {@code element} whose simple class name matches
     * any of the given names.
     * <p>
     * Matching is done by {@link Class#getSimpleName()}, allowing callers to match annotations
     * by name without a compile-time dependency on the annotation class itself
     * (e.g. {@code "GetMapping"}, {@code "PostMapping"}).
     *
     * @param element     the annotated element to inspect (method, class, parameter, …)
     * @param simpleNames the simple class names to match against
     * @return an unmodifiable list of matching annotations, in declaration order; empty if none match
     */
    public static List<Annotation> findAnnotationsBySimpleName(
            AnnotatedElement element, String... simpleNames) {

        Set<String> names = Set.of(simpleNames);
        return Arrays.stream(element.getAnnotations())
                .filter(a -> names.contains(a.annotationType().getSimpleName()))
                .toList();
    }

    // ------------------------------------------------------------------
    // Type-hierarchy-aware annotation helpers
    // ------------------------------------------------------------------

    /**
     * Returns all annotations visible on a method by walking the full type hierarchy:
     * the declaring class, all abstract superclasses, and all implemented interfaces
     * (including super-interfaces), in that order.
     * <p>
     * More specific declarations win: if the same annotation type appears on both the
     * concrete method and an ancestor, the concrete version is kept.
     * <p>
     * This supports several common Spring MVC patterns:
     * <ul>
     *   <li>Swagger/SpringDoc annotations on an API interface, routing annotations on
     *       the concrete controller.</li>
     *   <li>Shared routing or documentation declared on an abstract base controller.</li>
     *   <li>Any combination of the above across multiple levels.</li>
     * </ul>
     *
     * @param method the method to inspect
     * @return a mutable list of merged annotations, most-specific first, in discovery order
     */
    public static List<Annotation> getAllAnnotations(Method method) {
        Map<Class<? extends Annotation>, Annotation> merged = new LinkedHashMap<>();

        // Declaring class has highest priority
        Arrays.stream(method.getAnnotations())
                .forEach(ann -> merged.put(ann.annotationType(), ann));

        // Walk superclasses then interfaces, from most-specific to least-specific
        TypeUtils.getAncestorTypes(method.getDeclaringClass()).stream()
                .map(ancestor -> TypeUtils.findMethodInAncestor(ancestor, method))
                .filter(Objects::nonNull)
                .flatMap(ancestorMethod -> Arrays.stream(ancestorMethod.getAnnotations()))
                .forEach(ann -> merged.putIfAbsent(ann.annotationType(), ann));

        return new ArrayList<>(merged.values());
    }

    /**
     * Returns the merged parameter annotations for parameter at position {@code index},
     * walking the full type hierarchy: the declaring class, all abstract superclasses,
     * and all implemented interfaces (including super-interfaces), in that order.
     * <p>
     * More specific declarations win: if the same annotation type appears on both the
     * concrete method and an ancestor, the concrete version is kept.
     *
     * @param method the method whose parameter annotations to inspect
     * @param index  the zero-based parameter index
     * @return an array of merged annotations for the given parameter, most-specific first
     */
    public static Annotation[] getAllParameterAnnotations(Method method, int index) {
        Map<Class<? extends Annotation>, Annotation> merged = new LinkedHashMap<>();

        // Declaring class has highest priority
        Annotation[][] classAnns = method.getParameterAnnotations();
        if (index < classAnns.length) {
            Arrays.stream(classAnns[index])
                    .forEach(ann -> merged.put(ann.annotationType(), ann));
        }

        // Walk superclasses then interfaces, from most-specific to least-specific
        TypeUtils.getAncestorTypes(method.getDeclaringClass()).stream()
                .map(ancestor -> TypeUtils.findMethodInAncestor(ancestor, method))
                .filter(Objects::nonNull)
                .map(Method::getParameterAnnotations)
                .filter(anns -> index < anns.length)
                .flatMap(anns -> Arrays.stream(anns[index]))
                .forEach(ann -> merged.putIfAbsent(ann.annotationType(), ann));

        return merged.values().toArray(Annotation[]::new);
    }

    /**
     * Collects <em>all</em> annotations from {@code clazz} and its full type
     * hierarchy whose annotation type (or a meta-annotation) has the given
     * simple name.
     *
     * <p>Unlike {@link #getAllAnnotations(Class)}, this method does <b>not</b>
     * deduplicate by annotation type.  Every matching annotation found at each
     * level of the hierarchy is included, allowing callers to accumulate values
     * from multiple declarations spread across different interfaces — for example
     * two {@code @Tag} annotations on a generic base interface and a specific
     * child interface.
     *
     * @param clazz      the class to inspect
     * @param simpleName the simple name to match (e.g. {@code "Tag"})
     * @return all matching annotations in hierarchy order (class first, then ancestors)
     */
    public static List<Annotation> collectAllBySimpleName(Class<?> clazz, String simpleName) {
        return Stream.concat(Stream.of(clazz), TypeUtils.getAncestorTypes(clazz).stream())
                .flatMap(c -> Arrays.stream(c.getAnnotations()))
                .filter(ann -> isMetaAnnotated(ann.annotationType(), simpleName))
                .toList();
    }

    /**
     * Returns all annotations visible on a class by walking the full type hierarchy:
     * the class itself, all abstract superclasses, and all implemented interfaces
     * (including super-interfaces), in that order.
     * <p>
     * More specific declarations win: if the same annotation type appears on both the
     * class and an ancestor, the class-level version is kept.
     *
     * @param clazz the class to inspect
     * @return a mutable list of merged annotations, most-specific first, in discovery order
     */
    public static List<Annotation> getAllAnnotations(Class<?> clazz) {
        Map<Class<? extends Annotation>, Annotation> merged = new LinkedHashMap<>();

        // Class itself has highest priority
        Arrays.stream(clazz.getAnnotations())
                .forEach(ann -> merged.put(ann.annotationType(), ann));

        // Walk superclasses then interfaces, from most-specific to least-specific
        TypeUtils.getAncestorTypes(clazz).stream()
                .flatMap(ancestor -> Arrays.stream(ancestor.getAnnotations()))
                .forEach(ann -> merged.putIfAbsent(ann.annotationType(), ann));

        return new ArrayList<>(merged.values());
    }

    // ------------------------------------------------------------------
    // Meta-annotation traversal
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code annotationType} is, or is transitively meta-annotated
     * with, an annotation whose simple name equals {@code simpleName}.
     * <p>
     * This enables detection of composed annotations such as {@code @CustomRestController}
     * that meta-annotates {@code @RestController} without requiring a direct compile-time
     * reference to the target annotation type.
     * <p>
     * A cycle guard prevents infinite recursion through annotation types that are mutually
     * meta-annotated (e.g. {@code @Documented ↔ @Documented}).
     *
     * @param annotationType the annotation type to inspect
     * @param simpleName     the simple class name to match (e.g. {@code "RestController"})
     * @return {@code true} if a direct or transitive match is found
     */
    public static boolean isMetaAnnotated(Class<? extends Annotation> annotationType, String simpleName) {
        return isMetaAnnotated(annotationType, simpleName, new HashSet<>());
    }

    /**
     * Recursive implementation of {@link #isMetaAnnotated(Class, String)}.
     *
     * @param type       the annotation type currently being inspected
     * @param simpleName the simple name to match
     * @param visited    cycle-guard set; returns {@code false} immediately if already visited
     * @return {@code true} if a direct or transitive match is found
     */
    private static boolean isMetaAnnotated(Class<? extends Annotation> type,
                                            String simpleName,
                                            Set<Class<? extends Annotation>> visited) {
        if (!visited.add(type)) return false;
        if (simpleName.equals(type.getSimpleName())) return true;
        return Arrays.stream(type.getAnnotations())
                .anyMatch(meta -> isMetaAnnotated(meta.annotationType(), simpleName, visited));
    }

    // ------------------------------------------------------------------
    // Swagger annotation helpers
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code ann} is a Swagger annotation ({@code io.swagger} package)
     * with the given simple name.
     *
     * <p>The check is name-based combined with a package prefix guard ({@code "io.swagger"})
     * to remain compatible across classloader boundaries.</p>
     *
     * @param ann        the annotation to test; must not be {@code null}
     * @param simpleName the expected simple name (e.g. {@code "Operation"}, {@code "Parameter"})
     * @return {@code true} if the annotation matches
     */
    @SuppressWarnings("java:S1872")
    public static boolean isSwaggerAnnotation(Annotation ann, String simpleName) {
        return simpleName.equals(ann.annotationType().getSimpleName())
            && ann.annotationType().getPackageName().startsWith("io.swagger");
    }

    /**
     * Resolves the first non-blank value from a string-array annotation attribute
     * (e.g. {@code produces} or {@code consumes}) across all effective annotations on a method.
     *
     * <p>Annotations that do not declare the attribute are silently skipped.</p>
     *
     * @param method         the controller method to inspect; must not be {@code null}
     * @param attributeName  the annotation attribute to read (e.g. {@code "produces"})
     * @param defaultValue   value to return when no non-blank entry is found
     * @return the first non-blank value found, or {@code defaultValue}
     */
    public static String resolveStringArrayAttribute(Method method, String attributeName, String defaultValue) {
        return getAllAnnotations(method).stream()
                .flatMap(ann -> AnnotationAttributeUtils.getStringArrayValue(ann, attributeName).stream())
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse(defaultValue);
    }
}
