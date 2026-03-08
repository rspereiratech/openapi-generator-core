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
        try {
            Method m = annotation.annotationType().getDeclaredMethod(methodName);
            if (m.invoke(annotation) instanceof String[] arr) {
                return List.of(arr);
            }
        } catch (NoSuchMethodException ignored) {
            // attribute does not exist on this annotation — expected
        } catch (Exception e) {
            log.debug(INVOKE_FAILED, methodName, annotation.annotationType().getSimpleName(), e.getMessage());
        }
        return List.of();
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
        try {
            Method m = annotation.annotationType().getDeclaredMethod(attributeName);
            return m.invoke(annotation) instanceof String s ? s : "";
        } catch (NoSuchMethodException ignored) {
            // attribute does not exist on this annotation — expected
        } catch (Exception e) {
            log.debug(INVOKE_FAILED, attributeName, annotation.annotationType().getSimpleName(), e.getMessage());
        }
        return "";
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
        try {
            Method m = annotation.annotationType().getDeclaredMethod(attributeName);
            return m.invoke(annotation) instanceof Integer i ? i : defaultValue;
        } catch (NoSuchMethodException ignored) {
            // attribute does not exist on this annotation — expected
        } catch (Exception e) {
            log.debug(INVOKE_FAILED, attributeName, annotation.annotationType().getSimpleName(), e.getMessage());
        }
        return defaultValue;
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
        try {
            Method m = annotation.annotationType().getDeclaredMethod(attributeName);
            return m.invoke(annotation) instanceof Boolean b ? b : defaultValue;
        } catch (NoSuchMethodException ignored) {
            // attribute does not exist on this annotation — expected
        } catch (Exception e) {
            log.debug(INVOKE_FAILED, attributeName, annotation.annotationType().getSimpleName(), e.getMessage());
        }
        return defaultValue;
    }
}
