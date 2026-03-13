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
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility methods for reflectively reading attribute values from annotation instances.
 *
 * <p>All methods are stateless and thread-safe. The class cannot be instantiated.
 *
 * @author ruispereira
 */
@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AnnotationAttributeUtils {
    /** Log template for unexpected reflection failures: attribute name, annotation simple name, error message. */
    private static final String INVOKE_FAILED = "Failed to invoke '{}' on {}: {}";

    /**
     * Reflectively reads a {@code String[]} attribute from an annotation by name.
     * <p>
     * Invokes {@code annotation.methodName()} via reflection and returns the result as an
     * unmodifiable list. Returns an empty list if the method does not exist, returns a
     * non-{@code String[]} type, or throws during invocation.
     *
     * @param annotation the annotation instance to read from
     * @param methodName the attribute name to invoke (e.g. {@code "value"}, {@code "path"})
     * @return the attribute values, or an empty list if unavailable
     */
    public static List<String> getStringArrayValue(Annotation annotation, String methodName) {
        Object raw = invokeAttribute(annotation, methodName);
        return raw instanceof String[] arr ? List.of(arr) : List.of();
    }

    /**
     * Extracts the first path segment from a mapping annotation's {@code value()} or
     * {@code path()} attribute, in that order of preference.
     * <p>
     * Tries {@code value()} first; if empty, falls back to {@code path()}. Returns the
     * first element of whichever array is non-empty, or {@code ""} if both are absent.
     *
     * @param annotation the mapping annotation to inspect (e.g. {@code @RequestMapping})
     * @return the first path found, or {@code ""} if neither attribute yields a value
     */
    public static String extractPath(Annotation annotation) {
        return Stream.of("value", "path")
                .map(attr -> getStringArrayValue(annotation, attr))
                .filter(Predicate.not(List::isEmpty))
                .findFirst()
                .map(List::getFirst)
                .orElse("");
    }

    /**
     * Reflectively reads a {@code String} attribute from an annotation by name.
     * <p>
     * Invokes {@code annotation.attributeName()} via reflection and returns the result if it
     * is a {@link String}. Returns {@code ""} if the method does not exist, returns a
     * non-{@code String} type, or throws during invocation.
     *
     * @param annotation    the annotation instance to read from
     * @param attributeName the attribute name to invoke (e.g. {@code "value"}, {@code "name"})
     * @return the attribute value, or {@code ""} if unavailable
     */
    public static String getStringAttribute(Annotation annotation, String attributeName) {
        Object raw = invokeAttribute(annotation, attributeName);
        return raw instanceof String s ? s : "";
    }

    /**
     * Reflectively reads an {@code int} attribute from an annotation by name.
     * <p>
     * Invokes {@code annotation.attributeName()} via reflection and returns the result if it
     * is an {@link Integer}. Returns {@code defaultValue} if the method does not exist,
     * returns a non-{@code int} type, or throws during invocation.
     *
     * @param annotation    the annotation instance to read from
     * @param attributeName the attribute name to invoke (e.g. {@code "code"}, {@code "status"})
     * @param defaultValue  the fallback value when the attribute is unavailable
     * @return the attribute value, or {@code defaultValue} if unavailable
     */
    public static int getIntAttribute(Annotation annotation, String attributeName, int defaultValue) {
        Object raw = invokeAttribute(annotation, attributeName);
        return raw instanceof Integer i ? i : defaultValue;
    }

    /**
     * Reflectively reads a {@code boolean} attribute from an annotation by name.
     * <p>
     * Invokes {@code annotation.attributeName()} via reflection and returns the result if it
     * is a {@link Boolean}. Returns {@code defaultValue} if the method does not exist,
     * returns a non-{@code boolean} type, or throws during invocation.
     *
     * @param annotation    the annotation instance to read from
     * @param attributeName the attribute name to invoke (e.g. {@code "required"}, {@code "allowEmptyValue"})
     * @param defaultValue  the fallback value when the attribute is unavailable
     * @return the attribute value, or {@code defaultValue} if unavailable
     */
    public static boolean getBooleanAttribute(Annotation annotation, String attributeName, boolean defaultValue) {
        Object raw = invokeAttribute(annotation, attributeName);
        return raw instanceof Boolean b ? b : defaultValue;
    }

    /**
     * Reflectively reads a single {@link Annotation}-typed attribute from an annotation by name.
     * <p>
     * Returns {@link Optional#empty()} if the method does not exist, returns a
     * non-{@link Annotation} type, or throws during invocation.
     *
     * @param annotation    the annotation instance to read from
     * @param attributeName the attribute name to invoke (e.g. {@code "schema"}, {@code "array"})
     * @return the attribute value wrapped in an {@link Optional}, or empty if unavailable
     */
    public static Optional<Annotation> getAnnotationAttribute(
            Annotation annotation, String attributeName) {
        Object raw = invokeAttribute(annotation, attributeName);
        return raw instanceof Annotation a ? Optional.of(a) : Optional.empty();
    }

    /**
     * Reflectively reads an {@code Annotation[]}-typed attribute from an annotation by name.
     * <p>
     * Returns an empty list if the method does not exist, returns a non-{@code Annotation[]}
     * type, or throws during invocation.
     *
     * @param annotation    the annotation instance to read from
     * @param attributeName the attribute name to invoke (e.g. {@code "value"}, {@code "responses"}, {@code "content"})
     * @return the attribute values as an unmodifiable list, or an empty list if unavailable
     */
    public static List<Annotation> getAnnotationArrayAttribute(
            Annotation annotation, String attributeName) {
        Object raw = invokeAttribute(annotation, attributeName);
        return raw instanceof Annotation[] arr ? List.of(arr) : List.of();
    }

    /**
     * Reflectively reads a {@link Class}-typed attribute from an annotation by name.
     * <p>
     * Returns {@link Optional#empty()} if the method does not exist, returns a
     * non-{@link Class} type, or throws during invocation.
     *
     * @param annotation    the annotation instance to read from
     * @param attributeName the attribute name to invoke (e.g. {@code "implementation"})
     * @return the attribute value wrapped in an {@link Optional}, or empty if unavailable
     */
    public static Optional<Class<?>> getClassAttribute(
            Annotation annotation, String attributeName) {
        Object raw = invokeAttribute(annotation, attributeName);
        return raw instanceof Class<?> c ? Optional.of(c) : Optional.empty();
    }

    /**
     * Reflectively reads an {@link Enum}-typed attribute from an annotation by name and
     * returns the constant's {@linkplain Enum#name() declared name} as a {@link String}.
     *
     * <p>Using the constant name instead of casting to the concrete enum type keeps the
     * call-site classloader-independent: the enum may be loaded from a foreign
     * {@code URLClassLoader} and cannot be referenced by class literal in generator code.</p>
     *
     * <p>Returns {@link Optional#empty()} if the method does not exist, the returned value
     * is not an {@link Enum}, or an exception is thrown during invocation.</p>
     *
     * @param annotation    the annotation instance to read from
     * @param attributeName the attribute name to invoke (e.g. {@code "in"})
     * @return the enum constant name (e.g. {@code "QUERY"}, {@code "PATH"}), or empty
     */
    public static Optional<String> getEnumName(Annotation annotation, String attributeName) {
        Object raw = invokeAttribute(annotation, attributeName);
        return (raw instanceof Enum<?> e) ? Optional.of(e.name()) : Optional.empty();
    }

    /**
     * Applies {@code parser} to {@code value} and returns the result wrapped in an
     * {@link Optional}, or {@link Optional#empty()} if the parser throws a
     * {@link RuntimeException} (e.g. {@link NumberFormatException}).
     *
     * <p>Typical use: safely parsing numeric annotation attributes whose string
     * representation may be invalid.
     *
     * @param value  the string to parse; must not be {@code null}
     * @param parser a function that converts the string to {@code T}; must not be {@code null}
     * @param <T>    the target type
     * @return the parsed value, or empty if the parser threw
     */
    public static <T> Optional<T> tryParse(String value, Function<String, T> parser) {
        try {
            return Optional.of(parser.apply(value));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------
    // Internal helper
    // ------------------------------------------------------------------

    /**
     * Reflectively invokes a no-arg attribute method on an annotation instance.
     *
     * <p>Returns the raw result of the invocation, or {@code null} when the method does not
     * exist ({@link NoSuchMethodException} is silently swallowed, as it indicates the
     * annotation simply does not declare that attribute). All other exceptions are logged
     * at {@code DEBUG} level and {@code null} is returned.</p>
     *
     * @param annotation    the annotation instance to read from; must not be {@code null}
     * @param attributeName the attribute name to invoke; must not be {@code null}
     * @return the raw attribute value, or {@code null} if unavailable
     */
    private static Object invokeAttribute(Annotation annotation, String attributeName) {
        try {
            Method m = annotation.annotationType().getDeclaredMethod(attributeName);
            return m.invoke(annotation);
        } catch (NoSuchMethodException ignored) {
            // attribute does not exist on this annotation type — expected and normal
        } catch (Exception e) {
            log.debug(INVOKE_FAILED, attributeName, annotation.annotationType().getSimpleName(), e.getMessage());
        }
        return null;
    }
}
